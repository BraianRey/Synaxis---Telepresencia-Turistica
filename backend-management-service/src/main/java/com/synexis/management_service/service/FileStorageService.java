package com.synexis.management_service.service;

import java.util.Set;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String storeProfilePicture(MultipartFile file);
}
