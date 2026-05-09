package com.uniondesk.blockedword.web;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

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
}
