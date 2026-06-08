package com.uniondesk.attachment.web;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.uniondesk.attachment.core.AttachmentService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class AttachmentDtos {

    private AttachmentDtos() {
    }

    public record PresignRequest(
            @JsonProperty("file_name")
            @JsonAlias({"fileName"})
            @NotBlank String fileName,
            @JsonProperty("mime_type")
            @JsonAlias({"mimeType"})
            @NotBlank String mimeType,
            @JsonProperty("file_size")
            @JsonAlias({"fileSize"})
            long fileSize,
            @JsonProperty("target_type")
            @JsonAlias({"targetType"})
            @NotBlank String targetType,
            @JsonProperty("domain_id")
            @JsonAlias({"domainId"})
            @NotNull Long domainId) {
    }

    public record AttachmentUploadResponse(
            long attachmentId,
            String downloadUrl,
            String storageType) {

        public static AttachmentUploadResponse from(AttachmentService.AttachmentUploadResult result) {
            return new AttachmentUploadResponse(
                    result.attachmentId(),
                    "/api/v1/attachments/" + result.attachmentId() + "/download",
                    result.storageType());
        }

        @JsonProperty("attachment_id")
        public long attachmentIdSnake() {
            return attachmentId;
        }

        @JsonProperty("download_url")
        public String downloadUrlSnake() {
            return downloadUrl;
        }

        @JsonProperty("storage_type")
        public String storageTypeSnake() {
            return storageType;
        }
    }

    public record AttachmentPresignResponse(
            @JsonProperty("attachment_id")
            @JsonAlias({"attachmentId"})
            long attachmentId,
            @JsonProperty("upload_url")
            @JsonAlias({"uploadUrl"})
            String uploadUrl,
            @JsonProperty("expires_in")
            @JsonAlias({"expires_in_seconds", "expiresInSeconds"})
            int expiresInSeconds) {

        public static AttachmentPresignResponse from(AttachmentService.AttachmentPresignResult result) {
            return new AttachmentPresignResponse(
                    result.attachmentId(),
                    result.uploadUrl(),
                    result.expiresInSeconds());
        }
    }

    public record AttachmentDownloadResponse(
            @JsonProperty("download_url")
            @JsonAlias({"downloadUrl"})
            String downloadUrl,
            @JsonProperty("expires_in")
            @JsonAlias({"expires_in_seconds", "expiresInSeconds"})
            int expiresInSeconds,
            @JsonProperty("storage_type")
            @JsonAlias({"storageType"})
            String storageType) {

        public static AttachmentDownloadResponse from(AttachmentService.AttachmentDownloadAccess access) {
            return new AttachmentDownloadResponse(
                    access.downloadUrl(),
                    access.expiresInSeconds(),
                    access.storageType());
        }
    }
}
