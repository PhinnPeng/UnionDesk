package com.uniondesk.attachment.core;

import com.uniondesk.attachment.storage.AttachmentObjectStorage;
import com.uniondesk.auth.core.UserContext;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AttachmentService {

    public static final String STORAGE_TYPE_OBJECT = "object_storage";

    private static final Set<String> DEFAULT_ALLOWED_EXTENSIONS = Set.of(
            "pdf", "png", "jpg", "jpeg", "gif", "webp", "txt", "zip");

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;
    private final AttachmentObjectStorage objectStorage;

    public AttachmentService(
            JdbcTemplate jdbcTemplate,
            Clock clock,
            AttachmentObjectStorage objectStorage) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
        this.objectStorage = objectStorage;
    }

    @Transactional
    public AttachmentUploadResult uploadAttachment(UserContext context, UploadAttachmentCommand command) {
        if (command.content() == null || command.content().length == 0) {
            throw new IllegalArgumentException("附件内容不能为空");
        }
        AttachmentPolicy policy = loadPolicy(command.businessDomainId());
        validateAttachment(command, policy);
        long uploaderSubjectId = context == null ? ensureAnonymousSubject() : ensureIdentitySubject(context.userId());
        String storageKey = StringUtils.hasText(command.storageKey()) ? command.storageKey() : buildStorageKey(command.fileName());
        String contentType = StringUtils.hasText(command.contentType())
                ? command.contentType()
                : "application/octet-stream";
        objectStorage.putObject(storageKey, command.content(), contentType);

        String checksum = checksum(command.content());
        long attachmentId;
        if (command.attachmentId() != null) {
            jdbcTemplate.update("""
                            UPDATE file_attachment
                            SET business_domain_id = ?,
                                uploader_subject_id = ?,
                                portal_type = ?,
                                file_name = ?,
                                mime_type = ?,
                                file_size = ?,
                                storage_type = ?,
                                storage_key = ?,
                                checksum = ?,
                                status = 'confirmed'
                            WHERE id = ?
                            """,
                    command.businessDomainId(),
                    uploaderSubjectId,
                    command.portalType(),
                    command.fileName(),
                    command.contentType(),
                    command.content().length,
                    STORAGE_TYPE_OBJECT,
                    storageKey,
                    checksum,
                    command.attachmentId());
            attachmentId = command.attachmentId();
        } else {
            jdbcTemplate.update("""
                            INSERT INTO file_attachment (
                                business_domain_id, uploader_subject_id, portal_type, file_name, mime_type,
                                file_size, storage_type, storage_key, checksum, status, created_at
                            )
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'confirmed', ?)
                            """,
                    command.businessDomainId(),
                    uploaderSubjectId,
                    command.portalType(),
                    command.fileName(),
                    command.contentType(),
                    command.content().length,
                    STORAGE_TYPE_OBJECT,
                    storageKey,
                    checksum,
                    LocalDateTime.now(clock));
            attachmentId = loadLatestAttachmentId(storageKey);
        }
        if (command.targetType() != null && command.targetId() != null) {
            jdbcTemplate.update("""
                            INSERT INTO attachment_ref (attachment_id, target_type, target_id, relation_type)
                            VALUES (?, ?, ?, ?)
                            """,
                    attachmentId,
                    command.targetType(),
                    command.targetId(),
                    command.relationType() == null ? "primary" : command.relationType());
        }
        return new AttachmentUploadResult(attachmentId, STORAGE_TYPE_OBJECT, storageKey, checksum);
    }

    @Transactional
    public AttachmentPresignResult presignAttachment(PresignAttachmentCommand command) {
        validatePresignMetadata(command);
        AttachmentPolicy policy = loadPolicy(command.businessDomainId());
        validatePresignSize(command.fileSize(), policy);
        String storageKey = buildStorageKey(command.fileName());
        jdbcTemplate.update("""
                        INSERT INTO file_attachment (
                            business_domain_id, uploader_subject_id, portal_type, file_name, mime_type,
                            file_size, storage_type, storage_key, checksum, status, created_at
                        )
                        VALUES (?, NULL, ?, ?, ?, ?, ?, ?, NULL, 'pending', ?)
                        """,
                command.businessDomainId(),
                command.portalType(),
                command.fileName(),
                command.mimeType(),
                command.fileSize(),
                STORAGE_TYPE_OBJECT,
                storageKey,
                LocalDateTime.now(clock));
        long attachmentId = loadLatestAttachmentId(storageKey);
        AttachmentObjectStorage.PresignedUrl presigned = objectStorage.presignPut(
                storageKey,
                command.mimeType(),
                command.fileSize());
        return new AttachmentPresignResult(attachmentId, presigned.url(), presigned.expiresInSeconds());
    }

    @Transactional
    public void confirmAttachment(long attachmentId) {
        AttachmentFileView view = findAttachment(attachmentId);
        requireObjectStorage(view);
        AttachmentObjectStorage.ObjectHead head = objectStorage.headObject(view.storageKey());
        if (!head.exists()) {
            throw new IllegalArgumentException("对象存储中未找到附件，请先完成上传");
        }
        if (head.size() != view.fileSize()) {
            throw new IllegalArgumentException("附件大小与登记信息不一致");
        }
        int updated = jdbcTemplate.update("""
                        UPDATE file_attachment
                        SET status = 'confirmed'
                        WHERE id = ?
                        """,
                attachmentId);
        if (updated == 0) {
            throw new IllegalArgumentException("attachment not found");
        }
    }

    @Transactional(readOnly = true)
    public AttachmentDownloadAccess resolveDownloadAccess(long attachmentId) {
        AttachmentFileView view = findAttachment(attachmentId);
        requireObjectStorage(view);
        AttachmentObjectStorage.PresignedUrl presigned = objectStorage.presignGet(view.storageKey());
        return new AttachmentDownloadAccess(presigned.url(), presigned.expiresInSeconds(), STORAGE_TYPE_OBJECT);
    }

    @Transactional
    public void linkAttachmentsToTicket(long ticketId, List<Long> attachmentIds, String relationType) {
        linkAttachments("ticket", ticketId, attachmentIds, relationType);
    }

    @Transactional
    public void linkAttachments(String targetType, long targetId, List<Long> attachmentIds, String relationType) {
        if (attachmentIds == null || attachmentIds.isEmpty()) {
            return;
        }
        for (Long attachmentId : attachmentIds) {
            if (attachmentId == null) {
                continue;
            }
            jdbcTemplate.update("""
                            INSERT INTO attachment_ref (attachment_id, target_type, target_id, relation_type)
                            VALUES (?, ?, ?, ?)
                            """,
                    attachmentId,
                    targetType,
                    targetId,
                    relationType == null ? "primary" : relationType);
        }
    }

    @Transactional(readOnly = true)
    public AttachmentFileView findAttachment(long attachmentId) {
        return jdbcTemplate.queryForObject("""
                        SELECT
                            id,
                            business_domain_id,
                            uploader_subject_id,
                            portal_type,
                            file_name,
                            mime_type,
                            file_size,
                            storage_type,
                            storage_key,
                            checksum,
                            created_at,
                            deleted_at
                        FROM file_attachment
                        WHERE id = ?
                        LIMIT 1
                        """,
                (rs, rowNum) -> new AttachmentFileView(
                        rs.getLong("id"),
                        rs.getObject("business_domain_id", Long.class),
                        rs.getObject("uploader_subject_id", Long.class),
                        rs.getString("portal_type"),
                        rs.getString("file_name"),
                        rs.getString("mime_type"),
                        rs.getLong("file_size"),
                        rs.getString("storage_type"),
                        rs.getString("storage_key"),
                        rs.getString("checksum"),
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        rs.getTimestamp("deleted_at") == null ? null : rs.getTimestamp("deleted_at").toLocalDateTime()),
                attachmentId);
    }

    private void requireObjectStorage(AttachmentFileView view) {
        if ("local".equals(view.storageType())) {
            throw new IllegalStateException("本地存储附件已不再支持，请联系管理员迁移到对象存储");
        }
        if (!STORAGE_TYPE_OBJECT.equals(view.storageType())) {
            throw new IllegalStateException("不支持的附件存储类型: " + view.storageType());
        }
    }

    private void validateAttachment(UploadAttachmentCommand command, AttachmentPolicy policy) {
        String extension = fileExtension(command.fileName());
        if (!policy.allowedExtensions().contains(extension)) {
            throw new IllegalArgumentException("附件类型不在白名单内");
        }
        long maxBytes = policy.maxSingleSizeMb() * 1024L * 1024L;
        if (command.content().length > maxBytes) {
            throw new IllegalArgumentException("附件大小超限");
        }
    }

    private void validatePresignMetadata(PresignAttachmentCommand command) {
        String extension = fileExtension(command.fileName());
        AttachmentPolicy policy = loadPolicy(command.businessDomainId());
        if (!policy.allowedExtensions().contains(extension)) {
            throw new IllegalArgumentException("附件类型不在白名单内");
        }
    }

    private void validatePresignSize(long fileSize, AttachmentPolicy policy) {
        long maxBytes = policy.maxSingleSizeMb() * 1024L * 1024L;
        if (fileSize <= 0) {
            throw new IllegalArgumentException("附件大小无效");
        }
        if (fileSize > maxBytes) {
            throw new IllegalArgumentException("附件大小超限");
        }
    }

    private AttachmentPolicy loadPolicy(Long businessDomainId) {
        if (businessDomainId != null) {
            try {
                return jdbcTemplate.queryForObject("""
                                SELECT allowed_types_json, max_single_size_mb, max_total_size_mb
                                FROM attachment_policy
                                WHERE scope_type = 'domain' AND scope_id = ?
                                LIMIT 1
                                """,
                        (rs, rowNum) -> new AttachmentPolicy(
                                parseAllowedExtensions(rs.getString("allowed_types_json")),
                                rs.getInt("max_single_size_mb") == 0 ? 20 : rs.getInt("max_single_size_mb"),
                                rs.getInt("max_total_size_mb") == 0 ? 200 : rs.getInt("max_total_size_mb")),
                        businessDomainId);
            } catch (EmptyResultDataAccessException ignored) {
                // fall through
            }
        }
        try {
            return jdbcTemplate.queryForObject("""
                            SELECT allowed_types_json, max_single_size_mb, max_total_size_mb
                            FROM attachment_policy
                            WHERE scope_type = 'platform' AND scope_id = 0
                            LIMIT 1
                            """,
                    (rs, rowNum) -> new AttachmentPolicy(
                            parseAllowedExtensions(rs.getString("allowed_types_json")),
                            rs.getInt("max_single_size_mb") == 0 ? 20 : rs.getInt("max_single_size_mb"),
                            rs.getInt("max_total_size_mb") == 0 ? 200 : rs.getInt("max_total_size_mb")),
                    0L);
        } catch (EmptyResultDataAccessException ex) {
            return new AttachmentPolicy(DEFAULT_ALLOWED_EXTENSIONS, 20, 200);
        }
    }

    private Set<String> parseAllowedExtensions(String json) {
        if (!StringUtils.hasText(json)) {
            return DEFAULT_ALLOWED_EXTENSIONS;
        }
        try {
            @SuppressWarnings("unchecked")
            List<String> raw = new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, List.class);
            return raw.stream()
                    .filter(StringUtils::hasText)
                    .map(value -> value.trim().toLowerCase(Locale.ROOT))
                    .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
        } catch (Exception ex) {
            return DEFAULT_ALLOWED_EXTENSIONS;
        }
    }

    private String fileExtension(String fileName) {
        if (!StringUtils.hasText(fileName) || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).trim().toLowerCase(Locale.ROOT);
    }

    private String buildStorageKey(String fileName) {
        String safeName = StringUtils.hasText(fileName) ? fileName.replaceAll("[\\\\/:*?\"<>|]", "_") : "attachment.bin";
        return LocalDateTime.now(clock).toLocalDate().toString() + "/" + UUID.randomUUID() + "-" + safeName;
    }

    private String checksum(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(content));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("checksum 计算失败", ex);
        }
    }

    private long loadLatestAttachmentId(String storageKey) {
        Long attachmentId = jdbcTemplate.queryForObject("""
                        SELECT id
                        FROM file_attachment
                        WHERE storage_key = ?
                        ORDER BY id DESC
                        LIMIT 1
                        """,
                Long.class,
                storageKey);
        if (attachmentId == null) {
            throw new IllegalStateException("attachment not found");
        }
        return attachmentId;
    }

    private long ensureIdentitySubject(long userId) {
        Long subjectId = jdbcTemplate.queryForObject("""
                        SELECT id
                        FROM identity_subject
                        WHERE id = ?
                        LIMIT 1
                        """,
                Long.class,
                userId);
        if (subjectId != null) {
            return subjectId;
        }
        String phone = jdbcTemplate.queryForObject("""
                        SELECT COALESCE(NULLIF(mobile, ''), CONCAT('user-', id))
                        FROM user_account
                        WHERE id = ?
                        LIMIT 1
                        """,
                String.class,
                userId);
        if (!StringUtils.hasText(phone)) {
            phone = "user-" + userId;
        }
        jdbcTemplate.update("""
                        INSERT INTO identity_subject (id, subject_type, phone, status)
                        VALUES (?, 'person', ?, 'active')
                        """,
                userId,
                phone);
        return userId;
    }

    private long ensureAnonymousSubject() {
        return 0L;
    }

    private record AttachmentPolicy(Set<String> allowedExtensions, int maxSingleSizeMb, int maxTotalSizeMb) {
    }

    public record UploadAttachmentCommand(
            Long businessDomainId,
            String portalType,
            String fileName,
            String contentType,
            byte[] content,
            String targetType,
            Long targetId,
            String relationType,
            String storageKey,
            Long attachmentId) {
        public UploadAttachmentCommand(
                Long businessDomainId,
                String portalType,
                String fileName,
                String contentType,
                byte[] content,
                String targetType,
                Long targetId,
                String relationType) {
            this(businessDomainId, portalType, fileName, contentType, content, targetType, targetId, relationType, null, null);
        }
    }

    public record AttachmentUploadResult(
            long attachmentId,
            String storageType,
            String storageKey,
            String checksum) {
    }

    public record AttachmentPresignResult(
            long attachmentId,
            String uploadUrl,
            int expiresInSeconds) {
    }

    public record AttachmentDownloadAccess(
            String downloadUrl,
            int expiresInSeconds,
            String storageType) {
    }

    public record PresignAttachmentCommand(
            Long businessDomainId,
            String portalType,
            String fileName,
            String mimeType,
            long fileSize,
            String targetType) {
    }

    public record AttachmentFileView(
            long id,
            Long businessDomainId,
            Long uploaderSubjectId,
            String portalType,
            String fileName,
            String mimeType,
            long fileSize,
            String storageType,
            String storageKey,
            String checksum,
            LocalDateTime createdAt,
            LocalDateTime deletedAt) {
    }
}
