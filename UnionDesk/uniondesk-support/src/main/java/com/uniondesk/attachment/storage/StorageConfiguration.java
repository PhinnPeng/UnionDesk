package com.uniondesk.attachment.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfiguration {

    private static final Logger log = LoggerFactory.getLogger(StorageConfiguration.class);

    @Bean
    ApplicationRunner ensureAttachmentBucket(AttachmentObjectStorage attachmentObjectStorage) {
        return args -> {
            try {
                attachmentObjectStorage.ensureBucket();
            }
            catch (Exception ex) {
                // 远程反代/无建桶权限时不阻断启动；附件上传前需确保存储桶已存在
                log.warn("附件存储桶校验失败，应用继续启动：{}", ex.getMessage());
            }
        };
    }
}
