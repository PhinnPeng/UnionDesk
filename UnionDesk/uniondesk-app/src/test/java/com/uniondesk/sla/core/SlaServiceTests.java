package com.uniondesk.sla.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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
import java.util.List;
import java.util.Map;
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
        when(slaRepository.findCreatedAtById(101L, 1L)).thenReturn(LocalDateTime.parse("2026-05-03T08:00:00"));

        slaService.applyOnCreate(1L, 101L, 77L);

        verify(slaRepository).updateSlaDeadlines(
                101L,
                LocalDateTime.parse("2026-05-03T08:30:00"),
                LocalDateTime.parse("2026-05-03T09:30:00"));
        verify(slaRepository, never()).findGlobalPolicy();
    }

    @Test
    void applyOnCreateFallsBackToGlobalPolicyWhenNoDomainRule() {
        TicketSlaPolicyPo globalPolicy = new TicketSlaPolicyPo();
        globalPolicy.setFirstResponseMinutes(10);
        globalPolicy.setResolutionMinutes(20);
        when(slaRepository.findTicketPriority(101L)).thenReturn("normal");
        when(slaRepository.findPolicy(1L, 77L, "normal")).thenReturn(null);
        when(slaRepository.findGlobalPolicy()).thenReturn(globalPolicy);
        when(slaRepository.findCreatedAtById(101L, 1L)).thenReturn(LocalDateTime.parse("2026-05-03T08:00:00"));

        slaService.applyOnCreate(1L, 101L, 77L);

        verify(slaRepository).updateSlaDeadlines(
                101L,
                LocalDateTime.parse("2026-05-03T08:10:00"),
                LocalDateTime.parse("2026-05-03T08:20:00"));
    }

    @Test
    void applyOnCreateWithoutAnyPolicyLeavesDeadlinesUnset() {
        when(slaRepository.findTicketPriority(101L)).thenReturn("normal");
        when(slaRepository.findPolicy(1L, 77L, "normal")).thenReturn(null);
        when(slaRepository.findGlobalPolicy()).thenReturn(null);
        when(slaRepository.findCreatedAtById(101L, 1L)).thenReturn(LocalDateTime.parse("2026-05-03T08:00:00"));

        slaService.applyOnCreate(1L, 101L, 77L);

        verify(slaRepository).updateSlaDeadlines(101L, null, null);
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
        when(slaRepository.claimBreachAction(101L)).thenReturn(1);

        SlaService.SlaBreachDecision decision = slaService.evaluateTicket(1L, 101L);

        assertThat(decision.breached()).isTrue();
        assertThat(decision.firstResponseBreached()).isTrue();
        assertThat(decision.nextPriority()).isEqualTo("urgent");
        assertThat(decision.nextStatus()).isEqualTo("escalated");
        assertThat(decision.pendingActions()).isEmpty();
        verify(slaRepository).updatePriorityAndSlaStatus(eq("urgent"), eq("escalated"), eq(101L));
    }

    @Test
    void evaluateTicketEscalatesPriorityToNextUrgentLevelBySortOrder() {
        SlaTicketPo snapshot = new SlaTicketPo();
        snapshot.setPriority("normal");
        snapshot.setSlaStatus("tracking");
        snapshot.setSlaFirstResponseDeadline(LocalDateTime.parse("2026-05-03T07:30:00"));
        snapshot.setBreachActionJson("{\"escalate_priority\":true}");
        when(slaRepository.findSlaSnapshot(101L, 1L)).thenReturn(snapshot);
        when(slaRepository.claimBreachAction(101L)).thenReturn(1);
        when(slaRepository.findActivePriorityCodes(1L)).thenReturn(List.of("urgent", "high", "normal", "low"));

        SlaService.SlaBreachDecision decision = slaService.evaluateTicket(1L, 101L);

        assertThat(decision.nextPriority()).isEqualTo("high");
        assertThat(decision.nextStatus()).isEqualTo("breached");
        verify(slaRepository).updatePriorityAndSlaStatus(eq("high"), eq("breached"), eq(101L));
    }

    @Test
    void evaluateTicketKeepsPriorityWhenAlreadyMostUrgent() {
        SlaTicketPo snapshot = new SlaTicketPo();
        snapshot.setPriority("urgent");
        snapshot.setSlaStatus("tracking");
        snapshot.setSlaFirstResponseDeadline(LocalDateTime.parse("2026-05-03T07:30:00"));
        snapshot.setBreachActionJson("{\"escalate_priority\":true}");
        when(slaRepository.findSlaSnapshot(101L, 1L)).thenReturn(snapshot);
        when(slaRepository.claimBreachAction(101L)).thenReturn(1);
        when(slaRepository.findActivePriorityCodes(1L)).thenReturn(List.of("urgent", "high", "normal", "low"));

        SlaService.SlaBreachDecision decision = slaService.evaluateTicket(1L, 101L);

        assertThat(decision.nextPriority()).isEqualTo("urgent");
    }

    @Test
    void evaluateTicketRaisePriorityToTakesPrecedenceOverEscalation() {
        SlaTicketPo snapshot = new SlaTicketPo();
        snapshot.setPriority("normal");
        snapshot.setSlaStatus("tracking");
        snapshot.setSlaFirstResponseDeadline(LocalDateTime.parse("2026-05-03T07:30:00"));
        snapshot.setBreachActionJson("{\"escalate_priority\":true,\"raise_priority_to\":\"critical\"}");
        when(slaRepository.findSlaSnapshot(101L, 1L)).thenReturn(snapshot);
        when(slaRepository.claimBreachAction(101L)).thenReturn(1);

        SlaService.SlaBreachDecision decision = slaService.evaluateTicket(1L, 101L);

        assertThat(decision.nextPriority()).isEqualTo("critical");
        verify(slaRepository, never()).findActivePriorityCodes(1L);
    }

    @Test
    void evaluateTicketBuildsPendingActionsForAssignAndWatchers() {
        SlaTicketPo snapshot = new SlaTicketPo();
        snapshot.setPriority("normal");
        snapshot.setSlaStatus("tracking");
        snapshot.setSlaFirstResponseDeadline(LocalDateTime.parse("2026-05-03T07:30:00"));
        snapshot.setBreachActionJson(
                "{\"assign_to_staff_account_id\":42,\"add_watcher_staff_account_ids\":[17,9]}");
        when(slaRepository.findSlaSnapshot(101L, 1L)).thenReturn(snapshot);
        when(slaRepository.claimBreachAction(101L)).thenReturn(1);

        SlaService.SlaBreachDecision decision = slaService.evaluateTicket(1L, 101L);

        assertThat(decision.pendingActions()).hasSize(2);
        assertThat(decision.pendingActions().get(0)).isEqualTo(new SlaService.AssignAction(42L));
        assertThat(decision.pendingActions().get(1))
                .isEqualTo(new SlaService.AddWatchersAction(List.of(17L, 9L)));
    }

    @Test
    void evaluateTicketNonClaimerOnlyFlipsStatusWithoutActions() {
        SlaTicketPo snapshot = new SlaTicketPo();
        snapshot.setPriority("normal");
        snapshot.setSlaStatus("tracking");
        snapshot.setSlaFirstResponseDeadline(LocalDateTime.parse("2026-05-03T07:30:00"));
        snapshot.setBreachActionJson("{\"raise_priority_to\":\"urgent\",\"assign_to_staff_account_id\":42}");
        when(slaRepository.findSlaSnapshot(101L, 1L)).thenReturn(snapshot);
        when(slaRepository.claimBreachAction(101L)).thenReturn(0);

        SlaService.SlaBreachDecision decision = slaService.evaluateTicket(1L, 101L);

        assertThat(decision.breached()).isTrue();
        assertThat(decision.pendingActions()).isEmpty();
        verify(slaRepository).updateSlaStatus(eq(101L), eq(1L), eq("breached"));
        verify(slaRepository, never()).updatePriorityAndSlaStatus(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), eq(101L));
    }

    @Test
    void evaluateTicketSelfHealsToTrackingWhenBreachConditionsCleared() {
        SlaTicketPo snapshot = new SlaTicketPo();
        snapshot.setPriority("normal");
        snapshot.setSlaStatus("breached");
        snapshot.setSlaFirstResponseDeadline(LocalDateTime.parse("2026-05-03T09:30:00"));
        snapshot.setSlaFirstRespondedAt(LocalDateTime.parse("2026-05-03T08:30:00"));
        when(slaRepository.findSlaSnapshot(101L, 1L)).thenReturn(snapshot);

        SlaService.SlaBreachDecision decision = slaService.evaluateTicket(1L, 101L);

        assertThat(decision.breached()).isFalse();
        assertThat(decision.nextStatus()).isEqualTo("tracking");
        verify(slaRepository).updateSlaStatus(eq(101L), eq(1L), eq("tracking"));
    }

    @Test
    void evaluateTicketKeepsBreachedWhenResolutionDeadlineStillExpiredAfterFirstResponse() {
        SlaTicketPo snapshot = new SlaTicketPo();
        snapshot.setPriority("normal");
        snapshot.setSlaStatus("tracking");
        snapshot.setSlaFirstResponseDeadline(LocalDateTime.parse("2026-05-03T07:30:00"));
        snapshot.setSlaFirstRespondedAt(LocalDateTime.parse("2026-05-03T08:30:00"));
        snapshot.setSlaResolutionDeadline(LocalDateTime.parse("2026-05-03T07:45:00"));
        snapshot.setBreachActionJson("{}");
        when(slaRepository.findSlaSnapshot(101L, 1L)).thenReturn(snapshot);
        when(slaRepository.claimBreachAction(101L)).thenReturn(1);

        SlaService.SlaBreachDecision decision = slaService.evaluateTicket(1L, 101L);

        assertThat(decision.breached()).isTrue();
        assertThat(decision.firstResponseBreached()).isFalse();
        assertThat(decision.nextStatus()).isEqualTo("breached");
        verify(slaRepository).updatePriorityAndSlaStatus(eq("normal"), eq("breached"), eq(101L));
    }

    @Test
    void evaluateTicketSkipsStoppedTerminalTicket() {
        SlaTicketPo snapshot = new SlaTicketPo();
        snapshot.setPriority("normal");
        snapshot.setSlaStatus("stopped");
        snapshot.setSlaFirstResponseDeadline(LocalDateTime.parse("2026-05-03T07:30:00"));
        when(slaRepository.findSlaSnapshot(101L, 1L)).thenReturn(snapshot);

        SlaService.SlaBreachDecision decision = slaService.evaluateTicket(1L, 101L);

        assertThat(decision.breached()).isFalse();
        assertThat(decision.nextStatus()).isEqualTo("stopped");
        verify(slaRepository, never()).claimBreachAction(101L);
        verify(slaRepository, never()).updatePriorityAndSlaStatus(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), eq(101L));
        verify(slaRepository, never()).updateSlaStatus(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void createGlobalSlaRuleRejectsTicketType() {
        SlaService.SlaRuleCommand command = new SlaService.SlaRuleCommand(
                "全局规则", 7L, null, null, 10, 20, false, Map.of());

        assertThatThrownBy(() -> slaService.createGlobalSlaRule(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("全局 SLA 规则不允许关联事项类型");
    }

    @Test
    void createGlobalSlaRuleRejectsPriorityLevel() {
        SlaService.SlaRuleCommand command = new SlaService.SlaRuleCommand(
                "全局规则", null, 9L, null, 10, 20, false, Map.of());

        assertThatThrownBy(() -> slaService.createGlobalSlaRule(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("全局 SLA 规则不允许关联优先级");
    }

    @Test
    void createGlobalSlaRuleRejectsCalendar() {
        SlaService.SlaRuleCommand command = new SlaService.SlaRuleCommand(
                "全局规则", null, null, 5L, 10, 20, false, Map.of());

        assertThatThrownBy(() -> slaService.createGlobalSlaRule(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("全局 SLA 规则不允许关联工作日历");
    }
}
