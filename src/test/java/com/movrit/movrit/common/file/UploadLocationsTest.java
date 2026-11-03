package com.movrit.movrit.common.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** 공개·비공개 업로드 위치 분리와 예전 비공개 파일 옮기기를 확인합니다. */
class UploadLocationsTest {

  @TempDir Path temp;

  @Test
  void overlappingLocationsAreRejected() {
    String publicDir = temp.resolve("uploads").toString();
    // 같은 폴더, 공개 폴더 안의 비공개 폴더, 비공개 폴더 안의 공개 폴더는 모두 거절합니다.
    assertThatThrownBy(() -> new UploadLocations(publicDir, publicDir))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> new UploadLocations(publicDir, publicDir + "/private"))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(
            () ->
                new UploadLocations(
                    temp.resolve("p/uploads").toString(), temp.resolve("p").toString()))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void legacyPrivateFilesMoveOutOfPublicFolderWithoutOverwriting() throws Exception {
    Path publicRoot = temp.resolve("uploads");
    Path privateRoot = temp.resolve("uploads-private");
    Files.createDirectories(publicRoot.resolve("purchase-proof"));
    Files.createDirectories(publicRoot.resolve("chat-attachments/12"));
    Files.createDirectories(publicRoot.resolve("campaigns/1"));
    Files.writeString(publicRoot.resolve("purchase-proof/a.png"), "old-a");
    Files.writeString(publicRoot.resolve("purchase-proof/b.png"), "old-b");
    Files.writeString(publicRoot.resolve("chat-attachments/12/c.jpg"), "old-c");
    Files.writeString(publicRoot.resolve("campaigns/1/main.png"), "public");
    // 비공개 위치에 같은 이름이 이미 있으면 덮어쓰지 않고 남겨 둡니다.
    Files.createDirectories(privateRoot.resolve("purchase-proof"));
    Files.writeString(privateRoot.resolve("purchase-proof/b.png"), "new-b");

    new UploadLocations(publicRoot.toString(), privateRoot.toString()).moveLegacyPrivateFiles();

    assertThat(Files.readString(privateRoot.resolve("purchase-proof/a.png"))).isEqualTo("old-a");
    assertThat(Files.readString(privateRoot.resolve("chat-attachments/12/c.jpg")))
        .isEqualTo("old-c");
    assertThat(Files.exists(publicRoot.resolve("purchase-proof/a.png"))).isFalse();
    assertThat(Files.readString(privateRoot.resolve("purchase-proof/b.png"))).isEqualTo("new-b");
    assertThat(Files.readString(publicRoot.resolve("purchase-proof/b.png"))).isEqualTo("old-b");
    // 공개 이미지는 그대로 둡니다.
    assertThat(Files.readString(publicRoot.resolve("campaigns/1/main.png"))).isEqualTo("public");
  }
}
