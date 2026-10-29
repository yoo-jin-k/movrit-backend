package com.movrit.movrit.memberavatar.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.exif.ExifIFD0Directory;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

class MemberAvatarStorageTest {
  @TempDir Path root;

  @Test
  void ignoresOriginalNameAndStoresDecodedPixelsUnderDedicatedDirectory() throws Exception {
    MemberAvatarStorage storage = new MemberAvatarStorage(root.toString());
    var stored = storage.store(image("../../run.exe", "image/png", BufferedImage.TYPE_INT_ARGB));

    assertThat(stored.key()).matches("[0-9a-f-]{36}\\.png");
    assertThat(stored.key()).doesNotContain("run");
    Path path = storage.load(stored.key());
    assertThat(path.getParent().getFileName().toString()).isEqualTo("member-avatars");
    assertThat(ImageIO.read(path.toFile()).getWidth()).isEqualTo(3);
  }

  @Test
  void rejectsFakeMimeSvgExecutableAndTraversalKeys() throws Exception {
    MemberAvatarStorage storage = new MemberAvatarStorage(root.toString());
    byte[] png = imageBytes(BufferedImage.TYPE_INT_RGB);

    assertThatThrownBy(
            () ->
                storage.store(
                    new MockMultipartFile("file", "fake.jpg", "image/jpeg", png)))
        .isInstanceOf(ResponseStatusException.class);
    assertThatThrownBy(
            () ->
                storage.store(
                    new MockMultipartFile(
                        "file", "avatar.svg", "image/svg+xml", "<svg/>".getBytes())))
        .isInstanceOf(ResponseStatusException.class);
    assertThatThrownBy(
            () ->
                storage.store(
                    new MockMultipartFile(
                        "file", "avatar.png", "image/png", new byte[] {0x4d, 0x5a})))
        .isInstanceOf(ResponseStatusException.class);
    assertThatThrownBy(() -> storage.load("../../outside.png"))
        .isInstanceOf(ResponseStatusException.class);
  }

  @Test
  void rejectsOversizedTransferAndPixelDimensions() {
    MemberAvatarStorage storage = new MemberAvatarStorage(root.toString());
    assertThatThrownBy(
            () ->
                storage.store(
                    new MockMultipartFile(
                        "file", "large.png", "image/png", new byte[5 * 1024 * 1024 + 1])))
        .isInstanceOf(ResponseStatusException.class);
    assertThatThrownBy(() -> MemberAvatarStorage.validateDimensions(8193, 1))
        .isInstanceOf(ResponseStatusException.class);
  }

  @Test
  void appliesExifOrientationAndRemovesExifFromStoredJpeg() throws Exception {
    BufferedImage source = new BufferedImage(3, 2, BufferedImage.TYPE_INT_RGB);
    ByteArrayOutputStream encoded = new ByteArrayOutputStream();
    ImageIO.write(source, "jpg", encoded);
    byte[] exif =
        new byte[] {
          69, 120, 105, 102, 0, 0, 73, 73, 42, 0, 8, 0, 0, 0, 1, 0, 18, 1, 3, 0, 1, 0, 0, 0, 6, 0,
          0, 0, 0, 0, 0, 0
        };
    byte[] jpeg = encoded.toByteArray();
    ByteArrayOutputStream tagged = new ByteArrayOutputStream();
    tagged.write(jpeg, 0, 2);
    tagged.write(255);
    tagged.write(225);
    tagged.write((exif.length + 2) >> 8);
    tagged.write((exif.length + 2) & 255);
    tagged.write(exif);
    tagged.write(jpeg, 2, jpeg.length - 2);

    MemberAvatarStorage storage = new MemberAvatarStorage(root.toString());
    var stored =
        storage.store(
            new MockMultipartFile(
                "file", "portrait.jpg", "image/jpeg", tagged.toByteArray()));
    byte[] result = Files.readAllBytes(storage.load(stored.key()));
    BufferedImage display = ImageIO.read(new ByteArrayInputStream(result));

    assertThat(display.getWidth()).isEqualTo(2);
    assertThat(display.getHeight()).isEqualTo(3);
    assertThat(
            ImageMetadataReader.readMetadata(new ByteArrayInputStream(result))
                .getFirstDirectoryOfType(ExifIFD0Directory.class))
        .isNull();
  }

  @Test
  void resizesDisplayCopyToMaximum1024PixelsWithoutCropping() throws Exception {
    ByteArrayOutputStream encoded = new ByteArrayOutputStream();
    ImageIO.write(new BufferedImage(2048, 1024, BufferedImage.TYPE_INT_RGB), "jpg", encoded);
    MemberAvatarStorage storage = new MemberAvatarStorage(root.toString());

    var stored =
        storage.store(
            new MockMultipartFile(
                "file", "large.jpg", "image/jpeg", encoded.toByteArray()));
    BufferedImage display = ImageIO.read(storage.load(stored.key()).toFile());

    assertThat(display.getWidth()).isEqualTo(1024);
    assertThat(display.getHeight()).isEqualTo(512);
  }

  private MockMultipartFile image(String name, String mime, int type) throws Exception {
    return new MockMultipartFile("file", name, mime, imageBytes(type));
  }

  private byte[] imageBytes(int type) throws Exception {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    ImageIO.write(new BufferedImage(3, 2, type), "png", output);
    return output.toByteArray();
  }

}
