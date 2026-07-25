package com.uniondesk.ticket.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.uniondesk.ticket.entity.TicketStatusPo;
import com.uniondesk.ticket.repository.TicketStatusRepository;
import com.uniondesk.ticket.web.TicketStatusDtos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class TicketStatusServiceTests {

    @Mock
    private TicketStatusRepository ticketStatusRepository;

    private TicketStatusService ticketStatusService;

    @BeforeEach
    void setUp() {
        ticketStatusService = new TicketStatusService(ticketStatusRepository);
    }

    @Test
    void createPlatformPersistsCustomStatus() {
        when(ticketStatusRepository.findPlatformByName("待评估")).thenReturn(null);
        when(ticketStatusRepository.nextSortOrderPlatform()).thenReturn(4);

        TicketStatusDtos.TicketStatusView view = ticketStatusService.createPlatform(
                new TicketStatusDtos.CreateTicketStatusRequest("待评估", "用于评估优先级", "not_started", null),
                1L);

        ArgumentCaptor<TicketStatusPo> captor = ArgumentCaptor.forClass(TicketStatusPo.class);
        verify(ticketStatusRepository).insert(captor.capture());
        TicketStatusPo saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("待评估");
        assertThat(saved.getCategory()).isEqualTo(TicketStatusPo.CATEGORY_NOT_STARTED);
        assertThat(saved.getStateType()).isEqualTo(TicketStatusPo.STATE_TYPE_PAUSED);
        assertThat(saved.isSystem()).isFalse();
        assertThat(view.name()).isEqualTo("待评估");
    }

    @Test
    void updatePlatformSystemStatusOnlyAllowsDescription() {
        TicketStatusPo existing = systemStatus("not_started", "未开始");
        when(ticketStatusRepository.findRequiredById(1L)).thenReturn(existing);

        TicketStatusDtos.TicketStatusView view = ticketStatusService.updatePlatform(
                1L,
                new TicketStatusDtos.UpdateTicketStatusRequest(null, "系统描述", null),
                2L);

        assertThat(view.description()).isEqualTo("系统描述");
        verify(ticketStatusRepository).update(existing);
    }

    @Test
    void updatePlatformSystemStatusRejectsNameChange() {
        TicketStatusPo existing = systemStatus("not_started", "未开始");
        when(ticketStatusRepository.findRequiredById(1L)).thenReturn(existing);

        assertThatThrownBy(() -> ticketStatusService.updatePlatform(
                1L,
                new TicketStatusDtos.UpdateTicketStatusRequest("新名称", "描述", null),
                2L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("系统状态名称不可修改");
        verify(ticketStatusRepository, never()).update(any());
    }

    @Test
    void deletePlatformRejectsSystemStatus() {
        TicketStatusPo existing = systemStatus("completed", "已完成");
        when(ticketStatusRepository.findRequiredById(2L)).thenReturn(existing);

        assertThatThrownBy(() -> ticketStatusService.deletePlatform(2L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("系统状态不可删除");
        verify(ticketStatusRepository, never()).deletePlatform(2L);
    }

    private TicketStatusPo systemStatus(String code, String name) {
        TicketStatusPo po = new TicketStatusPo();
        po.setId(1L);
        po.setScope(TicketStatusPo.SCOPE_PLATFORM);
        po.setCode(code);
        po.setName(name);
        po.setDescription("");
        po.setCategory(TicketStatusPo.CATEGORY_NOT_STARTED);
        po.setStateType(TicketStatusPo.STATE_TYPE_PAUSED);
        po.setConfigJson("{}");
        po.setStatus(TicketStatusPo.STATUS_ACTIVE);
        po.setSystem(true);
        return po;
    }
}
