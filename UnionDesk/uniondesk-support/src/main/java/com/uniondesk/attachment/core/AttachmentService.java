package com.uniondesk.attachment.core;

import com.uniondesk.attachment.entity.AttachmentRefPo;
import com.uniondesk.attachment.entity.AttachmentPolicyPo;
import com.uniondesk.attachment.entity.FileAttachmentPo;
import com.uniondesk.attachment.repository.AttachmentPolicyRepository;
import com.uniondesk.attachment.repository.AttachmentRefRepository;
import com.uniondesk.attachment.repository.FileAttachmentRepository;
import com.uniondesk.attachment.storage.AttachmentObjectStorage;
import com.uniondesk.auth.core.UserContext;
import com.uniondesk.common.repository.IdentityResolutionRepository;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AttachmentService {

    public static final String STORAGE_TYPE_OBJECT = "object_storage";

    private static final Set<String> DEFAULT_ALLOWED_EXTENSIONS = Set.of(
            "pdf", "png", "jpg", "jpeg", "gif", "webp", "txt", "zip");

    private final FileAttachmentRepository fileAttachmentRepository;
    private final AttachmentRefRepository attachmentRefRepository;
    private final AttachmentPolicyRepository attachmentPolicyRepository;
    private final IdentityResolutionRepository identityResolutionRepository;
    private final Clock clock;
    private final AttachmentObjectStorage objectStorage;

    public AttachmentService(
            FileAttachmentRepository fileAttachmentRepository,
            AttachmentRefRepository attachmentRefRepository,
            AttachmentPolicyRepository attachmentPolicyRepository,
            IdentityResolutionRepository identityResolutionRepository,
            Clock clock,
            AttachmentObjectStorage objectStorage) {
        this.fileAttachmentRepository = fileAttachmentRepository;
        this.attachmentRefRepository = attachmentRefRepository;
        this.attachmentPolicyRepository = attachmentPolicyRepository;
        this.identityResolutionRepository = identityResolutionRepository;
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
        long uploaderSubjectId = context == null ? ensureAnonymousSubject() : identityResolutionRepository.ensureIdentitySubject(context.userId());
        String storageKey = StringUtils.hasText(command.storageKey()) ? command.storageKey() : buildStorageKey(command.fileName());
        String contentType = StringUtils.hasText(command.contentType())
                ? command.contentType()
                : "application/octet-stream";
        objectStorage.putObject(storageKey, command.content(), contentType);

        String checksum = checksum(command.content());
        long attachmentId;
        if (command.attachmentId() != null) {
            FileAttachmentPo po = new FileAttachmentPo();
            po.setId(command.attachmentId());
            po.setBusinessDomainId(command.businessDomainId());
            po.setUploaderSubjectId(uploaderSubjectId);
            po.setPortalType(command.portalType());
            po.setFileName(command.fileName());
            po.setMimeType(command.contentType());
            po.setFileSize((long) command.content().length);
            po.setStorageType(STORAGE_TYPE_OBJECT);
            po.setStorageKey(storageKey);
            po.setChecksum(checksum);
            fileAttachmentRepository.updateConfirmed(po);
            attachmentId = command.attachmentId();
        } else {
            FileAttachmentPo po = new FileAttachmentPo();
            po.setBusinessDomainId(command.businessDomainId());
            po.setUploaderSubjectId(uploaderSubjectId);
            po.setPortalType(command.portalType());
            po.setFileName(command.fileName());
            po.setMimeType(command.contentType());
            po.setFileSize((long) command.content().length);
            po.setStorageType(STORAGE_TYPE_OBJECT);
            po.setStorageKey(storageKey);
            po.setChecksum(checksum);
            po.setStatus("confirmed");
            po.setCreatedAt(LocalDateTime.now(clock));
            fileAttachmentRepository.save(po);
            attachmentId = po.getId() == null ? fileAttachmentRepository.findLatestIdByStorageKey(storageKey) : po.getId();
        }
        if (command.targetType() != null && command.targetId() != null) {
            saveAttachmentRef(attachmentId, command.targetType(), command.targetId(), command.relationType());
        }
        return new AttachmentUploadResult(attachmentId, STORAGE_TYPE_OBJECT, storageKey, checksum);
    }

    @Transactional
    public AttachmentPresignResult presignAttachment(PresignAttachmentCommand command) {
        validatePresignMetadata(command);
        AttachmentPolicy policy = loadPolicy(command.businessDomainId());
        validatePresignSize(command.fileSize(), policy);
        String storageKey = buildStorageKey(command.fileName());
        FileAttachmentPo po = new FileAttachmentPo();
        po.setBusinessDomainId(command.businessDomainId());
        po.setPortalType(command.portalType());
        po.setFileName(command.fileName());
        po.setMimeType(command.mimeType());
        po.setFileSize(command.fileSize());
        po.setStorageType(STORAGE_TYPE_OBJECT);
        po.setStorageKey(storageKey);
        po.setStatus("pending");
        po.setCreatedAt(LocalDateTime.now(clock));
        fileAttachmentRepository.save(po);
        long attachmentId = po.getId() == null ? fileAttachmentRepository.findLatestIdByStorageKey(storageKey) : po.getId();
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
        int updated = fileAttachmentRepository.updateStatus(attachmentId, "confirmed");
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
            saveAttachmentRef(attachmentId, targetType, targetId, relationType);
        }
    }

    @Transactional(readOnly = true)
    public AttachmentFileView findAttachment(long attachmentId) {
        FileAttachmentPo po = fileAttachmentRepository.findById(attachmentId);
        return toView(po);
    }

    private void saveAttachmentRef(long attachmentId, String targetType, long targetId, String relationType) {
        AttachmentRefPo refPo = new AttachmentRefPo();
        refPo.setAttachmentId(attachmentId);
        refPo.setTargetType(targetType);
        refPo.setTargetId(targetId);
        refPo.setRelationType(relationType == null ? "primary" : relationType);
        attachmentRefRepository.save(refPo);
    }

    private AttachmentFileView toView(FileAttachmentPo po) {
        return new AttachmentFileView(
                po.getId(),
                po.getBusinessDomainId(),
                po.getUploaderSubjectId(),
                po.getPortalType(),
                po.getFileName(),
                po.getMimeType(),
                po.getFileSize() == null ? 0L : po.getFileSize(),
                po.getStorageType(),
                po.getStorageKey(),
                po.getChecksum(),
                po.getCreatedAt(),
                po.getDeletedAt());
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
            AttachmentPolicyPo domainPolicy = attachmentPolicyRepository.findDomainPolicy(businessDomainId);
            if (domainPolicy != null) {
                return toPolicy(domainPolicy);
            }
        }
        AttachmentPolicyPo platformPolicy = attachmentPolicyRepository.findPlatformPolicy();
        if (platformPolicy != null) {
            return toPolicy(platformPolicy);
        }
        return new AttachmentPolicy(DEFAULT_ALLOWED_EXTENSIONS, 20, 200);
    }

    private AttachmentPolicy toPolicy(AttachmentPolicyPo po) {
        return new AttachmentPolicy(
                parseAllowedExtensions(po.getAllowedTypesJson()),
                po.getMaxSingleSizeMb() == null || po.getMaxSingleSizeMb() == 0 ? 20 : po.getMaxSingleSizeMb(),
                po.getMaxTotalSizeMb() == null || po.getMaxTotalSizeMb() == 0 ? 200 : po.getMaxTotalSizeMb());
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
