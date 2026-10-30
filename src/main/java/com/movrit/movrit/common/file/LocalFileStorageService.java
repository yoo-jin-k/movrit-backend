package com.movrit.movrit.common.file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/** 이미지를 서버 로컬 폴더(app.upload-dir)에 저장하는 공통 서비스입니다. JPG·PNG·WEBP 5MB 이하만 허용합니다. */
@Service
public class LocalFileStorageService {

  private static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024;
  private static final Map<String, String> EXTENSION_BY_CONTENT_TYPE =
      Map.of(
          "image/jpeg", ".jpg",
          "image/png", ".png",
          "image/webp", ".webp");

  private final Path rootPath;

  /** 업로드 폴더 경로를 절대 경로로 정리해 저장 기준 위치로 삼습니다. */
  public LocalFileStorageService(@Value("${app.upload-dir:uploads}") String uploadDir) {
    this.rootPath = Path.of(uploadDir).toAbsolutePath().normalize();
  }

  /**
   * 이미지를 검사한 뒤 임의 이름(UUID)으로 저장하고 /uploads/... 공개 주소를 반환합니다. 원본 파일명은 쓰지 않고 파일 형식으로 확장자를 정해 악성 파일명을
   * 막습니다. 업로드 폴더 밖으로 나가는 경로는 거절합니다.
   */
  public String storeImage(MultipartFile file, String directory) {
    String extension = validateAndResolveExtension(file);
    String storedName = UUID.randomUUID() + extension;
    Path targetDirectory = rootPath.resolve(directory).normalize();

    if (!targetDirectory.startsWith(rootPath)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "잘못된 업로드 경로입니다.");
    }

    try {
      Files.createDirectories(targetDirectory);
      Files.copy(
          file.getInputStream(),
          targetDirectory.resolve(storedName),
          StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "이미지 저장에 실패했습니다.");
    }

    return "/uploads/" + directory + "/" + storedName;
  }

  private String validateAndResolveExtension(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미지 파일을 선택해 주세요.");
    }
    if (file.getSize() > MAX_IMAGE_SIZE) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미지는 5MB 이하만 업로드할 수 있습니다.");
    }

    String extension = EXTENSION_BY_CONTENT_TYPE.get(file.getContentType());
    if (extension == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "JPG, PNG, WEBP 이미지만 업로드할 수 있습니다.");
    }
    return extension;
  }
}
