package com.uniondesk.attachment.web;

import com.uniondesk.auth.core.UserContextHolder;
import com.uniondesk.attachment.core.AttachmentService;
import com.uniondesk.iam.core.PermissionCodes;
import com.uniondesk.iam.core.RequirePermission;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;

@RestController
@RequestMapping("/api/v1/attachments")
public class AttachmentController {

    private final AttachmentService attachmentService;

    public AttachmentController(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @PostMapping("/upload")
    @RequirePermission(PermissionCodes.ATTACHMENT_UPLOAD)
    public AttachmentDtos.AttachmentUploadResponse upload(
            @RequestParam(name = "domain_id", required = false) Long domainId,
            @RequestParam(name = "businessDomainId", required = false) Long legacyDomainId,
            @RequestParam(name = "portal_type", required = false) String portalType,
            @RequestParam(name = "portalType", required = false) String legacyPortalType,
            @RequestParam(name = "target_type", required = false) String targetType,
            @RequestParam(name = "target_id", required = false) Long targetId,
            @RequestParam(name = "relation_type", required = false) String relationType,
            @RequestParam("file") MultipartFile file) throws Exception {
        Long businessDomainId = domainId != null ? domainId : legacyDomainId;
        if (businessDomainId == null) {
            throw new IllegalArgumentException("domain_id is required");
        }
        String resolvedPortalType = StringUtils.hasText(portalType)
                ? portalType
                : (StringUtils.hasText(legacyPortalType) ? legacyPortalType : "customer");
        AttachmentService.UploadAttachmentCommand command = new AttachmentService.UploadAttachmentCommand(
                businessDomainId,
                resolvedPortalType,
                file.getOriginalFilename(),
                file.getContentType(),
                file.getBytes(),
                targetType,
                targetId,
                relationType);
        return AttachmentDtos.AttachmentUploadResponse.from(
                attachmentService.uploadAttachment(UserContextHolder.requireCurrent(), command));
    }

    // P1: portalType 硬编码为 "customer"，管理端 presign 时需从 UserContext 推导
    @PostMapping("/presign")
    @RequirePermission(PermissionCodes.ATTACHMENT_UPLOAD)
    public AttachmentDtos.AttachmentPresignResponse presign(@Valid @RequestBody AttachmentDtos.PresignRequest request) {
        return AttachmentDtos.AttachmentPresignResponse.from(attachmentService.presignAttachment(new AttachmentService.PresignAttachmentCommand(
                request.domainId(),
                "customer",
                request.fileName(),
                request.mimeType(),
                request.fileSize(),
                request.targetType())));
    }

    @PutMapping("/{attachment_id}/confirm")
    @RequirePermission(PermissionCodes.ATTACHMENT_UPLOAD)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirm(@PathVariable("attachment_id") long attachmentId) {
        attachmentService.confirmAttachment(attachmentId);
    }

    @GetMapping("/{attachment_id}/download")
    @RequirePermission(PermissionCodes.ATTACHMENT_DOWNLOAD)
    public AttachmentDtos.AttachmentDownloadResponse download(@PathVariable("attachment_id") long attachmentId) {
        return AttachmentDtos.AttachmentDownloadResponse.from(attachmentService.resolveDownloadAccess(attachmentId));
    }
}
