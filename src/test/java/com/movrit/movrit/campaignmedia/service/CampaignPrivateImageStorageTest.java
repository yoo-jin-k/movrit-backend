package com.movrit.movrit.campaignmedia.service;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

class CampaignPrivateImageStorageTest {
  @TempDir Path directory;

  @Test
  void storesOutsidePublicRootAndNeverUsesClientFilename() throws Exception {
    Path publicRoot = directory.resolve("uploads");
    var storage = new CampaignPrivateImageStorage(publicRoot.toString());
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    ImageIO.write(new BufferedImage(2, 3, BufferedImage.TYPE_INT_ARGB), "png", bytes);
    var stored =
        storage.store(
            new MockMultipartFile("file", "../../private.png", "image/png", bytes.toByteArray()));
    Path privateFile = storage.resolve(stored.key());
    assertFalse(privateFile.startsWith(publicRoot));
    assertTrue(Files.isRegularFile(privateFile));
    assertEquals(2, stored.width());
    assertEquals(3, stored.height());
    assertThrows(ResponseStatusException.class, () -> storage.resolve("../../private.png"));
    storage.remove(stored.key());
    assertFalse(Files.exists(privateFile));
  }
}
