package com.uniondesk.attachment.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "uniondesk.storage")
public class StorageProperties {

    private String endpoint = "http://127.0.0.1:9000";
    private String accessKey = "comm_minio";
    private String secretKey = "";
    private String bucket = "uniondesk-attachments";
    private String region = "us-east-1";
    private int presignExpirySeconds = 300;

    public String endpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String accessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String secretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String bucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String region() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public int presignExpirySeconds() {
        return presignExpirySeconds;
    }

    public void setPresignExpirySeconds(int presignExpirySeconds) {
        this.presignExpirySeconds = presignExpirySeconds;
    }

    public void validate() {
        if (!StringUtils.hasText(endpoint)) {
            throw new IllegalStateException("uniondesk.storage.endpoint 未配置");
        }
        if (!StringUtils.hasText(accessKey)) {
            throw new IllegalStateException("uniondesk.storage.access-key 未配置");
        }
        if (!StringUtils.hasText(secretKey)) {
            throw new IllegalStateException("uniondesk.storage.secret-key 未配置，请设置环境变量 MINIO_SECRET_KEY");
        }
        if (!StringUtils.hasText(bucket)) {
            throw new IllegalStateException("uniondesk.storage.bucket 未配置");
        }
        if (presignExpirySeconds <= 0) {
            throw new IllegalStateException("uniondesk.storage.presign-expiry-seconds 必须大于 0");
        }
    }
}
