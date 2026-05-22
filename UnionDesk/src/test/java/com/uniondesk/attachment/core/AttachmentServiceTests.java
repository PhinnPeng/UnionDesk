package com.uniondesk.attachment.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.uniondesk.attachment.storage.AttachmentObjectStorage;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AttachmentServiceTests {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-05-03T08:00:00Z"), ZoneOffset.UTC);

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private AttachmentObjectStorage objectStorage;

    private AttachmentService attachmentService;

    @BeforeEach
    void setUp() {
        attachmentService = new AttachmentService(jdbcTemplate, CLOCK, objectStorage);
        stubDefaultPolicyFallback();
        stubAttachmentPersistence();
    }

    @Test
    void uploadAttachmentStoresInObjectStorage() throws NoSuchAlgorithmException {
        byte[] content = "hello".getBytes(StandardCharsets.UTF_8);

        AttachmentService.AttachmentUploadResult result = attachmentService.uploadAttachment(
                null,
                new AttachmentService.UploadAttachmentCommand(
                        1L,
                        "customer",
                        "note.txt",
                        "text/plain",
                        content,
                        null,
                        null,
                        null));

        assertThat(result.storageType()).isEqualTo(AttachmentService.STORAGE_TYPE_OBJECT);
        assertThat(result.checksum()).isEqualTo(sha256(content));
        verify(objectStorage).putObject(anyString(), eq(content), eq("text/plain"));
    }

    @Test
    void presignAttachmentReturnsMinioUploadUrl() {
        when(objectStorage.presignPut(anyString(), anyString(), anyLong()))
                .thenReturn(new AttachmentObjectStorage.PresignedUrl("http://127.0.0.1:9000/bucket/key?sig=1", 300));

        AttachmentService.AttachmentPresignResult result = attachmentService.presignAttachment(
                new AttachmentService.PresignAttachmentCommand(
                        1L,
                        "customer",
                        "note.pdf",
                        "application/pdf",
                        1024L,
                        "ticket"));

        assertThat(result.uploadUrl()).contains("9000");
        assertThat(result.expiresInSeconds()).isEqualTo(300);
    }

    @Test
    void uploadAttachmentRejectsDisallowedExtension() {
        assertThatThrownBy(() -> attachmentService.uploadAttachment(
                null,
                new AttachmentService.UploadAttachmentCommand(
                        1L,
                        "customer",
                        "virus.exe",
                        "application/octet-stream",
                        new byte[] {1, 2, 3},
                        null,
                        null,
                        null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("白名单");
    }

    @Test
    void uploadAttachmentRejectsOversizedFile() {
        byte[] content = new byte[20 * 1024 * 1024 + 1];

        assertThatThrownBy(() -> attachmentService.uploadAttachment(
                null,
                new AttachmentService.UploadAttachmentCommand(
                        1L,
                        "customer",
                        "big.pdf",
                        "application/pdf",
                        content,
                        null,
                        null,
                        null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("大小超限");
    }

    private void stubDefaultPolicyFallback() {
        when(jdbcTemplate.queryForObject(
                argThat(sql -> sql != null && sql.contains("FROM attachment_policy")),
                any(org.springframework.jdbc.core.RowMapper.class),
                any(Object[].class)))
                .thenThrow(new EmptyResultDataAccessException(1));
    }

    private void stubAttachmentPersistence() {
        when(jdbcTemplate.update(argThat(sql -> sql != null && sql.contains("INSERT INTO file_attachment")), any(Object[].class)))
                .thenReturn(1);
        when(jdbcTemplate.queryForObject(argThat(sql -> sql != null && sql.contains("FROM file_attachment") && sql.contains("storage_key")), eq(Long.class), any()))
                .thenReturn(77L);
    }

    private static String sha256(byte[] content) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return java.util.HexFormat.of().formatHex(digest.digest(content));
    }
}
