package com.uniondesk.ticket.core;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.uniondesk.ticket.entity.SlaScanCandidatePo;
import com.uniondesk.ticket.repository.TicketRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SlaScanJobTests {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private TicketService ticketService;

    private SlaScanJob slaScanJob;

    @BeforeEach
    void setUp() {
        slaScanJob = new SlaScanJob(ticketRepository, ticketService);
    }

    @Test
    void scanProcessesAllCandidates() {
        when(ticketRepository.findSlaScanCandidates(100)).thenReturn(List.of(candidate(1L, 1L), candidate(2L, 2L)));

        slaScanJob.scanOverdueTickets();

        verify(ticketService).processSlaBreach(1L, 1L);
        verify(ticketService).processSlaBreach(2L, 2L);
    }

    @Test
    void singleFailureDoesNotInterruptBatch() {
        when(ticketRepository.findSlaScanCandidates(100)).thenReturn(List.of(candidate(1L, 1L), candidate(2L, 2L)));
        doThrow(new RuntimeException("处置失败")).when(ticketService).processSlaBreach(1L, 1L);

        slaScanJob.scanOverdueTickets();

        verify(ticketService).processSlaBreach(2L, 2L);
    }

    @Test
    void scanSkipsWhenNoCandidates() {
        when(ticketRepository.findSlaScanCandidates(100)).thenReturn(List.of());

        slaScanJob.scanOverdueTickets();

        verify(ticketService, never()).processSlaBreach(anyLong(), anyLong());
    }

    private SlaScanCandidatePo candidate(long id, long domainId) {
        SlaScanCandidatePo po = new SlaScanCandidatePo();
        po.setId(id);
        po.setBusinessDomainId(domainId);
        return po;
    }
}
