package com.sdfds.service;

import org.springframework.web.multipart.MultipartFile;

public interface UploadPolicy {

    void validate(MultipartFile file);
}