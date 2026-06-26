package com.uniondesk.ticket.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniondesk.ticket.entity.TicketTypePo;
import com.uniondesk.ticket.repository.QuickReplyTemplateRepository;
import com.uniondesk.ticket.repository.TicketPriorityLevelRepository;
import com.uniondesk.ticket.repository.TicketTemplateRepository;
import com.uniondesk.ticket.repository.TicketTypeRepository;
import com.uniondesk.ticket.web.TicketConfigDtos;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
                objectMapper);
    }

    @Test
    void saveFormSchemaDraftOnlyUpdatesDraftColumn() throws Exception {
        TicketTypePo existing = ticketTypePo(DEFAULT_SCHEMA_JSON, DEFAULT_SCHEMA_JSON);
        TicketTypePo updated = ticketTypePo(DEFAULT_SCHEMA_JSON, DRAFT_WITH_CUSTOM_JSON);
        when(ticketTypeRepository.findRequiredByIdAndDomainId(TYPE_ID, DOMAIN_ID))
                .thenReturn(existing, updated);

        Map<String, Object> draftSchema = objectMapper.readValue(DRAFT_WITH_CUSTOM_JSON, Map.class);

        TicketConfigDtos.TicketTypeView result = ticketConfigService.saveFormSchemaDraft(DOMAIN_ID, TYPE_ID, draftSchema);

        ArgumentCaptor<String> draftCaptor = ArgumentCaptor.forClass(String.class);
        verify(ticketTypeRepository).updateFormSchemaDraft(eq(TYPE_ID), eq(DOMAIN_ID), draftCaptor.capture());
        verify(ticketTypeRepository, never()).publishFormSchema(anyLong(), anyLong(), anyString(), anyString());
        verify(ticketTypeRepository, never()).updateMetadata(anyLong(), anyLong(), anyString(), any(), any(), anyString(), anyString());
        assertThat(draftCaptor.getValue()).contains("\"draftOnly\"");
        assertThat(result.form_schema_draft()).isInstanceOf(Map.class);
    }

    @Test
    void publishFormSchemaCopiesDraftToPublishedAndSyncsDraft() {
        TicketTypePo existing = ticketTypePo(DEFAULT_SCHEMA_JSON, DRAFT_WITH_CUSTOM_JSON);
        TicketTypePo published = ticketTypePo(DRAFT_WITH_CUSTOM_JSON, DRAFT_WITH_CUSTOM_JSON);
        when(ticketTypeRepository.findRequiredByIdAndDomainId(TYPE_ID, DOMAIN_ID))
                .thenReturn(existing, published);

        ticketConfigService.publishFormSchema(DOMAIN_ID, TYPE_ID);

        ArgumentCaptor<String> publishedCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> draftCaptor = ArgumentCaptor.forClass(String.class);
        verify(ticketTypeRepository).publishFormSchema(eq(TYPE_ID), eq(DOMAIN_ID), publishedCaptor.capture(), draftCaptor.capture());
        assertThat(publishedCaptor.getValue()).contains("\"draftOnly\"");
        assertThat(draftCaptor.getValue()).isEqualTo(publishedCaptor.getValue());
    }

    @Test
    void publishFormSchemaIsIdempotentWhenDraftMissing() {
        TicketTypePo existing = ticketTypePo(DEFAULT_SCHEMA_JSON, null);
        when(ticketTypeRepository.findRequiredByIdAndDomainId(TYPE_ID, DOMAIN_ID))
                .thenReturn(existing, existing);

        ticketConfigService.publishFormSchema(DOMAIN_ID, TYPE_ID);
        ticketConfigService.publishFormSchema(DOMAIN_ID, TYPE_ID);

        verify(ticketTypeRepository, org.mockito.Mockito.times(2))
                .publishFormSchema(eq(TYPE_ID), eq(DOMAIN_ID), anyString(), anyString());
    }

    @Test
    void createTicketTypeRejectsDuplicateCodeAndName() {
        when(ticketTypeRepository.findByDomainIdAndCode(DOMAIN_ID, "technical"))
                .thenReturn(ticketTypePo(null, null));

        assertThatThrownBy(() -> ticketConfigService.createTicketType(
                DOMAIN_ID,
                new TicketConfigDtos.CreateTicketTypeRequest("technical", "技术支持", null, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("该域下编码已存在");

        when(ticketTypeRepository.findByDomainIdAndCode(DOMAIN_ID, "technical")).thenReturn(null);
        when(ticketTypeRepository.findByDomainIdAndName(DOMAIN_ID, "技术支持"))
                .thenReturn(ticketTypePo(null, null));

        assertThatThrownBy(() -> ticketConfigService.createTicketType(
                DOMAIN_ID,
                new TicketConfigDtos.CreateTicketTypeRequest("technical", "技术支持", null, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("该域下名称已存在");
    }

    @Test
    void updateTicketTypeDoesNotWriteFormSchemaColumns() {
        TicketTypePo existing = ticketTypePo(DEFAULT_SCHEMA_JSON, DRAFT_WITH_CUSTOM_JSON);
        existing.setName("旧名称");
        when(ticketTypeRepository.findRequiredByIdAndDomainId(TYPE_ID, DOMAIN_ID)).thenReturn(existing);
        when(ticketTypeRepository.findByDomainIdAndName(DOMAIN_ID, "新名称")).thenReturn(null);

        TicketConfigDtos.UpdateTicketTypeRequest request = new TicketConfigDtos.UpdateTicketTypeRequest(
                "新名称",
                "描述",
                "mdi:help",
                Map.of("states", List.of(Map.of("code", "closed", "name", "已关闭", "state_type", "terminal", "allow_customer_withdraw", false)), "transitions", List.of()),
                "disabled");

        TicketConfigDtos.TicketTypeView updated = ticketConfigService.updateTicketType(DOMAIN_ID, TYPE_ID, request);

        verify(ticketTypeRepository).updateMetadata(
                eq(TYPE_ID),
                eq(DOMAIN_ID),
                eq("新名称"),
                eq("描述"),
                eq("mdi:help"),
                anyString(),
                eq("disabled"));
        verify(ticketTypeRepository, never()).updateFormSchemaDraft(anyLong(), anyLong(), anyString());
        verify(ticketTypeRepository, never()).publishFormSchema(anyLong(), anyLong(), anyString(), anyString());
        assertThat(updated.name()).isEqualTo("新名称");
        assertThat(updated.description()).isEqualTo("描述");
        assertThat(updated.icon()).isEqualTo("mdi:help");
        assertThat(updated.form_schema()).isNotNull();
        assertThat(updated.form_schema_draft()).isNotNull();
    }

    private TicketTypePo ticketTypePo(String formSchema, String formSchemaDraft) {
        TicketTypePo po = new TicketTypePo();
        po.setId(TYPE_ID);
        po.setBusinessDomainId(DOMAIN_ID);
        po.setCode("default");
        po.setName("默认类型");
        po.setStatusFlowConfig("{\"states\":[],\"transitions\":[]}");
        po.setFormSchema(formSchema);
        po.setFormSchemaDraft(formSchemaDraft);
        po.setStatus("active");
        return po;
    }
}
