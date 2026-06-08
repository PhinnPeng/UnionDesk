package com.uniondesk.domain.web;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.time.LocalDateTime;

public final class InvitationCodeDtos {

    private InvitationCodeDtos() {
    }

    public record InvitationCodeView(
            long id,
            long businessDomainId,
            String code,
            String channel,
            LocalDateTime expiresAt,
            Integer maxUses,
            int usedCount,
            String status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
    }

    public record CreateInvitationCodeRequest(
            String channel,
            @JsonAlias({"expires_at"})
            LocalDateTime expiresAt,
            @JsonAlias({"max_uses"})
            Integer maxUses) {
    }
}
