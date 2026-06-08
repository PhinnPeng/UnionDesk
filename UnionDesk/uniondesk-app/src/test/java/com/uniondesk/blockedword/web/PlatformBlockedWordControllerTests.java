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
import com.uniondesk.common.web.PageResult;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class PlatformBlockedWordControllerTests {

    private final BlockedWordService blockedWordService = mock(BlockedWordService.class);

    @Test
    void listReturnsPageResult() throws Exception {
        MockMvc mockMvc = mockMvc();
        when(blockedWordService.listGlobalPage(1, 20, "广")).thenReturn(new PageResult<>(
                1,
                List.of(new BlockedWordDtos.BlockedWordView(11L, "广告", LocalDateTime.of(2026, 6, 8, 12, 0)))));

        mockMvc.perform(get("/api/v1/admin/blocked-words?page=1&page_size=20&keyword=广"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.list[0].word").value("广告"));
        verify(blockedWordService).listGlobalPage(1, 20, "广");
    }

    @Test
    void createReturnsCreatedWord() throws Exception {
        MockMvc mockMvc = mockMvc();
        when(blockedWordService.createGlobal(eq("spam"))).thenReturn(
                new BlockedWordDtos.BlockedWordView(11L, "spam", LocalDateTime.of(2026, 6, 8, 12, 0)));

        mockMvc.perform(post("/api/v1/admin/blocked-words")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"word\":\"spam\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.word").value("spam"));
    }

    @Test
    void batchCreateReturnsResult() throws Exception {
        MockMvc mockMvc = mockMvc();
        when(blockedWordService.createGlobalBatch(any())).thenReturn(
                new BlockedWordDtos.BatchCreateBlockedWordResult(2, List.of()));

        mockMvc.perform(post("/api/v1/admin/blocked-words/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"words\":[\"a\",\"b\"]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.created_count").value(2));
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        MockMvc mockMvc = mockMvc();

        mockMvc.perform(delete("/api/v1/admin/blocked-words/11"))
                .andExpect(status().isNoContent());
        verify(blockedWordService).deleteGlobal(11L);
    }

    private MockMvc mockMvc() {
        return MockMvcBuilders.standaloneSetup(new PlatformBlockedWordController(blockedWordService)).build();
    }
}
