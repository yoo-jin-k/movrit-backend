package com.movrit.movrit.channelconnect.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * 채널 직접 등록 때 올리는 '프로필 화면 캡처'를 저장합니다.
 *
 * <ul>
 *   <li>JPG·PNG만 받고, 실제 이미지로 읽어 다시 저장해 숨은 메타데이터(촬영 위치 등)를 지웁니다.
 *   <li>원래 파일 이름은 쓰지 않고 새 무작위 이름으로 저장합니다(비공개 위치 app.private-upload-dir 아래 channel-evidence).
 *   <li>캡처는 운영자 확인용이라 공개 주소로 열리지 않고, 운영자 API로만 내려갑니다.
 * </ul>
 */
@Component
public class ChannelEvidenceStorage {
  static final long MAX_BYTES = 5L * 1024 * 1024;
  private static final Set<String> FORMATS = Set.of("jpeg", "png");
  private static final Pattern SAFE_KEY =
      Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|png)$");

  private final Path root;

  public ChannelEvidenceStorage(
      @Value("${app.private-upload-dir:uploads-private}") String uploadDir) {
    root = Path.of(uploadDir).toAbsolutePath().normalize().resolve("channel-evidence").normalize();
  }

  /** 캡처 이미지를 검사해 새 이름으로 저장하고 저장 키를 돌려줍니다. */
  public String store(MultipartFile file) {
    if (file == null || file.isEmpty()) throw invalid("프로필 화면 캡처를 첨부해 주세요.");
    if (file.getSize() > MAX_BYTES) throw invalid("캡처 이미지는 5MB 이하만 올릴 수 있습니다.");
    BufferedImage image = read(file);
    String format = image.getColorModel().hasAlpha() ? "png" : "jpg";
    String key = UUID.randomUUID() + "." + format;
    try {
      Files.createDirectories(root);
      if (!ImageIO.write(image, format, resolve(key).toFile())) {
        throw new IOException("지원하지 않는 저장 형식");
      }
      return key;
    } catch (IOException exception) {
      delete(key);
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "캡처 이미지 저장에 실패했습니다.");
    }
  }

  /** 저장 키로 캡처 파일을 찾습니다. 없으면 404입니다. */
  public Path load(String key) {
    Path path = resolve(key);
    if (!Files.isRegularFile(path)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "캡처 이미지를 찾을 수 없습니다.");
    }
    return path;
  }

  /** 캡처 파일을 지웁니다. 지우지 못해도 오류를 내지 않습니다(남은 파일은 운영 정리로 처리합니다). */
  public void delete(String key) {
    if (key == null || key.isBlank()) return;
    try {
      Files.deleteIfExists(resolve(key));
    } catch (IOException | ResponseStatusException ignored) {
      // 등록 결과는 유지하고, 지우지 못한 파일은 운영 정리 작업에서 처리합니다.
    }
  }

  /** 저장 키의 확장자로 응답 형식을 정합니다. */
  public static String contentType(String key) {
    return key != null && key.endsWith(".png") ? "image/png" : "image/jpeg";
  }

  private BufferedImage read(MultipartFile file) {
    try (var input = file.getInputStream()) {
      byte[] bytes = input.readNBytes((int) MAX_BYTES + 1);
      if (bytes.length == 0 || bytes.length > MAX_BYTES) throw invalid("캡처 이미지 크기가 올바르지 않습니다.");
      try (var stream = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
        var readers = ImageIO.getImageReaders(stream);
        if (!readers.hasNext()) throw invalid("JPG 또는 PNG 이미지를 올려 주세요.");
        ImageReader reader = readers.next();
        try {
          if (!FORMATS.contains(reader.getFormatName().toLowerCase(Locale.ROOT))) {
            throw invalid("JPG 또는 PNG 이미지를 올려 주세요.");
          }
          reader.setInput(stream, true, true);
          int width = reader.getWidth(0);
          int height = reader.getHeight(0);
          // 지나치게 큰 이미지로 서버가 느려지지 않도록 크기를 제한합니다.
          if (width < 1 || height < 1 || (long) width * height > 40_000_000L) {
            throw invalid("이미지 크기가 너무 큽니다.");
          }
          BufferedImage image = reader.read(0);
          if (image == null) throw invalid("손상된 이미지는 올릴 수 없습니다.");
          return image;
        } finally {
          reader.dispose();
        }
      }
    } catch (ResponseStatusException exception) {
      throw exception;
    } catch (Exception exception) {
      throw invalid("정상적인 JPG 또는 PNG 이미지를 올려 주세요.");
    }
  }

  private Path resolve(String key) {
    if (key == null || !SAFE_KEY.matcher(key).matches()) throw invalid("잘못된 캡처 저장 키입니다.");
    Path path = root.resolve(key).normalize();
    if (!path.startsWith(root)) throw invalid("잘못된 캡처 경로입니다.");
    return path;
  }

  private static ResponseStatusException invalid(String message) {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
  }
}
