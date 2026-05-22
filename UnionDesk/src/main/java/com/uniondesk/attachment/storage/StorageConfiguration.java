package com.uniondesk.attachment.storage;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfiguration {

    @Bean
    ApplicationRunner ensureAttachmentBucket(AttachmentObjectStorage attachmentObjectStorage) {
        return args -> attachmentObjectStorage.ensureBucket();
    }
}
