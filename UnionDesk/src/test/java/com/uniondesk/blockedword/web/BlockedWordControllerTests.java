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
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class BlockedWordControllerTests {

    private final BlockedWordService blockedWordService = mock(BlockedWordService.class);

    @Test
    void listReturnsBlockedWords() throws Exception {
        MockMvc mockMvc = mockMvc();
        when(blockedWordService.listBlockedWords(1L)).thenReturn(List.of(
                new BlockedWordDtos.BlockedWordView(11L, "spam", LocalDateTime.of(2026, 5, 3, 12, 0))));

        mockMvc.perform(get("/api/v1/admin/domains/1/blocked-words"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(11))
                .andExpect(jsonPath("$[0].word").value("spam"));
        verify(blockedWordService).listBlockedWords(1L);
    }

    @Test
    void createReturnsCreatedWord() throws Exception {
        MockMvc mockMvc = mockMvc();
        when(blockedWordService.createBlockedWord(eq(1L), eq("spam"))).thenReturn(
                new BlockedWordDtos.BlockedWordView(11L, "spam", LocalDateTime.of(2026, 5, 3, 12, 0)));

        mockMvc.perform(post("/api/v1/admin/domains/1/blocked-words")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"word\":\"spam\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(11))
                .andExpect(jsonPath("$.word").value("spam"));
        verify(blockedWordService).createBlockedWord(1L, "spam");
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        MockMvc mockMvc = mockMvc();

        mockMvc.perform(delete("/api/v1/admin/domains/1/blocked-words/11"))
                .andExpect(status().isNoContent());
        verify(blockedWordService).deleteBlockedWord(1L, 11L);
    }

    private MockMvc mockMvc() {
        return MockMvcBuilders.standaloneSetup(new BlockedWordController(blockedWordService)).build();
    }
}
