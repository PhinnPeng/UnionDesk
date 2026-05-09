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
                "local",
                "2026-05-03/test-guide.pdf",
                "C:\\tmp\\uniondesk\\attachments\\2026-05-03\\test-guide.pdf",
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
                .andExpect(jsonPath("$.storageType").value("local"));

        verify(attachmentService).uploadAttachment(any(), any());
    }

    @Test
    void presignReturnsUploadUrl() throws Exception {
        AttachmentService attachmentService = mock(AttachmentService.class);
        when(attachmentService.presignAttachment(any())).thenReturn(new AttachmentService.AttachmentPresignResult(
                9L,
                "/api/v1/attachments/upload",
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
                .andExpect(jsonPath("$.attachmentId").value(9))
                .andExpect(jsonPath("$.uploadUrl").value("/api/v1/attachments/upload"));

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
}
