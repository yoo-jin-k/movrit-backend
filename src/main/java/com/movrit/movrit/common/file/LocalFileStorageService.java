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
import java.util.Map;
import java.util.UUID;

@Service
public class LocalFileStorageService {

    private static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024;
    private static final Map<String, String> EXTENSION_BY_CONTENT_TYPE = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp"
    );

    private final Path rootPath;

    public LocalFileStorageService(@Value("${app.upload-dir:uploads}") String uploadDir) {
        this.rootPath = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    /**
     * 원본 파일명은 저장 파일명으로 사용하지 않는다.
     * MIME 타입에서 허용 확장자를 직접 결정하면 악의적인 파일명이 그대로 노출되는 것도 막을 수 있다.
     */
    public String storeImage(MultipartFile file, String directory) {
        String extension = validateAndResolveExtension(file);
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

    private String validateAndResolveExtension(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미지 파일을 선택해줘.");
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미지는 5MB 이하만 업로드할 수 있어.");
        }

        String extension = EXTENSION_BY_CONTENT_TYPE.get(file.getContentType());
        if (extension == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "JPG, PNG, WEBP 이미지만 업로드할 수 있어.");
        }
        return extension;
    }
}
