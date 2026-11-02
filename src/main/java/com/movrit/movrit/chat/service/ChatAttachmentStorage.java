package com.movrit.movrit.chat.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * 채팅 첨부 파일을 저장합니다(uploads/chat-attachments).
 *
 * <ul>
 *   <li>사진(JPG·PNG)은 실제 이미지로 읽어 다시 저장해 숨은 정보(촬영 위치 등)를 지웁니다.
 *   <li>PDF는 파일 앞부분이 '%PDF-'인지 확인한 뒤 그대로 저장합니다.
 *   <li>그 밖의 형식과 5MB를 넘는 파일은 받지 않습니다. 원래 파일 이름은 저장 경로에 쓰지 않습니다.
 * </ul>
 */
@Component
public class ChatAttachmentStorage {
  static final long MAX_BYTES = 5L * 1024 * 1024;
  private static final Pattern SAFE_KEY =
      Pattern.compile(
          "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|png|pdf)$");

  private final Path root;

  public ChatAttachmentStorage(@Value("${app.upload-dir:uploads}") String uploadDir) {
    root = Path.of(uploadDir).toAbsolutePath().normalize().resolve("chat-attachments").normalize();
  }

  /** 저장 결과입니다. */
  public record Stored(String key, String name, String contentType, long size) {}

  public Stored store(MultipartFile file) {
    if (file == null || file.isEmpty()) throw invalid("첨부할 파일을 선택해 주세요.");
    if (file.getSize() > MAX_BYTES) throw invalid("첨부 파일은 5MB 이하만 보낼 수 있습니다.");
    byte[] bytes;
    try (var input = file.getInputStream()) {
      bytes = input.readNBytes((int) MAX_BYTES + 1);
    } catch (IOException exception) {
      throw invalid("파일을 읽지 못했습니다.");
    }
    if (bytes.length == 0 || bytes.length > MAX_BYTES) throw invalid("파일 크기가 올바르지 않습니다.");
    String name = safeName(file.getOriginalFilename());
    try {
      Files.createDirectories(root);
      if (isPdf(bytes)) {
        String key = UUID.randomUUID() + ".pdf";
        Files.write(resolve(key), bytes);
        return new Stored(key, name, "application/pdf", bytes.length);
      }
      BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
      if (image == null || (long) image.getWidth() * image.getHeight() > 40_000_000L) {
        throw invalid("사진(JPG·PNG) 또는 PDF만 보낼 수 있습니다.");
      }
      String format = image.getColorModel().hasAlpha() ? "png" : "jpg";
      String key = UUID.randomUUID() + "." + format;
      if (!ImageIO.write(image, format, resolve(key).toFile())) throw invalid("사진을 저장하지 못했습니다.");
      long size = Files.size(resolve(key));
      return new Stored(key, name, format.equals("png") ? "image/png" : "image/jpeg", size);
    } catch (ResponseStatusException exception) {
      throw exception;
    } catch (IOException exception) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "첨부 파일 저장에 실패했습니다.");
    }
  }

  public Path load(String key) {
    Path path = resolve(key);
    if (!Files.isRegularFile(path)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "첨부 파일을 찾을 수 없습니다.");
    }
    return path;
  }

  public void delete(String key) {
    if (key == null) return;
    try {
      Files.deleteIfExists(resolve(key));
    } catch (IOException | ResponseStatusException ignored) {
      // 남은 파일은 운영 정리 작업에서 처리합니다.
    }
  }

  static boolean isPdf(byte[] bytes) {
    return bytes.length > 5
        && Arrays.equals(Arrays.copyOf(bytes, 5), "%PDF-".getBytes(StandardCharsets.US_ASCII));
  }

  /** 내려받을 때 쓸 파일 이름입니다. 경로 문자와 제어 문자를 지우고 100자로 자릅니다. */
  static String safeName(String original) {
    String value =
        original == null ? "" : original.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "").trim();
    if (value.isBlank()) value = "첨부파일";
    return value.length() > 100 ? value.substring(value.length() - 100) : value;
  }

  private Path resolve(String key) {
    if (key == null || !SAFE_KEY.matcher(key.toLowerCase(Locale.ROOT)).matches()) {
      throw invalid("잘못된 첨부 저장 키입니다.");
    }
    Path path = root.resolve(key).normalize();
    if (!path.startsWith(root)) throw invalid("잘못된 첨부 경로입니다.");
    return path;
  }

  private static ResponseStatusException invalid(String message) {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
  }
}
