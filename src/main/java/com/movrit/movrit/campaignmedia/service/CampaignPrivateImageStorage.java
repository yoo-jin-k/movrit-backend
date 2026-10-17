package com.movrit.movrit.campaignmedia.service;

import com.movrit.movrit.collaboration.service.CampaignImageStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.nio.file.*;

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
    public record StoredImage(String key, String contentType, long byteSize, int width, int height) { }
    public StoredImage store(MultipartFile file) {
        String url = encoder.storeImage(file, "assets");
        String key = url.substring(url.lastIndexOf('/') + 1);
        Path path = resolve(key);
        try {
            var image = ImageIO.read(path.toFile());
            return new StoredImage(key, key.endsWith(".png") ? "image/png" : "image/jpeg",
                    Files.size(path), image.getWidth(), image.getHeight());
        } catch (IOException | RuntimeException exception) {
            remove(key);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "이미지 저장 결과를 확인할 수 없습니다.");
        }
    }
    public Path resolve(String key) {
        if (key == null || !key.matches("[0-9a-f-]{36}\\.(jpg|png)")) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "이미지를 찾을 수 없습니다.");
        }
        return root.resolve("assets").resolve(key);
    }
    public void remove(String key) {
        try { Files.deleteIfExists(resolve(key)); } catch (IOException ignored) { }
    }
}
