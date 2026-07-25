package com.uniondesk.ticket.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniondesk.ticket.entity.TicketTypePo;
import com.uniondesk.ticket.repository.QuickReplyTemplateRepository;
import com.uniondesk.ticket.repository.TicketPriorityLevelRepository;
import com.uniondesk.ticket.repository.TicketTemplateRepository;
import com.uniondesk.ticket.repository.TicketTypeAttributeSlotRepository;
import com.uniondesk.ticket.repository.TicketTypeRepository;
import com.uniondesk.ticket.core.TicketTransitionRuleService;
import com.uniondesk.ticket.web.TicketConfigDtos;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TicketConfigServiceTests {

    private static final String DEFAULT_SCHEMA_JSON = DefaultFormSchemaProvider.defaultSchemaJson();
    private static final String DRAFT_WITH_CUSTOM_JSON = """
            {
              "type": "object",
              "properties": {
                "title": {
                  "type": "string",
                  "title": "标题",
                  "x-component": "Input",
                  "required": true
                },
                "description": {
                  "type": "string",
                  "title": "详细描述",
                  "x-component": "Input.TextArea",
                  "required": true
                },
                "draftOnly": {
                  "type": "string",
                  "x-component": "Input"
                }
              }
            }
            """;

    private static final long DOMAIN_ID = 1L;
    private static final long TYPE_ID = 11L;

    @Mock
    private TicketTypeRepository ticketTypeRepository;
    @Mock
    private TicketTemplateRepository ticketTemplateRepository;
    @Mock
    private QuickReplyTemplateRepository quickReplyTemplateRepository;
    @Mock
    private TicketPriorityLevelRepository ticketPriorityLevelRepository;
    @Mock
    private TicketFormSchemaService ticketFormSchemaService;
    @Mock
    private TicketTypeAttributeSlotService ticketTypeAttributeSlotService;
    @Mock
    private TicketTypeAttributeSlotRepository ticketTypeAttributeSlotRepository;
    @Mock
    private TicketTransitionRuleService transitionRuleService;
    @Mock
    private TicketTypeFlowService ticketTypeFlowService;

    private TicketConfigService ticketConfigService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        ticketConfigService = new TicketConfigService(
                ticketTypeRepository,
                ticketTemplateRepository,
                quickReplyTemplateRepository,
                ticketPriorityLevelRepository,
                ticketFormSchemaService,
                ticketTypeAttributeSlotService,
                ticketTypeAttributeSlotRepository,
                transitionRuleService,
                ticketTypeFlowService,
                objectMapper);
    }

    @Test
    void saveFormSchemaDraftDelegatesToFormSchemaService() throws Exception {
        TicketTypePo existing = ticketTypePo();
        when(ticketTypeRepository.findRequiredByIdAndDomainId(TYPE_ID, DOMAIN_ID))
                .thenReturn(existing);
        when(ticketTypeFlowService.loadAssembled(DOMAIN_ID, TYPE_ID)).thenReturn(
                new TicketConfigDtos.WorkflowConfigView(
                        Map.of("states", List.of(), "transitions", List.of()),
                        List.of()));
        Map<String, Object> draftSchema = objectMapper.readValue(DRAFT_WITH_CUSTOM_JSON, Map.class);
        TicketFormSchemaService.FormSchemaAggregate aggregate = new TicketFormSchemaService.FormSchemaAggregate(
                objectMapper.readValue(DEFAULT_SCHEMA_JSON, Map.class),
                draftSchema,
                1,
                true);
        when(ticketFormSchemaService.saveDraft(DOMAIN_ID, TYPE_ID, draftSchema)).thenReturn(aggregate);

        TicketConfigDtos.TicketTypeView result = ticketConfigService.saveFormSchemaDraft(DOMAIN_ID, TYPE_ID, draftSchema);

        verify(ticketFormSchemaService).saveDraft(DOMAIN_ID, TYPE_ID, draftSchema);
        assertThat(result.form_schema_draft()).isInstanceOf(Map.class);
        assertThat(result.form_schema_has_unpublished()).isTrue();
    }

    @Test
    void publishFormSchemaDelegatesWithSchemaBody() throws Exception {
        TicketTypePo existing = ticketTypePo();
        when(ticketTypeRepository.findRequiredByIdAndDomainId(TYPE_ID, DOMAIN_ID))
                .thenReturn(existing);
        when(ticketTypeFlowService.loadAssembled(DOMAIN_ID, TYPE_ID)).thenReturn(
                new TicketConfigDtos.WorkflowConfigView(
                        Map.of("states", List.of(), "transitions", List.of()),
                        List.of()));
        Map<String, Object> draftSchema = objectMapper.readValue(DRAFT_WITH_CUSTOM_JSON, Map.class);
        TicketFormSchemaService.FormSchemaAggregate aggregate = new TicketFormSchemaService.FormSchemaAggregate(
                draftSchema,
                draftSchema,
                2,
                false);
        when(ticketFormSchemaService.publish(DOMAIN_ID, TYPE_ID, draftSchema, null)).thenReturn(aggregate);

        ticketConfigService.publishFormSchema(DOMAIN_ID, TYPE_ID, draftSchema);

        verify(ticketFormSchemaService).publish(DOMAIN_ID, TYPE_ID, draftSchema, null);
    }

    @Test
    void createTicketTypeRejectsDuplicateCodeAndName() {
        when(ticketTypeRepository.findByDomainIdAndCode(DOMAIN_ID, "technical"))
                .thenReturn(ticketTypePo());

        assertThatThrownBy(() -> ticketConfigService.createTicketType(
                DOMAIN_ID,
                new TicketConfigDtos.CreateTicketTypeRequest("technical", "技术支持", null, null, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("该域下编码已存在");

        when(ticketTypeRepository.findByDomainIdAndCode(DOMAIN_ID, "technical")).thenReturn(null);
        when(ticketTypeRepository.findByDomainIdAndName(DOMAIN_ID, "技术支持"))
                .thenReturn(ticketTypePo());

        assertThatThrownBy(() -> ticketConfigService.createTicketType(
                DOMAIN_ID,
                new TicketConfigDtos.CreateTicketTypeRequest("technical", "技术支持", null, null, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("该域下名称已存在");
    }

    @Test
    void updateTicketTypeDoesNotTouchFormSchemaService() {
        TicketTypePo existing = ticketTypePo();
        existing.setName("旧名称");
        when(ticketTypeRepository.findRequiredByIdAndDomainId(TYPE_ID, DOMAIN_ID)).thenReturn(existing);
        when(ticketTypeRepository.findByDomainIdAndName(DOMAIN_ID, "新名称")).thenReturn(null);
        when(ticketTypeAttributeSlotService.computePluginRevision(TYPE_ID)).thenReturn(null);
        when(ticketFormSchemaService.loadAggregate(eq(DOMAIN_ID), eq(TYPE_ID), any())).thenReturn(
                new TicketFormSchemaService.FormSchemaAggregate(
                        Map.of("type", "object"),
                        Map.of("type", "object"),
                        1,
                        false));
        when(ticketTypeFlowService.loadAssembled(DOMAIN_ID, TYPE_ID)).thenReturn(
                new TicketConfigDtos.WorkflowConfigView(
                        Map.of("states", List.of(), "transitions", List.of()),
                        List.of()));

        Map<String, Object> statusFlow = Map.of(
                "states", List.of(Map.of(
                        "code", "closed",
                        "name", "已关闭",
                        "state_type", "terminal",
                        "allow_customer_withdraw", false)),
                "transitions", List.of(),
                "initial_state_code", "closed");
        TicketConfigDtos.UpdateTicketTypeRequest request = new TicketConfigDtos.UpdateTicketTypeRequest(
                "新名称",
                "描述",
                null,
                "mdi:help",
                statusFlow,
                "disabled",
                List.of());

        TicketConfigDtos.TicketTypeView updated = ticketConfigService.updateTicketType(DOMAIN_ID, TYPE_ID, request);

        verify(ticketTypeRepository).updateMetadata(
                eq(TYPE_ID),
                eq(DOMAIN_ID),
                eq("新名称"),
                eq("描述"),
                eq(null),
                eq("mdi:help"),
                eq("disabled"));
        verify(ticketTypeFlowService).replaceAll(eq(DOMAIN_ID), eq(TYPE_ID), eq(statusFlow), eq(List.of()));
        verify(ticketFormSchemaService, never()).saveDraft(anyLong(), anyLong(), any());
        verify(ticketFormSchemaService, never()).publish(anyLong(), anyLong(), any(), any());
        assertThat(updated.name()).isEqualTo("新名称");
        assertThat(updated.form_schema()).isNotNull();
    }

    @Test
    void deleteTicketTypeRemovesFormSchemaRows() {
        when(ticketTypeRepository.findRequiredByIdAndDomainId(TYPE_ID, DOMAIN_ID)).thenReturn(ticketTypePo());
        when(ticketTypeRepository.countTicketsByTypeId(DOMAIN_ID, TYPE_ID)).thenReturn(0);
        when(ticketTypeRepository.deleteByIdAndDomainId(TYPE_ID, DOMAIN_ID)).thenReturn(1);

        ticketConfigService.deleteTicketType(DOMAIN_ID, TYPE_ID);

        verify(ticketFormSchemaService).deleteByTicketType(DOMAIN_ID, TYPE_ID);
    }

    private TicketTypePo ticketTypePo() {
        TicketTypePo po = new TicketTypePo();
        po.setId(TYPE_ID);
        po.setBusinessDomainId(DOMAIN_ID);
        po.setCode("default");
        po.setName("默认类型");
        po.setStatus("active");
        return po;
    }
}
