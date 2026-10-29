package com.movrit.movrit.memberavatar.service;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.exif.ExifIFD0Directory;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
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

/** 회원 프로필 표시본을 캠페인 파일과 분리해 검증·재인코딩·저장합니다. */
@Component
public class MemberAvatarStorage {
  static final long MAX_BYTES = 5L * 1024 * 1024;
  private static final String DIRECTORY = "member-avatars";
  private static final Map<String, String> INPUT_MIME =
      Map.of("jpeg", "image/jpeg", "png", "image/png");
  private static final Pattern SAFE_KEY =
      Pattern.compile(
          "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|png)$");

  private final Path root;

  public MemberAvatarStorage(@Value("${app.upload-dir:uploads}") String uploadDir) {
    root = Path.of(uploadDir).toAbsolutePath().normalize().resolve(DIRECTORY).normalize();
  }

  /** 원본 이름을 무시하고 새 UUID 키로 메타데이터가 없는 표시본을 저장합니다. */
  public StoredAvatar store(MultipartFile file) {
    if (file == null || file.isEmpty()) throw invalid("프로필 사진을 선택해 주세요.");
    if (file.getSize() > MAX_BYTES) throw invalid("프로필 사진은 5MiB 이하만 업로드할 수 있습니다.");
    BufferedImage normalized = read(file);
    String format = normalized.getColorModel().hasAlpha() ? "png" : "jpg";
    String contentType = format.equals("png") ? "image/png" : "image/jpeg";
    String key = UUID.randomUUID() + "." + format;
    Path destination = resolve(key);
    try {
      Files.createDirectories(root);
      if (!ImageIO.write(normalized, format, destination.toFile())) {
        throw new IOException("지원되지 않는 출력 형식");
      }
      return new StoredAvatar(key, contentType);
    } catch (IOException exception) {
      delete(key);
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "프로필 사진 저장에 실패했습니다.");
    }
  }

  public Path load(String key) {
    Path path = resolve(key);
    if (!Files.isRegularFile(path)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "프로필 사진을 찾을 수 없습니다.");
    }
    return path;
  }

  public void delete(String key) {
    if (key == null || key.isBlank()) return;
    try {
      Files.deleteIfExists(resolve(key));
    } catch (IOException | ResponseStatusException ignored) {
      // DB 커밋 결과는 유지하고, 삭제하지 못한 고아 파일은 운영 정리 작업에서 처리합니다.
    }
  }

  private BufferedImage read(MultipartFile file) {
    try (var input = file.getInputStream()) {
      byte[] bytes = input.readNBytes((int) MAX_BYTES + 1);
      if (bytes.length == 0 || bytes.length > MAX_BYTES) {
        throw invalid("프로필 사진 크기가 올바르지 않습니다.");
      }
      return decode(bytes, file.getContentType());
    } catch (ResponseStatusException exception) {
      throw exception;
    } catch (Exception exception) {
      throw invalid("정상적인 JPG 또는 PNG 파일을 선택해 주세요.");
    }
  }

  private BufferedImage decode(byte[] bytes, String contentType) throws Exception {
    try (var stream = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
      var readers = ImageIO.getImageReaders(stream);
      if (!readers.hasNext()) throw invalid("실제 이미지 형식을 확인할 수 없습니다.");
      ImageReader reader = readers.next();
      try {
        String format = reader.getFormatName().toLowerCase(Locale.ROOT);
        if (!INPUT_MIME.containsKey(format) || !INPUT_MIME.get(format).equals(contentType)) {
          throw invalid("파일의 실제 형식과 이미지 형식 정보가 일치하지 않습니다.");
        }
        if (format.equals("png")) rejectAnimatedPng(bytes);
        reader.setInput(stream, false, false);
        validateDimensions(reader.getWidth(0), reader.getHeight(0));
        boolean[] warning = {false};
        reader.addIIOReadWarningListener((source, message) -> warning[0] = true);
        BufferedImage image = reader.read(0);
        if (image == null || warning[0]) throw invalid("손상된 프로필 사진은 업로드할 수 없습니다.");
        validateDimensions(image.getWidth(), image.getHeight());
        var metadata = ImageMetadataReader.readMetadata(new ByteArrayInputStream(bytes));
        var exif = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
        Integer orientation =
            exif == null ? null : exif.getInteger(ExifIFD0Directory.TAG_ORIENTATION);
        if (orientation != null && (orientation < 1 || orientation > 8)) {
          throw invalid("이미지 방향 정보가 올바르지 않습니다.");
        }
        return resize(orient(image, orientation == null ? 1 : orientation));
      } finally {
        reader.dispose();
      }
    }
  }

  static void validateDimensions(int width, int height) {
    if (width < 1
        || height < 1
        || width > 8192
        || height > 8192
        || (long) width * height > 25_000_000L) {
      throw invalid("프로필 사진은 각 변 8,192px, 총 25,000,000픽셀 이하여야 합니다.");
    }
  }

  private static BufferedImage orient(BufferedImage source, int orientation) {
    int width = source.getWidth();
    int height = source.getHeight();
    boolean swapped = orientation >= 5;
    BufferedImage target =
        new BufferedImage(
            swapped ? height : width,
            swapped ? width : height,
            source.getColorModel().hasAlpha()
                ? BufferedImage.TYPE_INT_ARGB
                : BufferedImage.TYPE_INT_RGB);
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        int tx = x;
        int ty = y;
        switch (orientation) {
          case 2 -> tx = width - 1 - x;
          case 3 -> {
            tx = width - 1 - x;
            ty = height - 1 - y;
          }
          case 4 -> ty = height - 1 - y;
          case 5 -> {
            tx = y;
            ty = x;
          }
          case 6 -> {
            tx = height - 1 - y;
            ty = x;
          }
          case 7 -> {
            tx = height - 1 - y;
            ty = width - 1 - x;
          }
          case 8 -> {
            tx = y;
            ty = width - 1 - x;
          }
          default -> {}
        }
        target.setRGB(tx, ty, source.getRGB(x, y));
      }
    }
    return target;
  }

  /** 헤더 표시본은 긴 변을 1,024px 이하로 줄여 원본 해상도와 무관하게 전송량을 제한합니다. */
  private static BufferedImage resize(BufferedImage source) {
    int longest = Math.max(source.getWidth(), source.getHeight());
    if (longest <= 1024) return source;
    double ratio = 1024.0 / longest;
    int width = Math.max(1, (int) Math.round(source.getWidth() * ratio));
    int height = Math.max(1, (int) Math.round(source.getHeight() * ratio));
    BufferedImage target =
        new BufferedImage(
            width,
            height,
            source.getColorModel().hasAlpha()
                ? BufferedImage.TYPE_INT_ARGB
                : BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics = target.createGraphics();
    try {
      graphics.setRenderingHint(
          RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
      graphics.setRenderingHint(
          RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      graphics.drawImage(source, 0, 0, width, height, null);
    } finally {
      graphics.dispose();
    }
    return target;
  }

  private static void rejectAnimatedPng(byte[] bytes) {
    int offset = 8;
    while (offset + 8 <= bytes.length) {
      long size =
          Integer.toUnsignedLong(
              ByteBuffer.wrap(bytes, offset, 4).order(ByteOrder.BIG_ENDIAN).getInt());
      String type = new String(bytes, offset + 4, 4, StandardCharsets.US_ASCII);
      if (type.equals("acTL")) throw invalid("움직이는 PNG는 업로드할 수 없습니다.");
      long next = offset + 12L + size;
      if (next > bytes.length || next <= offset) throw invalid("이미지 파일 구조가 올바르지 않습니다.");
      offset = (int) next;
    }
  }

  private Path resolve(String key) {
    if (key == null || !SAFE_KEY.matcher(key).matches()) {
      throw invalid("잘못된 프로필 사진 저장 키입니다.");
    }
    Path path = root.resolve(key).normalize();
    if (!path.startsWith(root)) throw invalid("잘못된 프로필 사진 경로입니다.");
    return path;
  }

  private static ResponseStatusException invalid(String message) {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
  }

  public record StoredAvatar(String key, String contentType) {}
}
