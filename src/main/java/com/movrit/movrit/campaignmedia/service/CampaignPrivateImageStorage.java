package com.movrit.movrit.campaignmedia.service;

import com.movrit.movrit.collaboration.service.CampaignImageStorageService;
import java.io.IOException;
import java.nio.file.*;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/** 새 이미지 관리 방식의 이미지 파일을 저장합니다. 누구나 접근할 수 있는 /uploads 폴더가 아닌 별도 폴더에 두고, 서버가 권한을 확인한 뒤에만 내려줍니다. */
/** 공개 /uploads 리소스 핸들러 밖의 형제 디렉터리에 표시본을 저장합니다. */
@Service
public class CampaignPrivateImageStorage {
  private final Path root;
  private final CampaignImageStorageService encoder;

  public CampaignPrivateImageStorage(@Value("${app.upload-dir:uploads}") String uploadDir) {
    Path publicRoot = Path.of(uploadDir).toAbsolutePath().normalize();
    root = publicRoot.resolveSibling(publicRoot.getFileName() + "-campaign-private");
    encoder = new CampaignImageStorageService(root.toString());
  }

  /** 저장된 이미지의 파일 이름(key)·형식·크기·가로·세로 정보입니다. */
  public record StoredImage(String key, String contentType, long byteSize, int width, int height) {}

  /** 이미지를 검사·재인코딩해 저장하고 파일 정보를 돌려줍니다. 저장 결과를 확인하지 못하면 파일을 지우고 500으로 응답합니다. */
  public StoredImage store(MultipartFile file) {
    String url = encoder.storeImage(file, "assets");
    String key = url.substring(url.lastIndexOf('/') + 1);
    Path path = resolve(key);
    try {
      var image = ImageIO.read(path.toFile());
      return new StoredImage(
          key,
          key.endsWith(".png") ? "image/png" : "image/jpeg",
          Files.size(path),
          image.getWidth(),
          image.getHeight());
    } catch (IOException | RuntimeException exception) {
      remove(key);
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "이미지 저장 결과를 확인할 수 없습니다.");
    }
  }

  /** 파일 이름으로 실제 저장 경로를 찾습니다. 서버가 만든 이름 형식(UUID.jpg|png)이 아니면 경로 조작을 막기 위해 404로 응답합니다. */
  public Path resolve(String key) {
    if (key == null || !key.matches("[0-9a-f-]{36}\\.(jpg|png)")) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "이미지를 찾을 수 없습니다.");
    }
    return root.resolve("assets").resolve(key);
  }

  /** 저장된 이미지 파일을 지웁니다. 이미 없거나 지우지 못해도 오류로 처리하지 않습니다. */
  public void remove(String key) {
    try {
      Files.deleteIfExists(resolve(key));
    } catch (IOException ignored) {
    }
  }
}
