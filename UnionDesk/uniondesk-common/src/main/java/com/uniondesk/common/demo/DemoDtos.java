package com.uniondesk.common.demo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class DemoDtos {

    private DemoDtos() {
    }

    public record LoginUserView(long id, String username, String mobile, String email, List<String> roles) {
    }

    public record LoginResponse(
            String accessToken,
            String refreshToken,
            String role,
            String tokenType,
            long expiresInSeconds,
            LoginUserView user,
            List<BusinessDomainView> accessibleDomains,
            long defaultBusinessDomainId) {
    }

    public record BusinessDomainView(
            long id,
            String code,
            String name,
            String visibilityPolicy,
            int status,
            long ticketCount,
            long openTicketCount,
            long consultationCount,
            long openConsultationCount) {
    }

    public record TicketStatsView(
            long totalTickets,
            long openTickets,
            long processingTickets,
            long resolvedTickets,
            long totalReplies) {
    }

    public record ConsultationStatsView(
            long totalSessions,
            long openSessions,
            long closedSessions,
            long totalMessages) {
    }

    public record DashboardResponse(
            BusinessDomainView businessDomain,
            TicketStatsView ticketStats,
            ConsultationStatsView consultationStats,
            List<TicketSummaryView> recentTickets,
            List<ConsultationSummaryView> recentConsultations) {
    }

    public record TicketSummaryView(
            long id,
            String ticketNo,
            long businessDomainId,
            String businessDomainName,
            long ticketTypeId,
            String ticketTypeName,
            String title,
            String status,
            String priority,
            String source,
            long customerId,
            Long assignedTo,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime lastReplyAt,
            long replyCount) {
    }

    public record TicketReplyView(
            long id,
            String senderRole,
            String replyType,
            String content,
            LocalDateTime createdAt) {
    }

    public record TicketDetailView(
            long id,
            String ticketNo,
            long businessDomainId,
            String businessDomainName,
            long ticketTypeId,
            String ticketTypeName,
            long customerId,
            Long assignedTo,
            String title,
            String description,
            String status,
            String priority,
            String source,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime lastReplyAt,
            long replyCount,
            List<TicketReplyView> replies) {
    }

    public record ConsultationSummaryView(
            long id,
            String sessionNo,
            long businessDomainId,
            String businessDomainName,
            long customerId,
            String sessionStatus,
            Long assignedTo,
            String linkedTicketNo,
            LocalDateTime lastMessageAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            long messageCount) {
    }

    public record ConsultationMessageView(
            long id,
            String sessionNo,
            int seqNo,
            long businessDomainId,
            String senderRole,
            String messageType,
            String content,
            String payloadJson,
            LocalDateTime createdAt) {
    }

    public record CreateTicketRequest(
            Long businessDomainId,
            Long ticketTypeId,
            Long customerId,
            @NotBlank String title,
            String description,
            String priority,
            String source) {
    }

    public record UpdateTicketStatusRequest(@NotBlank String status) {
    }

    public record ConsultationMessageRequest(
            @NotBlank String sessionNo,
            Long businessDomainId,
            Long senderUserId,
            String senderRole,
            String messageType,
            @NotBlank String content,
            Map<String, Object> payload) {
    }

    public record ConsultationMessagePayloadRequest(
            Long businessDomainId,
            Long senderUserId,
            String senderRole,
            String messageType,
            @NotBlank String content,
            Map<String, Object> payload) {
    }

    public record ConvertConsultationToTicketRequest(
            Long businessDomainId,
            Long ticketTypeId,
            Long customerId,
            String title,
            String description,
            String priority,
            String source) {
    }

    public record ConsultationFlowResponse(
            ConsultationSummaryView session,
            TicketDetailView ticket) {
    }

    public record DomainTicketCounts(long totalTickets, long openTickets) {
    }

    public record DomainConsultationCounts(long totalSessions, long openSessions) {
    }

    public record DomainAccessView(long domainId, String accessStatus) {
    }

    public record LoginSeedView(
            long userId,
            String username,
            String mobile,
            String email,
            List<String> roles,
            long defaultBusinessDomainId,
            List<BusinessDomainView> accessibleDomains) {
    }
}
