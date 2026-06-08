package com.uniondesk.blockedword.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.time.LocalDateTime;
import java.util.List;

public final class BlockedWordDtos {

    private BlockedWordDtos() {
    }

    public record BlockedWordView(
            long id,
            String word,
            LocalDateTime created_at) {
    }

    public record CreateBlockedWordRequest(
            @NotBlank String word) {
    }

    public record BatchCreateBlockedWordRequest(
            @NotEmpty List<@NotBlank String> words) {
    }

    public record BatchCreateSkippedItem(
            String word,
            String reason) {
    }

    public record BatchCreateBlockedWordResult(
            int created_count,
            List<BatchCreateSkippedItem> skipped) {
    }
}
