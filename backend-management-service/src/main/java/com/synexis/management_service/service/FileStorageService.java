package com.synexis.management_service.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String storeProfilePicture(MultipartFile file);
}
