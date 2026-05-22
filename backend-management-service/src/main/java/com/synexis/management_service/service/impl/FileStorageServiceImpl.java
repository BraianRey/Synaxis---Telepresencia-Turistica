package com.synexis.management_service.service.impl;

import com.synexis.management_service.service.FileStorageService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    private final Path uploadDirectory;

    public FileStorageServiceImpl() {
        this.uploadDirectory = Paths.get("picProfile").toAbsolutePath();
    }

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp");
    private static final long MAX_SIZE = 5 * 1024 * 1024; // 5 MB

    @Override
    public String storeProfilePicture(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new IllegalArgumentException(
                    "Unsupported file type: " + contentType + ". Allowed: JPEG, PNG, WebP");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new IllegalArgumentException(
                    "File size exceeds maximum allowed (5 MB)");
        }

        String extension = switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };

        String filename = UUID.randomUUID().toString() + extension;
        Path targetPath = uploadDirectory.resolve(filename);

        try {
            Files.createDirectories(uploadDirectory);
            file.transferTo(targetPath.toFile());
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file: " + e.getMessage(), e);
        }

        return "picProfile/" + filename;
    }
}
