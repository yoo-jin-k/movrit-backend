package com.movrit.movrit.collaboration.service;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.exif.ExifIFD0Directory;
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
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * 캠페인 대표·상세 이미지를 검사한 뒤 안전한 표시용 파일로 다시 만들어 저장합니다. JPG·PNG·WebP만 받고, 크기·해상도·움직이는 이미지·손상 파일을 걸러내며 위치
 * 정보 같은 원본 메타데이터는 남기지 않습니다. 상품 이미지 업로드 정책과는 별개입니다.
 */
/** 캠페인 표시본만 검증·재인코딩하며 공통 상품 업로드 정책은 변경하지 않습니다. */
@Service
public class CampaignImageStorageService {
  static final long MAX_BYTES = 5L * 1024 * 1024;
  private static final Map<String, String> MIME =
      Map.of("jpeg", "image/jpeg", "png", "image/png", "webp", "image/webp");
  private final Path root;

  /** 업로드 폴더(app.upload-dir) 아래 캠페인 공개 이미지 저장 위치를 정합니다. */
  public CampaignImageStorageService(@Value("${app.upload-dir:uploads}") String uploadDir) {
    root = Path.of(uploadDir).toAbsolutePath().normalize();
  }

  /**
   * 이미지를 검사하고 새 파일로 저장한 뒤 화면에서 쓸 주소(/uploads/...)를 반환합니다.
   *
   * <p>파일 없음·5MiB 초과·형식 불일치·손상·움직이는 이미지·너무 큰 해상도는 400으로 거절합니다. 저장 중 실패하면 만들다 만 파일을 지우고 500으로 응답합니다.
   */
  public String storeImage(MultipartFile file, String directory) {
    if (file == null || file.isEmpty()) throw invalid("이미지 파일을 선택해 주세요.");
    if (file.getSize() > MAX_BYTES) throw invalid("이미지는 5MiB 이하만 업로드할 수 있습니다.");
    Path folder = root.resolve(directory).normalize();
    if (!folder.startsWith(root)) throw invalid("잘못된 업로드 경로입니다.");
    final BufferedImage normalized;
    try (var input = file.getInputStream()) {
      byte[] bytes = input.readNBytes((int) MAX_BYTES + 1);
      if (bytes.length == 0 || bytes.length > MAX_BYTES) throw invalid("이미지 파일 크기가 올바르지 않습니다.");
      normalized = decode(bytes, file.getContentType());
    } catch (ResponseStatusException e) {
      throw e;
    } catch (Exception e) {
      throw invalid("이미지 파일을 읽을 수 없습니다. 정상적인 JPG, PNG, WebP 파일을 선택해 주세요.");
    }
    // 새 픽셀 데이터만 인코딩해 GPS·EXIF·텍스트 등 원본 메타데이터를 전달하지 않습니다.
    String format = normalized.getColorModel().hasAlpha() ? "png" : "jpg";
    String filename = UUID.randomUUID() + "." + format;
    Path destination = folder.resolve(filename);
    try {
      Files.createDirectories(folder);
      if (!ImageIO.write(normalized, format, destination.toFile()))
        throw new IOException("지원되지 않는 출력 형식");
    } catch (IOException e) {
      try {
        Files.deleteIfExists(destination);
      } catch (IOException ignored) {
      }
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "이미지 저장에 실패했습니다.");
    }
    return "/uploads/" + directory + "/" + filename;
  }

  /**
   * 실제 파일 내용으로 형식을 확인해 이미지를 읽고, EXIF 방향대로 바로 세운 이미지를 돌려줍니다. 확장자·Content-Type만 믿지 않고 파일 내용과 일치하는지
   * 검사합니다.
   */
  private BufferedImage decode(byte[] bytes, String contentType) throws Exception {
    try (var stream = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
      var candidates = ImageIO.getImageReaders(stream);
      if (!candidates.hasNext()) throw invalid("실제 이미지 형식을 확인할 수 없습니다.");
      ImageReader reader = candidates.next();
      try {
        String format = reader.getFormatName().toLowerCase(Locale.ROOT);
        if (!MIME.containsKey(format) || !MIME.get(format).equals(contentType)) {
          throw invalid("파일의 실제 형식과 이미지 형식 정보가 일치하지 않습니다.");
        }
        rejectAnimation(bytes, format);
        reader.setInput(stream, false, false);
        validateDimensions(reader.getWidth(0), reader.getHeight(0));
        // 손상된 JPEG 등을 경고 후 부분 복구하는 대신 잘못된 업로드로 거부합니다.
        boolean[] warning = {false};
        reader.addIIOReadWarningListener((source, message) -> warning[0] = true);
        BufferedImage image = reader.read(0);
        if (image == null || warning[0]) throw invalid("손상된 이미지 파일은 업로드할 수 없습니다.");
        validateDimensions(image.getWidth(), image.getHeight());
        var metadata = ImageMetadataReader.readMetadata(new ByteArrayInputStream(bytes));
        var exif = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
        Integer orientation =
            exif == null ? null : exif.getInteger(ExifIFD0Directory.TAG_ORIENTATION);
        if (orientation != null && (orientation < 1 || orientation > 8))
          throw invalid("이미지 방향 정보가 올바르지 않습니다.");
        return orient(image, orientation == null ? 1 : orientation);
      } finally {
        reader.dispose();
      }
    }
  }

  /** 가로·세로 각 8,192px, 전체 2,500만 픽셀을 넘는 이미지를 거절해 서버 메모리 과다 사용을 막습니다. */
  static void validateDimensions(int width, int height) {
    if (width < 1
        || height < 1
        || width > 8192
        || height > 8192
        || (long) width * height > 25_000_000L) {
      throw invalid("이미지는 각 변 8,192px, 총 25,000,000픽셀 이하여야 합니다.");
    }
  }

  /** 휴대폰 사진의 EXIF 방향 값(1~8)에 맞춰 이미지를 실제로 회전·반전합니다. 저장본에는 방향 태그가 없어도 어느 브라우저에서나 똑바로 보입니다. */
  /** EXIF 1~8의 회전·반전을 실제 픽셀에 적용하므로 표시본에 방향 태그가 필요하지 않습니다. */
  static BufferedImage orient(BufferedImage source, int orientation) {
    int width = source.getWidth(), height = source.getHeight();
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
        int tx = x, ty = y;
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

  /** 움직이는 PNG(APNG)·WebP를 거절합니다. 이미지 라이브러리는 첫 장면만 읽기 때문에 파일 안의 애니메이션 표시 조각을 직접 찾아 확인합니다. */
  /** 기본 ImageIO가 첫 프레임만 읽을 수 있으므로 컨테이너의 애니메이션 청크도 검사합니다. */
  private static void rejectAnimation(byte[] bytes, String format) {
    if (!format.equals("png") && !format.equals("webp")) return;
    int offset = format.equals("png") ? 8 : 12;
    while (offset + 8 <= bytes.length) {
      boolean png = format.equals("png");
      long size =
          Integer.toUnsignedLong(
              ByteBuffer.wrap(bytes, offset + (png ? 0 : 4), 4)
                  .order(png ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN)
                  .getInt());
      String type = new String(bytes, offset + (png ? 4 : 0), 4, StandardCharsets.US_ASCII);
      if (type.equals("acTL") || type.equals("ANIM") || type.equals("ANMF"))
        throw invalid("움직이는 이미지는 업로드할 수 없습니다.");
      long next = offset + 8L + size + (png ? 4 : size % 2);
      if (next > bytes.length || next <= offset) throw invalid("이미지 파일 구조가 올바르지 않습니다.");
      offset = (int) next;
    }
  }

  private static ResponseStatusException invalid(String message) {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
  }
}
