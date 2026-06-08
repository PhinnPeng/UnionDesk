package com.uniondesk.sla.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniondesk.sla.entity.SlaTicketPo;
import com.uniondesk.sla.entity.TicketSlaPolicyPo;
import com.uniondesk.sla.repository.SlaRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SlaServiceTests {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-05-03T08:00:00Z"), ZoneOffset.UTC);

    @Mock
    private SlaRepository slaRepository;

    private SlaService slaService;

    @BeforeEach
    void setUp() {
        slaService = new SlaService(slaRepository, CLOCK, new ObjectMapper());
    }

    @Test
    void applyOnCreateCalculatesDeadlinesFromPolicyAndCreationTime() {
        TicketSlaPolicyPo policyPo = new TicketSlaPolicyPo();
        policyPo.setFirstResponseMinutes(30);
        policyPo.setResolutionMinutes(90);
        when(slaRepository.findTicketPriority(101L)).thenReturn("normal");
        when(slaRepository.findPolicy(1L, 77L, "normal")).thenReturn(policyPo);

        slaService.applyOnCreate(1L, 101L, 77L);

        verify(slaRepository).updateSlaDeadlines(101L, 30, 90);
    }

    @Test
    void evaluateTicketAppliesBreachActions() {
        SlaTicketPo snapshot = new SlaTicketPo();
        snapshot.setPriority("normal");
        snapshot.setSlaStatus("tracking");
        snapshot.setSlaFirstResponseDeadline(LocalDateTime.parse("2026-05-03T07:30:00"));
        snapshot.setSlaResolutionDeadline(LocalDateTime.parse("2026-05-03T09:30:00"));
        snapshot.setBreachActionJson("{\"raise_priority_to\":\"urgent\",\"sla_status\":\"escalated\"}");
        when(slaRepository.findSlaSnapshot(101L, 1L)).thenReturn(snapshot);

        SlaService.SlaBreachDecision decision = slaService.evaluateTicket(1L, 101L);

        assertThat(decision.breached()).isTrue();
        assertThat(decision.firstResponseBreached()).isTrue();
        assertThat(decision.nextPriority()).isEqualTo("urgent");
        assertThat(decision.nextStatus()).isEqualTo("escalated");
        verify(slaRepository).updatePriorityAndSlaStatus(eq("urgent"), eq("escalated"), eq(101L));
    }
}
