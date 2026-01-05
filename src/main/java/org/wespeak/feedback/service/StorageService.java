package org.wespeak.feedback.service;

import java.io.InputStream;

public interface StorageService {
    InputStream downloadFile(String key);
    String uploadFile(String key, InputStream content, long contentLength, String contentType);
    String generatePresignedUrl(String key);
}
