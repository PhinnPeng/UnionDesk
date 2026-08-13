package com.uniondesk.ticket.core;

import com.uniondesk.common.web.ErrorCodes;
import com.uniondesk.ticket.entity.TicketDetailPo;
import com.uniondesk.ticket.entity.TicketSatisfactionPo;
import com.uniondesk.ticket.repository.TicketRepository;
import com.uniondesk.ticket.repository.TicketSatisfactionRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TicketSatisfactionService {

    private static final List<String> EVALUABLE_STATUSES = List.of("closed", "resolved");

    private final TicketRepository ticketRepository;
    private final TicketSatisfactionRepository satisfactionRepository;
    private final Clock clock;

    public TicketSatisfactionService(
            TicketRepository ticketRepository,
            TicketSatisfactionRepository satisfactionRepository,
            Clock clock) {
        this.ticketRepository = ticketRepository;
        this.satisfactionRepository = satisfactionRepository;
        this.clock = clock;
    }

    @Transactional
    public SatisfactionSubmissionResult submit(
            long businessDomainId,
            long ticketId,
            long customerUserId,
            int rating,
            String comment) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("评分须在 1-5 星之间");
        }
        TicketDetailPo ticket = ticketRepository.findRequiredByIdAndDomainId(ticketId, businessDomainId);
        requireTicketOwner(ticket, customerUserId);
        if (!EVALUABLE_STATUSES.contains(ticket.getStatus())) {
            throw new IllegalArgumentException("工单未关闭不可评价");
        }
        if (satisfactionRepository.findByTicketIdAndDomainId(ticketId, businessDomainId) != null) {
            throw new IllegalArgumentException("该工单已评价，请勿重复提交");
        }
        TicketSatisfactionPo po = new TicketSatisfactionPo();
        po.setBusinessDomainId(businessDomainId);
        po.setTicketId(ticketId);
        po.setCustomerId(customerUserId);
        po.setRating(rating);
        po.setComment(StringUtils.hasText(comment) ? comment.trim() : null);
        try {
            satisfactionRepository.save(po);
        } catch (DuplicateKeyException ex) {
            throw new IllegalArgumentException("该工单已评价，请勿重复提交");
        }
        return new SatisfactionSubmissionResult(po.getId());
    }

    @Transactional(readOnly = true)
    public SatisfactionView getByTicket(long businessDomainId, long ticketId, long customerUserId) {
        TicketDetailPo ticket = ticketRepository.findRequiredByIdAndDomainId(ticketId, businessDomainId);
        requireTicketOwner(ticket, customerUserId);
        TicketSatisfactionPo po = satisfactionRepository.findByTicketIdAndDomainId(ticketId, businessDomainId);
        if (po == null) {
            return null;
        }
        return new SatisfactionView(
                po.getId(),
                po.getRating(),
                po.getComment(),
                po.getStatus(),
                po.getCreatedAt());
    }

    private void requireTicketOwner(TicketDetailPo ticket, long customerUserId) {
        if (ticket.getCustomerId() != customerUserId) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ErrorCodes.FORBIDDEN.message());
        }
    }

    public record SubmitSatisfactionCommand(int rating, String comment) {
    }

    public record SatisfactionSubmissionResult(long id) {
    }

    public record SatisfactionView(
            long id,
            int rating,
            String comment,
            String status,
            LocalDateTime createdAt) {
    }
}
