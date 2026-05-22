package com.uniondesk.attachment.storage;

import java.net.URI;
import java.time.Duration;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Component
public class S3MinioAttachmentStorage implements AttachmentObjectStorage {

    private final StorageProperties properties;
    private final S3Client s3Client;
    private final S3Presigner presigner;

    public S3MinioAttachmentStorage(StorageProperties properties) {
        properties.validate();
        this.properties = properties;
        AwsBasicCredentials credentials = AwsBasicCredentials.create(properties.accessKey(), properties.secretKey());
        StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(credentials);
        S3Configuration serviceConfiguration = S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                .build();
        URI endpoint = URI.create(properties.endpoint());
        Region region = Region.of(properties.region());
        this.s3Client = S3Client.builder()
                .endpointOverride(endpoint)
                .region(region)
                .credentialsProvider(credentialsProvider)
                .serviceConfiguration(serviceConfiguration)
                .build();
        this.presigner = S3Presigner.builder()
                .endpointOverride(endpoint)
                .region(region)
                .credentialsProvider(credentialsProvider)
                .serviceConfiguration(serviceConfiguration)
                .build();
    }

    @Override
    public void ensureBucket() {
        String bucket = properties.bucket();
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
        } catch (NoSuchBucketException ex) {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
        }
    }

    @Override
    public void putObject(String storageKey, byte[] content, String contentType) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(properties.bucket())
                .key(storageKey)
                .contentType(contentType)
                .contentLength((long) content.length)
                .build();
        s3Client.putObject(request, software.amazon.awssdk.core.sync.RequestBody.fromBytes(content));
    }

    @Override
    public PresignedUrl presignPut(String storageKey, String contentType, long fileSize) {
        Duration expiry = Duration.ofSeconds(properties.presignExpirySeconds());
        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(properties.bucket())
                .key(storageKey)
                .contentType(contentType)
                .contentLength(fileSize)
                .build();
        var presigned = presigner.presignPutObject(PutObjectPresignRequest.builder()
                .signatureDuration(expiry)
                .putObjectRequest(objectRequest)
                .build());
        return new PresignedUrl(presigned.url().toString(), properties.presignExpirySeconds());
    }

    @Override
    public PresignedUrl presignGet(String storageKey) {
        Duration expiry = Duration.ofSeconds(properties.presignExpirySeconds());
        GetObjectRequest objectRequest = GetObjectRequest.builder()
                .bucket(properties.bucket())
                .key(storageKey)
                .build();
        var presigned = presigner.presignGetObject(GetObjectPresignRequest.builder()
                .signatureDuration(expiry)
                .getObjectRequest(objectRequest)
                .build());
        return new PresignedUrl(presigned.url().toString(), properties.presignExpirySeconds());
    }

    @Override
    public ObjectHead headObject(String storageKey) {
        try {
            HeadObjectResponse response = s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(properties.bucket())
                    .key(storageKey)
                    .build());
            return new ObjectHead(true, response.contentLength());
        } catch (NoSuchKeyException ex) {
            return new ObjectHead(false, 0L);
        }
    }
}
