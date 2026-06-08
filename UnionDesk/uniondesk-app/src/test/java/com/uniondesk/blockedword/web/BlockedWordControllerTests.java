package com.uniondesk.blockedword.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.uniondesk.blockedword.core.BlockedWordService;
import com.uniondesk.blockedword.web.BlockedWordDtos.BatchCreateBlockedWordResult;
import com.uniondesk.blockedword.web.BlockedWordDtos.BatchCreateSkippedItem;
import com.uniondesk.common.web.PageResult;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class BlockedWordControllerTests {

    private final BlockedWordService blockedWordService = mock(BlockedWordService.class);

    @Test
    void listReturnsPageResult() throws Exception {
        MockMvc mockMvc = mockMvc();
        when(blockedWordService.listDomainPage(1L, 1, 20, null)).thenReturn(new PageResult<>(
                1,
                List.of(new BlockedWordDtos.BlockedWordView(11L, "spam", LocalDateTime.of(2026, 5, 3, 12, 0)))));

        mockMvc.perform(get("/api/v1/admin/domains/1/blocked-words"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.list[0].word").value("spam"));
        verify(blockedWordService).listDomainPage(1L, 1, 20, null);
    }

    @Test
    void createReturnsCreatedWord() throws Exception {
        MockMvc mockMvc = mockMvc();
        when(blockedWordService.createDomain(eq(1L), eq("spam"))).thenReturn(
                new BlockedWordDtos.BlockedWordView(11L, "spam", LocalDateTime.of(2026, 5, 3, 12, 0)));

        mockMvc.perform(post("/api/v1/admin/domains/1/blocked-words")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"word\":\"spam\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(11))
                .andExpect(jsonPath("$.word").value("spam"));
        verify(blockedWordService).createDomain(1L, "spam");
    }

    @Test
    void batchCreateReturnsResult() throws Exception {
        MockMvc mockMvc = mockMvc();
        when(blockedWordService.createDomainBatch(eq(1L), any())).thenReturn(
                new BatchCreateBlockedWordResult(1, List.of(new BatchCreateSkippedItem("spam", "已存在"))));

        mockMvc.perform(post("/api/v1/admin/domains/1/blocked-words/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"words\":[\"spam\",\"广告\"]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.created_count").value(1))
                .andExpect(jsonPath("$.skipped[0].word").value("spam"));
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        MockMvc mockMvc = mockMvc();

        mockMvc.perform(delete("/api/v1/admin/domains/1/blocked-words/11"))
                .andExpect(status().isNoContent());
        verify(blockedWordService).deleteDomain(1L, 11L);
    }

    private MockMvc mockMvc() {
        return MockMvcBuilders.standaloneSetup(new BlockedWordController(blockedWordService)).build();
    }
}
