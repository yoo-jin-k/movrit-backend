package com.movrit.movrit.common.file;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
public class LocalFileStorageService {

    private static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final Path rootPath;

    public LocalFileStorageService(@Value("${app.upload-dir:uploads}") String uploadDir) {
        this.rootPath = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    /**
     * 업로드 파일명은 사용자가 보낸 이름을 그대로 저장하지 않는다.
     * UUID 기반 이름을 사용하면 한글/특수문자 문제와 같은 이름의 파일 덮어쓰기를 피할 수 있다.
     */
    public String storeImage(MultipartFile file, String directory) {
        validateImage(file);

        String extension = extensionOf(file.getOriginalFilename());
        String storedName = UUID.randomUUID() + extension;
        Path targetDirectory = rootPath.resolve(directory).normalize();

        if (!targetDirectory.startsWith(rootPath)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "잘못된 업로드 경로야.");
        }

        try {
            Files.createDirectories(targetDirectory);
            Files.copy(file.getInputStream(), targetDirectory.resolve(storedName), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "이미지 저장에 실패했어.");
        }

        return "/uploads/" + directory + "/" + storedName;
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미지 파일을 선택해줘.");
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미지는 5MB 이하만 업로드할 수 있어.");
        }
        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "JPG, PNG, WEBP 이미지만 업로드할 수 있어.");
        }
    }

    private String extensionOf(String originalName) {
        if (originalName == null) return ".img";
        int dot = originalName.lastIndexOf('.');
        return dot >= 0 ? originalName.substring(dot).toLowerCase() : ".img";
    }
}
