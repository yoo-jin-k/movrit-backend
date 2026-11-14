package com.movrit.movrit.chat.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * 채팅 첨부 파일을 저장합니다(비공개 위치 app.private-upload-dir 아래 chat-attachments).
 *
 * <ul>
 *   <li>사진(JPG·PNG)은 머리 정보로 크기(4,000만 화소 이하)를 먼저 확인한 뒤, 실제 이미지로 읽어 다시 저장해 숨은 정보(촬영 위치 등)를 지웁니다.
 *   <li>PDF는 파일 앞부분이 '%PDF-'인지 확인한 뒤 그대로 저장합니다.
 *   <li>영상(MP4·MOV·WebM)은 파일 앞부분 표식으로 형식을 확인한 뒤 그대로 저장합니다(20MB까지).
 *   <li>그 밖의 형식과 크기를 넘는 파일(사진·PDF 5MB)은 받지 않습니다. 원래 파일 이름은 저장 경로에 쓰지 않습니다.
 * </ul>
 *
 * <p>변경 이력: 사진을 다 풀어 본 뒤에 크기를 확인하던 것 → 머리 정보로 크기부터 확인. 'ftyp' 표식만 있으면 MP4로 받던 것(AVIF 등 사진도 통과) →
 * MP4·MOV 브랜드 목록에 있을 때만 영상으로 받음.
 */
@Component
public class ChatAttachmentStorage {
  /** 사진·PDF 최대 크기입니다. */
  static final long MAX_BYTES = 5L * 1024 * 1024;

  /** 영상 최대 크기입니다. 휴대폰으로 찍은 짧은 영상(약 30초 안팎)을 보낼 수 있는 정도로 둡니다. */
  static final long MAX_VIDEO_BYTES = 20L * 1024 * 1024;

  /** 사진 최대 픽셀 수(가로 × 세로)입니다. */
  static final long MAX_PIXELS = 40_000_000L;

  /** 영상으로 받는 MP4 계열 'ftyp' 브랜드입니다. 같은 구조를 쓰는 사진 형식(HEIC·AVIF 등)이나 3GP처럼 화면이 받지 않는 형식은 넣지 않습니다. */
  private static final java.util.Set<String> MP4_BRANDS =
      java.util.Set.of(
          "isom", "iso2", "iso3", "iso4", "iso5", "iso6", "mp41", "mp42", "avc1", "M4V ", "M4VH",
          "M4VP", "mmp4", "dash");

  /** 영상 형식별 저장 확장자입니다. */
  private static final java.util.Map<String, String> VIDEO_EXTENSIONS =
      java.util.Map.of("video/mp4", "mp4", "video/quicktime", "mov", "video/webm", "webm");

  private static final Pattern SAFE_KEY =
      Pattern.compile(
          "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|png|pdf|mp4|mov|webm)$");

  private final Path root;

  public ChatAttachmentStorage(
      @Value("${app.private-upload-dir:uploads-private}") String uploadDir) {
    root = Path.of(uploadDir).toAbsolutePath().normalize().resolve("chat-attachments").normalize();
  }

  /** 저장 결과입니다. */
  public record Stored(String key, String name, String contentType, long size) {}

  public Stored store(MultipartFile file) {
    if (file == null || file.isEmpty()) throw invalid("첨부할 파일을 선택해 주세요.");
    if (file.getSize() > MAX_VIDEO_BYTES) throw invalid("영상은 20MB, 사진·PDF는 5MB 이하만 보낼 수 있습니다.");
    byte[] bytes;
    try (var input = file.getInputStream()) {
      bytes = input.readNBytes((int) MAX_VIDEO_BYTES + 1);
    } catch (IOException exception) {
      throw invalid("파일을 읽지 못했습니다.");
    }
    if (bytes.length == 0 || bytes.length > MAX_VIDEO_BYTES) throw invalid("파일 크기가 올바르지 않습니다.");
    String name = safeName(file.getOriginalFilename());
    try {
      Files.createDirectories(root);
      // 영상(MP4·MOV·WebM)은 다시 인코딩하지 않고 파일 앞부분 표식으로 형식만 확인해 저장합니다(20MB까지).
      String video = videoType(bytes);
      if (video != null) {
        String key = UUID.randomUUID() + "." + VIDEO_EXTENSIONS.get(video);
        Files.write(resolve(key), bytes);
        return new Stored(key, name, video, bytes.length);
      }
      // 영상이 아닌 파일은 기존처럼 5MB까지만 받습니다.
      if (bytes.length > MAX_BYTES) throw invalid("사진·PDF는 5MB 이하만 보낼 수 있습니다.");
      if (isPdf(bytes)) {
        String key = UUID.randomUUID() + ".pdf";
        Files.write(resolve(key), bytes);
        return new Stored(key, name, "application/pdf", bytes.length);
      }
      // 사진은 JPG·PNG만 받습니다(GIF·BMP 등은 앞부분 표식으로 걸러 냄). 깨진 파일은 서버 오류가 아니라 400으로 안내합니다.
      if (!isJpegOrPng(bytes)) throw invalid("사진(JPG·PNG), PDF, 영상(MP4·MOV·WebM)만 보낼 수 있습니다.");
      // 작은 파일이라도 풀면 큰 사진이 될 수 있어, 픽셀을 풀기 전에 머리 정보로 크기부터 확인합니다.
      requireImageSizeWithinLimit(bytes);
      BufferedImage image;
      try {
        image = ImageIO.read(new ByteArrayInputStream(bytes));
      } catch (IOException exception) {
        throw invalid("손상된 사진은 보낼 수 없습니다.");
      }
      if (image == null || (long) image.getWidth() * image.getHeight() > MAX_PIXELS) {
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

  /**
   * 파일 앞부분 표식으로 영상 형식을 알아냅니다. 영상이 아니면 null입니다.
   *
   * <ul>
   *   <li>MP4·MOV: 5~8번째 바이트가 'ftyp'이고, 그 뒤 브랜드가 'qt '이면 MOV(아이폰 기본 형식), MP4 브랜드 목록(isom·mp42 등)에
   *       있으면 MP4로 봅니다. 그 밖의 브랜드(HEIC·AVIF 사진 등)는 영상으로 보지 않습니다.
   *   <li>WebM: 처음 4바이트가 1A 45 DF A3입니다.
   * </ul>
   */
  static String videoType(byte[] bytes) {
    if (bytes.length >= 12
        && Arrays.equals(
            Arrays.copyOfRange(bytes, 4, 8), "ftyp".getBytes(StandardCharsets.US_ASCII))) {
      String brand = new String(Arrays.copyOfRange(bytes, 8, 12), StandardCharsets.US_ASCII);
      if (brand.equals("qt  ")) return "video/quicktime";
      return MP4_BRANDS.contains(brand) ? "video/mp4" : null;
    }
    if (bytes.length >= 4
        && (bytes[0] & 0xff) == 0x1a
        && (bytes[1] & 0xff) == 0x45
        && (bytes[2] & 0xff) == 0xdf
        && (bytes[3] & 0xff) == 0xa3) {
      return "video/webm";
    }
    return null;
  }

  /**
   * 사진 머리 정보에서 가로·세로만 읽어 최대 픽셀 수를 넘는지 확인합니다. 픽셀은 풀지 않습니다.
   *
   * @throws ResponseStatusException 읽을 수 없는 사진이면 400, 너무 크면 400
   */
  static void requireImageSizeWithinLimit(byte[] bytes) {
    try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
      Iterator<ImageReader> readers = input == null ? null : ImageIO.getImageReaders(input);
      if (readers == null || !readers.hasNext()) throw invalid("사진(JPG·PNG) 또는 PDF만 보낼 수 있습니다.");
      ImageReader reader = readers.next();
      try {
        reader.setInput(input, true, true);
        long pixels = (long) reader.getWidth(0) * reader.getHeight(0);
        if (pixels > MAX_PIXELS) throw invalid("사진이 너무 큽니다. 4,000만 화소 이하로 줄여 보내 주세요.");
      } finally {
        reader.dispose();
      }
    } catch (IOException exception) {
      throw invalid("손상된 사진은 보낼 수 없습니다.");
    }
  }

  /** JPG(FF D8 FF) 또는 PNG(89 'PNG') 파일인지 앞부분 표식으로 확인합니다. */
  static boolean isJpegOrPng(byte[] bytes) {
    boolean jpeg =
        bytes.length > 3
            && (bytes[0] & 0xff) == 0xff
            && (bytes[1] & 0xff) == 0xd8
            && (bytes[2] & 0xff) == 0xff;
    boolean png =
        bytes.length > 8
            && (bytes[0] & 0xff) == 0x89
            && bytes[1] == 'P'
            && bytes[2] == 'N'
            && bytes[3] == 'G';
    return jpeg || png;
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
