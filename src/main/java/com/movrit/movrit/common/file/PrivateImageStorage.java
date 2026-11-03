package com.movrit.movrit.common.file;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * 공개 주소 없이 API로만 내려가는 비공개 이미지(구매 인증 캡처, 협의 취소 증빙 등)를 저장하는 공통 동작입니다.
 *
 * <ul>
 *   <li>JPG·PNG만 받고, 실제 이미지로 읽어 다시 저장해 숨은 정보(촬영 위치 등)를 지웁니다.
 *   <li>원래 파일 이름은 쓰지 않고 새 무작위 이름으로 저장합니다.
 *   <li>5MB, 4천만 화소를 넘는 이미지는 받지 않습니다.
 * </ul>
 *
 * <p>기능별 저장소는 이 클래스를 상속해 폴더 이름과 안내 문구(예: "캡처 이미지")만 정합니다.
 */
public abstract class PrivateImageStorage {
  public static final long MAX_BYTES = 5L * 1024 * 1024;
  private static final Pattern SAFE_KEY =
      Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|png)$");

  private final Path root;
  private final String label;

  /**
   * @param uploadDir 비공개 업로드 최상위 폴더(app.private-upload-dir). 공개 폴더(/uploads)와 떨어져 있어 주소로 열리지 않습니다
   * @param folder 이 기능의 하위 폴더 이름
   * @param label 안내 문구에 쓸 이미지 이름(예: "캡처 이미지", "증빙 이미지")
   */
  protected PrivateImageStorage(String uploadDir, String folder, String label) {
    this.root = Path.of(uploadDir).toAbsolutePath().normalize().resolve(folder).normalize();
    this.label = label;
  }

  /** 이미지를 검사해 새 이름으로 저장하고 저장 키를 돌려줍니다. */
  public String store(MultipartFile file) {
    if (file == null || file.isEmpty()) throw invalid(label + "를 첨부해 주세요.");
    if (file.getSize() > MAX_BYTES) throw invalid(label + "는 5MB 이하만 올릴 수 있습니다.");
    BufferedImage image;
    try (var input = file.getInputStream()) {
      byte[] bytes = input.readNBytes((int) MAX_BYTES + 1);
      if (bytes.length == 0 || bytes.length > MAX_BYTES) throw invalid(label + " 크기가 올바르지 않습니다.");
      // JPG(FF D8 FF)·PNG(89 'PNG')만 받습니다. GIF·BMP 등은 여기서 거릅니다.
      boolean jpeg = bytes.length > 3 && (bytes[0] & 0xff) == 0xff && (bytes[1] & 0xff) == 0xd8;
      boolean png = bytes.length > 8 && (bytes[0] & 0xff) == 0x89 && bytes[1] == 'P';
      if (!jpeg && !png) throw invalid("JPG 또는 PNG 이미지를 올려 주세요.");
      image = ImageIO.read(new ByteArrayInputStream(bytes));
    } catch (IOException exception) {
      throw invalid(label + "를 읽지 못했습니다.");
    }
    // 지나치게 큰 이미지로 서버가 느려지지 않도록 크기를 제한합니다.
    if (image == null || (long) image.getWidth() * image.getHeight() > 40_000_000L) {
      throw invalid("JPG 또는 PNG 이미지를 올려 주세요.");
    }
    String format = image.getColorModel().hasAlpha() ? "png" : "jpg";
    String key = UUID.randomUUID() + "." + format;
    try {
      Files.createDirectories(root);
      if (!ImageIO.write(image, format, resolve(key).toFile())) throw new IOException("저장 형식 없음");
      return key;
    } catch (IOException exception) {
      delete(key);
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, label + " 저장에 실패했습니다.");
    }
  }

  /** 저장 키로 파일을 찾습니다. 없으면 404입니다. */
  public Path load(String key) {
    Path path = resolve(key);
    if (!Files.isRegularFile(path)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, label + "를 찾을 수 없습니다.");
    }
    return path;
  }

  /** 저장 실패 등으로 남은 파일을 지웁니다. 지우지 못해도 오류를 내지 않습니다. */
  public void delete(String key) {
    if (key == null || key.isBlank()) return;
    try {
      Files.deleteIfExists(resolve(key));
    } catch (IOException | ResponseStatusException ignored) {
      // 결과는 유지하고, 지우지 못한 파일은 운영 정리 작업에서 처리합니다.
    }
  }

  /** 저장 키의 확장자로 응답 형식을 정합니다. */
  public static String contentType(String key) {
    return key != null && key.endsWith(".png") ? "image/png" : "image/jpeg";
  }

  private Path resolve(String key) {
    if (key == null || !SAFE_KEY.matcher(key).matches()) throw invalid("잘못된 저장 키입니다.");
    Path path = root.resolve(key).normalize();
    if (!path.startsWith(root)) throw invalid("잘못된 저장 경로입니다.");
    return path;
  }

  private static ResponseStatusException invalid(String message) {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
  }
}
