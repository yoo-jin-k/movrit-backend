package com.movrit.movrit.common.file;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 업로드 파일을 두는 두 위치를 정하고 서로 섞이지 않게 지킵니다.
 *
 * <ul>
 *   <li>공개 위치(app.upload-dir): 캠페인·브랜드·상품 이미지처럼 누구나 볼 수 있는 파일. /uploads 주소로 내려갑니다.
 *   <li>비공개 위치(app.private-upload-dir, 기본값은 공개 위치 옆의 "{공개 위치}-private"): 구매 인증·취소 증빙·채팅 첨부·채널 확인
 *       자료·프로필 사진. 주소로는 열 수 없고 권한을 확인하는 API로만 내려갑니다.
 * </ul>
 *
 * <p>두 위치가 같거나 한쪽이 다른 쪽 안에 있으면 서버를 띄우지 않습니다(설정 실수로 비공개 파일이 다시 공개되는 일을 막기 위해서).
 *
 * <p>예전에는 비공개 파일도 공개 위치 아래에 저장했습니다. 서버가 뜰 때 공개 위치에 남아 있는 비공개 폴더의 파일을 비공개 위치로 옮깁니다(지우지 않고 옮기기만 하며,
 * 같은 이름의 파일이 이미 있으면 건드리지 않고 남겨 둡니다).
 */
@Slf4j
@Component
public class UploadLocations {

  /** 비공개 파일 폴더 이름입니다. 각 저장소가 비공개 위치 아래에 이 이름으로 폴더를 만듭니다. */
  public static final List<String> PRIVATE_FOLDERS =
      List.of(
          "purchase-proof",
          "cancel-evidence",
          "chat-attachments",
          "channel-evidence",
          "member-avatars");

  private final Path publicRoot;
  private final Path privateRoot;

  public UploadLocations(
      @Value("${app.upload-dir:uploads}") String uploadDir,
      @Value("${app.private-upload-dir:uploads-private}") String privateUploadDir) {
    this.publicRoot = Path.of(uploadDir).toAbsolutePath().normalize();
    this.privateRoot = Path.of(privateUploadDir).toAbsolutePath().normalize();
    if (publicRoot.startsWith(privateRoot) || privateRoot.startsWith(publicRoot)) {
      throw new IllegalStateException(
          "비공개 업로드 위치(app.private-upload-dir)는 공개 업로드 위치(app.upload-dir)와 겹치지 않는 별도 폴더여야 합니다.");
    }
  }

  public Path publicRoot() {
    return publicRoot;
  }

  public Path privateRoot() {
    return privateRoot;
  }

  /** 서버가 뜰 때 한 번, 공개 위치에 남은 예전 비공개 파일을 비공개 위치로 옮깁니다. */
  @PostConstruct
  void moveLegacyPrivateFiles() {
    MoveResult total = new MoveResult(0, 0);
    for (String folder : PRIVATE_FOLDERS) {
      total = total.plus(moveFolder(publicRoot.resolve(folder), privateRoot.resolve(folder)));
    }
    if (total.moved() > 0 || total.skipped() > 0) {
      // 파일 이름·경로는 남기지 않고 개수만 남깁니다.
      log.info(
          "예전 비공개 업로드 파일을 비공개 위치로 옮겼습니다: {}건, 같은 이름이 있어 남겨 둔 파일 {}건",
          total.moved(),
          total.skipped());
    }
  }

  /** 한 폴더 안의 파일을 같은 상대 경로로 옮깁니다. 이미 있는 파일은 덮어쓰지 않습니다. */
  static MoveResult moveFolder(Path source, Path target) {
    if (!Files.isDirectory(source)) return new MoveResult(0, 0);
    int moved = 0;
    int skipped = 0;
    try (Stream<Path> files = Files.walk(source)) {
      for (Path file : files.filter(Files::isRegularFile).toList()) {
        Path destination = target.resolve(source.relativize(file)).normalize();
        if (!destination.startsWith(target) || Files.exists(destination)) {
          skipped++;
          continue;
        }
        Files.createDirectories(destination.getParent());
        Files.move(file, destination);
        moved++;
      }
    } catch (IOException exception) {
      throw new UncheckedIOException("예전 비공개 업로드 파일을 옮기지 못했습니다.", exception);
    }
    return new MoveResult(moved, skipped);
  }

  /** 옮긴 파일 수와, 같은 이름이 있어 남겨 둔 파일 수입니다. */
  record MoveResult(int moved, int skipped) {
    MoveResult plus(MoveResult other) {
      return new MoveResult(moved + other.moved, skipped + other.skipped);
    }
  }
}
