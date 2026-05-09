package com.uniondesk.sla.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Constructor;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SlaServiceTests {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-05-03T08:00:00Z"), ZoneOffset.UTC);

    @Mock
    private JdbcTemplate jdbcTemplate;

    private SlaService slaService;

    @BeforeEach
    void setUp() {
        slaService = new SlaService(jdbcTemplate, CLOCK, new ObjectMapper());
    }

    @Test
    void applyOnCreateCalculatesDeadlinesFromPolicyAndCreationTime() throws Exception {
        Object policy = newPrivateRecord(
                "com.uniondesk.sla.core.SlaService$TicketSlaPolicy",
                new Class<?>[] {Integer.class, Integer.class, String.class},
                30,
                90,
                null);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn((List) List.of(policy));
        when(jdbcTemplate.queryForObject(
                argThat(sql -> sql != null && sql.contains("SELECT created_at")),
                any(RowMapper.class),
                any(Object[].class)))
                .thenReturn(LocalDateTime.parse("2026-05-03T07:00:00"));
        when(jdbcTemplate.update(argThat(sql -> sql != null && sql.contains("UPDATE ticket")), any(Object[].class)))
                .thenReturn(1);

        slaService.applyOnCreate(1L, 101L, 77L);

        ArgumentCaptor<Object[]> updateArgs = ArgumentCaptor.forClass(Object[].class);
        org.mockito.Mockito.verify(jdbcTemplate)
                .update(argThat(sql -> sql != null && sql.contains("UPDATE ticket")), updateArgs.capture());
        assertThat(updateArgs.getValue()[0]).isEqualTo(30);
        assertThat(updateArgs.getValue()[1]).isEqualTo(30);
        assertThat(updateArgs.getValue()[2]).isEqualTo(90);
        assertThat(updateArgs.getValue()[3]).isEqualTo(90);
        assertThat(updateArgs.getValue()[4]).isEqualTo(101L);
    }

    @Test
    void evaluateTicketAppliesBreachActions() throws Exception {
        Object snapshot = newPrivateRecord(
                "com.uniondesk.sla.core.SlaService$TicketSlaSnapshot",
                new Class<?>[] {
                    String.class,
                    String.class,
                    LocalDateTime.class,
                    LocalDateTime.class,
                    LocalDateTime.class,
                    LocalDateTime.class,
                    String.class
                },
                "normal",
                "tracking",
                LocalDateTime.parse("2026-05-03T07:30:00"),
                LocalDateTime.parse("2026-05-03T09:30:00"),
                null,
                null,
                "{\"raise_priority_to\":\"urgent\",\"sla_status\":\"escalated\"}");
        when(jdbcTemplate.queryForObject(
                argThat(sql -> sql != null && sql.contains("FROM ticket t")),
                any(RowMapper.class),
                any(Object[].class)))
                .thenReturn(snapshot);
        when(jdbcTemplate.update(argThat(sql -> sql != null && sql.contains("UPDATE ticket")), any(Object[].class)))
                .thenReturn(1);

        SlaService.SlaBreachDecision decision = slaService.evaluateTicket(1L, 101L);

        assertThat(decision.breached()).isTrue();
        assertThat(decision.firstResponseBreached()).isTrue();
        assertThat(decision.nextPriority()).isEqualTo("urgent");
        assertThat(decision.nextStatus()).isEqualTo("escalated");

        ArgumentCaptor<Object[]> updateArgs = ArgumentCaptor.forClass(Object[].class);
        org.mockito.Mockito.verify(jdbcTemplate).update(argThat(sql -> sql != null && sql.contains("UPDATE ticket")), updateArgs.capture());
        assertThat(updateArgs.getValue()[0]).isEqualTo("urgent");
        assertThat(updateArgs.getValue()[1]).isEqualTo("escalated");
        assertThat(updateArgs.getValue()[2]).isEqualTo(101L);
    }

    private static Object newPrivateRecord(String className, Class<?>[] parameterTypes, Object... args) throws Exception {
        Class<?> type = Class.forName(className);
        Constructor<?> constructor = type.getDeclaredConstructor(parameterTypes);
        constructor.setAccessible(true);
        return constructor.newInstance(args);
    }
}
