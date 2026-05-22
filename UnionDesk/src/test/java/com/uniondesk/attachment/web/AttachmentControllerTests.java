package com.uniondesk.attachment.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.uniondesk.attachment.core.AttachmentService;
import com.uniondesk.auth.core.UserContext;
import com.uniondesk.auth.core.UserContextHolder;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AttachmentControllerTests {

    @AfterEach
    void clearContext() {
        UserContextHolder.clear();
    }

    @Test
    void uploadReturnsResult() throws Exception {
        AttachmentService attachmentService = mock(AttachmentService.class);
        when(attachmentService.uploadAttachment(any(), any())).thenReturn(new AttachmentService.AttachmentUploadResult(
                7L,
                AttachmentService.STORAGE_TYPE_OBJECT,
                "2026-05-03/test-guide.pdf",
                "checksum-1"));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AttachmentController(attachmentService)).build();
        UserContextHolder.set(new UserContext(2L, "super_admin", 10L, "sid-1", "ud-admin-web"));
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-guide.pdf",
                "application/pdf",
                "hello".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/v1/attachments/upload")
                        .file(file)
                        .param("businessDomainId", "10")
                        .param("portalType", "customer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attachmentId").value(7))
                .andExpect(jsonPath("$.downloadUrl").value("/api/v1/attachments/7/download"))
                .andExpect(jsonPath("$.storageType").value("object_storage"));

        verify(attachmentService).uploadAttachment(any(), any());
    }

    @Test
    void presignReturnsUploadUrl() throws Exception {
        AttachmentService attachmentService = mock(AttachmentService.class);
        when(attachmentService.presignAttachment(any())).thenReturn(new AttachmentService.AttachmentPresignResult(
                9L,
                "http://127.0.0.1:9000/uniondesk-attachments/key?X-Amz-Signature=abc",
                300));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AttachmentController(attachmentService)).build();

        mockMvc.perform(post("/api/v1/attachments/presign")
                        .contentType("application/json")
                        .content("""
                                {
                                  "fileName": "test-guide.pdf",
                                  "mimeType": "application/pdf",
                                  "fileSize": 1024,
                                  "targetType": "ticket",
                                  "domainId": 10
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attachment_id").value(9))
                .andExpect(jsonPath("$.upload_url").value("http://127.0.0.1:9000/uniondesk-attachments/key?X-Amz-Signature=abc"));

        verify(attachmentService).presignAttachment(any());
    }

    @Test
    void confirmReturnsNoContent() throws Exception {
        AttachmentService attachmentService = mock(AttachmentService.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AttachmentController(attachmentService)).build();

        mockMvc.perform(put("/api/v1/attachments/7/confirm"))
                .andExpect(status().isNoContent());

        verify(attachmentService).confirmAttachment(7L);
    }

    @Test
    void downloadReturnsPresignedAccess() throws Exception {
        AttachmentService attachmentService = mock(AttachmentService.class);
        when(attachmentService.resolveDownloadAccess(7L)).thenReturn(new AttachmentService.AttachmentDownloadAccess(
                "http://127.0.0.1:9000/uniondesk-attachments/key?get=1",
                300,
                AttachmentService.STORAGE_TYPE_OBJECT));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AttachmentController(attachmentService)).build();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/attachments/7/download"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.download_url").value("http://127.0.0.1:9000/uniondesk-attachments/key?get=1"))
                .andExpect(jsonPath("$.expires_in").value(300))
                .andExpect(jsonPath("$.storage_type").value("object_storage"));

        verify(attachmentService).resolveDownloadAccess(7L);
    }
}
