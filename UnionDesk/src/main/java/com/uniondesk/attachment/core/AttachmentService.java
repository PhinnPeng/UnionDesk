package com.uniondesk.attachment.core;

import com.uniondesk.auth.core.UserContext;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AttachmentService {

    private static final Set<String> DEFAULT_ALLOWED_EXTENSIONS = Set.of(
            "pdf", "png", "jpg", "jpeg", "gif", "webp", "txt", "zip");

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    public AttachmentService(JdbcTemplate jdbcTemplate, Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
    }

    @Transactional
    public AttachmentUploadResult uploadAttachment(UserContext context, UploadAttachmentCommand command) {
        if (command.content() == null || command.content().length == 0) {
            throw new IllegalArgumentException("附件内容不能为空");
        }
        AttachmentPolicy policy = loadPolicy(command.businessDomainId());
        validateAttachment(command, policy);
        long uploaderSubjectId = context == null ? ensureAnonymousSubject() : ensureIdentitySubject(context.userId());
        String storageType = "local";
        String storageKey = StringUtils.hasText(command.storageKey()) ? command.storageKey() : buildStorageKey(command.fileName());
        Path storedPath = resolveStorageRoot().resolve(storageKey);
        try {
            Files.createDirectories(storedPath.getParent());
            Files.write(storedPath, command.content(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ex) {
            throw new IllegalStateException("附件写入失败", ex);
        }

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
                    storageType,
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
                    storageType,
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
        return new AttachmentUploadResult(attachmentId, storageType, storageKey, storedPath.toString(), checksum);
    }

    @Transactional
    public AttachmentPresignResult presignAttachment(PresignAttachmentCommand command) {
        String storageKey = buildStorageKey(command.fileName());
        jdbcTemplate.update("""
                        INSERT INTO file_attachment (
                            business_domain_id, uploader_subject_id, portal_type, file_name, mime_type,
                            file_size, storage_type, storage_key, checksum, status, created_at
                        )
                        VALUES (?, NULL, ?, ?, ?, ?, 'local', ?, NULL, 'pending', ?)
                        """,
                command.businessDomainId(),
                command.portalType(),
                command.fileName(),
                command.mimeType(),
                command.fileSize(),
                storageKey,
                LocalDateTime.now(clock));
        long attachmentId = loadLatestAttachmentId(storageKey);
        return new AttachmentPresignResult(attachmentId, "/api/v1/attachments/upload", 300);
    }

    @Transactional
    public void confirmAttachment(long attachmentId) {
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

    @Transactional(readOnly = true)
    public byte[] loadAttachmentContent(long attachmentId) {
        AttachmentFileView view = findAttachment(attachmentId);
        Path file = resolveStorageRoot().resolve(view.storageKey());
        try {
            return Files.readAllBytes(file);
        } catch (IOException ex) {
            throw new IllegalStateException("附件读取失败", ex);
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

    private Path resolveStorageRoot() {
        Path root = Path.of(System.getProperty("java.io.tmpdir"), "uniondesk", "attachments");
        try {
            Files.createDirectories(root);
        } catch (IOException ex) {
            throw new IllegalStateException("无法初始化本地附件目录", ex);
        }
        return root;
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
            String localPath,
            String checksum) {
    }

    public record AttachmentPresignResult(
            long attachmentId,
            String uploadUrl,
            int expiresInSeconds) {
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
