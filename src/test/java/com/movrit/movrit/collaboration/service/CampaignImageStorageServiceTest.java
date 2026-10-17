package com.movrit.movrit.collaboration.service;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.exif.ExifIFD0Directory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.zip.CRC32;

import static org.junit.jupiter.api.Assertions.*;

class CampaignImageStorageServiceTest {
    @TempDir Path root;

    @Test
    void decodesRealPngAndStripsTextMetadata() throws Exception {
        byte[] original = png(3, 2);
        byte[] tagged = insertPngChunk(original, "tEXt", "GPS\0PRIVATE_LOCATION".getBytes());
        Path stored = store(tagged, "image/png");
        assertEquals(3, ImageIO.read(stored.toFile()).getWidth());
        assertFalse(new String(Files.readAllBytes(stored), java.nio.charset.StandardCharsets.ISO_8859_1).contains("PRIVATE_LOCATION"));
    }

    @Test
    void rejectsSpoofedMimeAndDamagedFilesWithoutCreatingOutput() throws Exception {
        assertThrows(ResponseStatusException.class, () -> store(png(2, 2), "image/jpeg"));
        assertThrows(ResponseStatusException.class, () -> store(new byte[]{1, 2, 3}, "image/png"));
        byte[] truncated = java.util.Arrays.copyOf(png(3, 3), 40);
        assertThrows(ResponseStatusException.class, () -> store(truncated, "image/png"));
        try (var paths = Files.list(root)) { assertEquals(0, paths.count()); }
    }

    @Test
    void rejectsEmptyOversizeAndAnimatedPng() throws Exception {
        assertThrows(ResponseStatusException.class, () -> store(new byte[0], "image/png"));
        assertThrows(ResponseStatusException.class, () -> store(new byte[(int) CampaignImageStorageService.MAX_BYTES + 1], "image/png"));
        byte[] animated = insertPngChunk(png(2, 2), "acTL", ByteBuffer.allocate(8).putInt(2).putInt(0).array());
        assertThrows(ResponseStatusException.class, () -> store(animated, "image/png"));
    }

    @Test
    void enforcesPixelBoundariesBeforeDecoding() throws Exception {
        assertDoesNotThrow(() -> CampaignImageStorageService.validateDimensions(5000, 5000));
        assertDoesNotThrow(() -> CampaignImageStorageService.validateDimensions(8192, 1));
        assertThrows(ResponseStatusException.class, () -> CampaignImageStorageService.validateDimensions(5001, 5000));
        assertThrows(ResponseStatusException.class, () -> CampaignImageStorageService.validateDimensions(8193, 1));
        assertThrows(ResponseStatusException.class, () -> CampaignImageStorageService.validateDimensions(0, 1));
        // 실제 이미지 헤더의 한도 초과도 읽기 전에 거부해야 합니다.
        byte[] oversized = png(1, 1);
        ByteBuffer.wrap(oversized, 16, 8).putInt(8193).putInt(1);
        CRC32 crc = new CRC32(); crc.update(oversized, 12, 17);
        ByteBuffer.wrap(oversized, 29, 4).putInt((int) crc.getValue());
        assertThrows(ResponseStatusException.class, () -> store(oversized, "image/png"));
    }

    @Test
    void readsActualLosslessWebpAndWritesCleanDisplayImage() throws Exception {
        // 자체 생성한 빨강·파랑 2픽셀 WebP입니다. 외부 이미지나 사용자 파일이 아닙니다.
        byte[] webp = Base64.getDecoder().decode("UklGRh4AAABXRUJQVlA4TBEAAAAvAQAAAA8Q87//8x8OMqL/AQA=");
        BufferedImage display = ImageIO.read(store(webp, "image/webp").toFile());
        assertEquals(2, display.getWidth());
        assertEquals(1, display.getHeight());
    }

    @Test
    void rejectsAnimatedWebpContainer() throws Exception {
        byte[] webp = Base64.getDecoder().decode("UklGRh4AAABXRUJQVlA4TBEAAAAvAQAAAA8Q87//8x8OMqL/AQA=");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(webp, 0, 12);
        out.write(new byte[]{65,78,73,77,6,0,0,0,0,0,0,0,0,0});
        out.write(webp, 12, webp.length - 12);
        byte[] animated = out.toByteArray();
        ByteBuffer.wrap(animated, 4, 4).order(java.nio.ByteOrder.LITTLE_ENDIAN).putInt(animated.length - 8);
        assertThrows(ResponseStatusException.class, () -> store(animated, "image/webp"));
    }

    @Test
    void appliesJpegExifOrientationAndDoesNotRetainExif() throws Exception {
        BufferedImage source = new BufferedImage(3, 2, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream encoded = new ByteArrayOutputStream(); ImageIO.write(source, "jpg", encoded);
        // APP1 + EXIF TIFF IFD0: Orientation=6 (시계 방향 90도).
        byte[] exif = new byte[]{69,120,105,102,0,0,73,73,42,0,8,0,0,0,1,0,18,1,3,0,1,0,0,0,6,0,0,0,0,0,0,0};
        byte[] jpeg = encoded.toByteArray();
        ByteArrayOutputStream tagged = new ByteArrayOutputStream();
        tagged.write(jpeg, 0, 2); tagged.write(255); tagged.write(225);
        tagged.write((exif.length + 2) >> 8); tagged.write((exif.length + 2) & 255);
        tagged.write(exif); tagged.write(jpeg, 2, jpeg.length - 2);
        byte[] result = Files.readAllBytes(store(tagged.toByteArray(), "image/jpeg"));
        BufferedImage display = ImageIO.read(new ByteArrayInputStream(result));
        assertEquals(2, display.getWidth()); assertEquals(3, display.getHeight());
        assertNull(ImageMetadataReader.readMetadata(new ByteArrayInputStream(result)).getFirstDirectoryOfType(ExifIFD0Directory.class));
    }

    @Test
    void preservesAllEightOrientationPixelMappings() {
        BufferedImage source = new BufferedImage(3, 2, BufferedImage.TYPE_INT_ARGB);
        source.setRGB(0, 0, 0xffff0000);
        int[][] positions = {{0,0},{2,0},{2,1},{0,1},{0,0},{1,0},{1,2},{0,2}};
        for (int orientation = 1; orientation <= 8; orientation++) {
            BufferedImage result = CampaignImageStorageService.orient(source, orientation);
            assertEquals(0xffff0000, result.getRGB(positions[orientation-1][0], positions[orientation-1][1]));
            assertEquals(orientation >= 5 ? 2 : 3, result.getWidth());
        }
    }

    private Path store(byte[] bytes, String mime) {
        String url = new CampaignImageStorageService(root.toString()).storeImage(
                new MockMultipartFile("file", "ignored.name", mime, bytes), "campaigns/qa");
        return root.resolve(url.substring("/uploads/".length()));
    }

    private byte[] png(int width, int height) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB), "png", out);
        return out.toByteArray();
    }

    private byte[] insertPngChunk(byte[] png, String type, byte[] data) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(png, 0, 33);
        out.write(ByteBuffer.allocate(4).putInt(data.length).array());
        byte[] name = type.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        out.write(name); out.write(data);
        CRC32 crc = new CRC32(); crc.update(name); crc.update(data);
        out.write(ByteBuffer.allocate(4).putInt((int) crc.getValue()).array());
        out.write(png, 33, png.length - 33);
        return out.toByteArray();
    }
}
