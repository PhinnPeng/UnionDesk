package com.uniondesk.attachment.storage;

public interface AttachmentObjectStorage {

    void ensureBucket();

    void putObject(String storageKey, byte[] content, String contentType);

    PresignedUrl presignPut(String storageKey, String contentType, long fileSize);

    PresignedUrl presignGet(String storageKey);

    ObjectHead headObject(String storageKey);

    record PresignedUrl(String url, int expiresInSeconds) {
    }

    record ObjectHead(boolean exists, long size) {
    }
}
