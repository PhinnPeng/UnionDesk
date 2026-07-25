package com.uniondesk.ticket.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniondesk.ticket.entity.TicketFormSchemaPo;
import com.uniondesk.ticket.mapper.TicketFormSchemaMapper;
import com.uniondesk.ticket.repository.TicketFormSchemaRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TicketFormSchemaServiceTests {

    private static final long DOMAIN_ID = 1L;
    private static final long TYPE_ID = 11L;
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

    @Mock
    private TicketFormSchemaRepository ticketFormSchemaRepository;

    private TicketFormSchemaService ticketFormSchemaService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        ticketFormSchemaService = new TicketFormSchemaService(ticketFormSchemaRepository, objectMapper);
    }

    @Test
    void saveDraftUpsertsDraftRow() throws Exception {
        when(ticketFormSchemaRepository.findDraft(TYPE_ID, DOMAIN_ID))
                .thenReturn(null)
                .thenReturn(draftRow(DRAFT_WITH_CUSTOM_JSON));
        when(ticketFormSchemaRepository.findLatestPublished(TYPE_ID, DOMAIN_ID))
                .thenReturn(publishedRow(1, DEFAULT_SCHEMA_JSON));
        Map<String, Object> draftSchema = objectMapper.readValue(DRAFT_WITH_CUSTOM_JSON, Map.class);

        TicketFormSchemaService.FormSchemaAggregate aggregate = ticketFormSchemaService.saveDraft(DOMAIN_ID, TYPE_ID, draftSchema);

        ArgumentCaptor<TicketFormSchemaPo> captor = ArgumentCaptor.forClass(TicketFormSchemaPo.class);
        verify(ticketFormSchemaRepository).insert(captor.capture());
        assertThat(captor.getValue().getRecordType()).isEqualTo(TicketFormSchemaPo.RECORD_DRAFT);
        assertThat(captor.getValue().getVersionNo()).isZero();
        assertThat(captor.getValue().getFormSchema()).contains("\"draftOnly\"");
        assertThat(aggregate.draftSchema()).isInstanceOf(Map.class);
    }

    @Test
    void publishInsertsPublishedAndSyncsDraft() throws Exception {
        when(ticketFormSchemaRepository.findMaxPublishedVersionNo(TYPE_ID, DOMAIN_ID)).thenReturn(1);
        when(ticketFormSchemaRepository.findDraft(TYPE_ID, DOMAIN_ID))
                .thenReturn(draftRow(DRAFT_WITH_CUSTOM_JSON));
        when(ticketFormSchemaRepository.findLatestPublished(TYPE_ID, DOMAIN_ID))
                .thenReturn(publishedRow(2, DRAFT_WITH_CUSTOM_JSON));
        Map<String, Object> draftSchema = objectMapper.readValue(DRAFT_WITH_CUSTOM_JSON, Map.class);

        ticketFormSchemaService.publish(DOMAIN_ID, TYPE_ID, draftSchema, null);

        ArgumentCaptor<TicketFormSchemaPo> captor = ArgumentCaptor.forClass(TicketFormSchemaPo.class);
        verify(ticketFormSchemaRepository).insert(captor.capture());
        TicketFormSchemaPo published = captor.getValue();
        assertThat(published.getRecordType()).isEqualTo(TicketFormSchemaPo.RECORD_PUBLISHED);
        assertThat(published.getVersionNo()).isEqualTo(2);
        verify(ticketFormSchemaRepository).updateDraftSchema(eq(TYPE_ID), eq(DOMAIN_ID), any());
        verify(ticketFormSchemaRepository).trimPublishedHistory(TYPE_ID, DOMAIN_ID);
    }

    @Test
    void rollbackPublishesTargetAsNewVersion() {
        TicketFormSchemaPo target = publishedRow(2, DRAFT_WITH_CUSTOM_JSON);
        when(ticketFormSchemaRepository.findPublishedByVersionNo(TYPE_ID, DOMAIN_ID, 2)).thenReturn(target);
        when(ticketFormSchemaRepository.findMaxPublishedVersionNo(TYPE_ID, DOMAIN_ID)).thenReturn(3);
        when(ticketFormSchemaRepository.findDraft(TYPE_ID, DOMAIN_ID))
                .thenReturn(draftRow(DRAFT_WITH_CUSTOM_JSON));
        when(ticketFormSchemaRepository.findLatestPublished(TYPE_ID, DOMAIN_ID))
                .thenReturn(publishedRow(4, DRAFT_WITH_CUSTOM_JSON));

        ticketFormSchemaService.rollback(DOMAIN_ID, TYPE_ID, 2, null);

        ArgumentCaptor<TicketFormSchemaPo> captor = ArgumentCaptor.forClass(TicketFormSchemaPo.class);
        verify(ticketFormSchemaRepository).insert(captor.capture());
        assertThat(captor.getValue().getVersionNo()).isEqualTo(4);
        assertThat(captor.getValue().getFormSchema()).contains("\"draftOnly\"");
    }

    @Test
    void rollbackThrowsWhenVersionMissing() {
        when(ticketFormSchemaRepository.findPublishedByVersionNo(TYPE_ID, DOMAIN_ID, 99)).thenReturn(null);

        assertThatThrownBy(() -> ticketFormSchemaService.rollback(DOMAIN_ID, TYPE_ID, 99, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("未找到该发布版本");
    }

    @Test
    void loadAggregateMarksUnpublishedWhenDraftDiffers() {
        when(ticketFormSchemaRepository.findDraft(TYPE_ID, DOMAIN_ID))
                .thenReturn(draftRow(DRAFT_WITH_CUSTOM_JSON));
        when(ticketFormSchemaRepository.findLatestPublished(TYPE_ID, DOMAIN_ID))
                .thenReturn(publishedRow(1, DEFAULT_SCHEMA_JSON));

        TicketFormSchemaService.FormSchemaAggregate aggregate = ticketFormSchemaService.loadAggregate(DOMAIN_ID, TYPE_ID);

        assertThat(aggregate.currentVersionNo()).isEqualTo(1);
        assertThat(aggregate.hasUnpublished()).isTrue();
    }

    @Test
    void initializeForNewTypeCreatesDraftAndPublishedV1() {
        ticketFormSchemaService.initializeForNewType(DOMAIN_ID, TYPE_ID, DEFAULT_SCHEMA_JSON);

        ArgumentCaptor<TicketFormSchemaPo> captor = ArgumentCaptor.forClass(TicketFormSchemaPo.class);
        verify(ticketFormSchemaRepository, org.mockito.Mockito.times(2)).insert(captor.capture());
        List<TicketFormSchemaPo> rows = captor.getAllValues();
        assertThat(rows).extracting(TicketFormSchemaPo::getRecordType)
                .containsExactly(TicketFormSchemaPo.RECORD_PUBLISHED, TicketFormSchemaPo.RECORD_DRAFT);
        assertThat(rows.get(0).getVersionNo()).isEqualTo(1);
        assertThat(rows.get(1).getVersionNo()).isZero();
    }

    @Test
    void trimPublishedHistoryDeletesOldestBeyondTen() {
        TicketFormSchemaMapper mapper = org.mockito.Mockito.mock(TicketFormSchemaMapper.class);
        TicketFormSchemaRepository repository = new TicketFormSchemaRepository(mapper);
        List<TicketFormSchemaPo> published = new ArrayList<>();
        for (int versionNo = 12; versionNo >= 1; versionNo--) {
            published.add(publishedRow(versionNo, DEFAULT_SCHEMA_JSON));
        }
        when(mapper.listPublishedByTicketTypeId(TYPE_ID, DOMAIN_ID, 60)).thenReturn(published);

        repository.trimPublishedHistory(TYPE_ID, DOMAIN_ID);

        verify(mapper).deletePublishedByVersionNo(TYPE_ID, DOMAIN_ID, 1);
        verify(mapper).deletePublishedByVersionNo(TYPE_ID, DOMAIN_ID, 2);
        verify(mapper, never()).deletePublishedByVersionNo(TYPE_ID, DOMAIN_ID, 3);
    }

    private TicketFormSchemaPo draftRow(String schemaJson) {
        TicketFormSchemaPo po = new TicketFormSchemaPo();
        po.setRecordType(TicketFormSchemaPo.RECORD_DRAFT);
        po.setVersionNo(0);
        po.setFormSchema(schemaJson);
        return po;
    }

    private TicketFormSchemaPo publishedRow(int versionNo, String schemaJson) {
        TicketFormSchemaPo po = new TicketFormSchemaPo();
        po.setRecordType(TicketFormSchemaPo.RECORD_PUBLISHED);
        po.setVersionNo(versionNo);
        po.setFormSchema(schemaJson);
        return po;
    }
}
