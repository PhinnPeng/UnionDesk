package com.uniondesk.ticket.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniondesk.ticket.entity.TicketFormSchemaPo;
import com.uniondesk.ticket.repository.TicketFormSchemaRepository;
import com.uniondesk.ticket.web.TicketConfigDtos;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class TicketFormSchemaService {

    private final TicketFormSchemaRepository ticketFormSchemaRepository;
    private final ObjectMapper objectMapper;

    public TicketFormSchemaService(
            TicketFormSchemaRepository ticketFormSchemaRepository,
            ObjectMapper objectMapper) {
        this.ticketFormSchemaRepository = ticketFormSchemaRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void initializeForNewType(long domainId, long ticketTypeId, String formSchemaJson) {
        insertPublished(domainId, ticketTypeId, formSchemaJson, 1, null);
        insertDraft(domainId, ticketTypeId, formSchemaJson);
    }

    @Transactional
    public void initializeForNewPlatformType(long ticketTypeId, String formSchemaJson) {
        initializeForNewType(TicketFormSchemaRepository.PLATFORM_SCHEMA_DOMAIN_KEY, ticketTypeId, formSchemaJson);
    }

    @Transactional
    public FormSchemaAggregate saveDraft(long domainId, long ticketTypeId, Object schema) {
        String formSchemaJson = validateAndSerialize(schema);
        upsertDraft(domainId, ticketTypeId, formSchemaJson);
        return loadAggregate(domainId, ticketTypeId);
    }

    @Transactional
    public FormSchemaAggregate publish(long domainId, long ticketTypeId, Object schema, Long publishedBy) {
        String formSchemaJson = resolvePublishSchemaJson(domainId, ticketTypeId, schema);
        int nextVersion = ticketFormSchemaRepository.findMaxPublishedVersionNo(ticketTypeId, domainId) + 1;
        insertPublished(domainId, ticketTypeId, formSchemaJson, nextVersion, publishedBy);
        upsertDraft(domainId, ticketTypeId, formSchemaJson);
        ticketFormSchemaRepository.trimPublishedHistory(ticketTypeId, domainId);
        return loadAggregate(domainId, ticketTypeId);
    }

    @Transactional
    public FormSchemaAggregate rollback(long domainId, long ticketTypeId, int versionNo, Long publishedBy) {
        TicketFormSchemaPo target = ticketFormSchemaRepository.findPublishedByVersionNo(ticketTypeId, domainId, versionNo);
        if (target == null) {
            throw new IllegalArgumentException("未找到该发布版本");
        }
        return publish(domainId, ticketTypeId, readJsonObject(target.getFormSchema()), publishedBy);
    }

    public List<TicketConfigDtos.FormSchemaVersionSummaryView> listPublishedVersions(long domainId, long ticketTypeId) {
        FormSchemaAggregate aggregate = loadAggregate(domainId, ticketTypeId);
        return ticketFormSchemaRepository.listPublished(ticketTypeId, domainId, TicketFormSchemaRepository.MAX_PUBLISHED_VERSIONS)
                .stream()
                .map(row -> new TicketConfigDtos.FormSchemaVersionSummaryView(
                        row.getVersionNo(),
                        row.getVersionNo() == aggregate.currentVersionNo(),
                        toDateTimeString(row.getPublishedAt()),
                        row.getPublishedBy() == null ? null : String.valueOf(row.getPublishedBy())))
                .toList();
    }

    public TicketConfigDtos.FormSchemaVersionDetailView getPublishedVersion(long domainId, long ticketTypeId, int versionNo) {
        TicketFormSchemaPo row = ticketFormSchemaRepository.findPublishedByVersionNo(ticketTypeId, domainId, versionNo);
        if (row == null) {
            throw new IllegalArgumentException("未找到该发布版本");
        }
        return new TicketConfigDtos.FormSchemaVersionDetailView(
                row.getVersionNo(),
                readJsonObject(row.getFormSchema()),
                toDateTimeString(row.getPublishedAt()),
                row.getPublishedBy() == null ? null : String.valueOf(row.getPublishedBy()));
    }

    @Transactional
    public void deleteByTicketType(long domainId, long ticketTypeId) {
        ticketFormSchemaRepository.deleteByTicketTypeId(ticketTypeId, domainId);
    }

    public FormSchemaAggregate loadAggregate(long domainId, long ticketTypeId) {
        return loadAggregate(domainId, ticketTypeId, null);
    }

    public FormSchemaAggregate loadAggregate(long domainId, long ticketTypeId, String currentPluginRevision) {
        TicketFormSchemaPo draft = ticketFormSchemaRepository.findDraft(ticketTypeId, domainId);
        TicketFormSchemaPo published = ticketFormSchemaRepository.findLatestPublished(ticketTypeId, domainId);
        String draftJson = draft == null ? null : draft.getFormSchema();
        String publishedJson = published == null ? null : published.getFormSchema();
        int currentVersionNo = published == null ? 0 : published.getVersionNo();
        boolean hasUnpublished = hasUnpublished(draftJson, publishedJson, published, currentPluginRevision);
        return new FormSchemaAggregate(
                readJsonObject(publishedJson),
                readJsonObject(draftJson),
                currentVersionNo,
                hasUnpublished);
    }

    public boolean hasUnpublishedByPluginRevision(long domainId, long ticketTypeId, String currentPluginRevision) {
        TicketFormSchemaPo published = ticketFormSchemaRepository.findLatestPublished(ticketTypeId, domainId);
        if (published == null) {
            return StringUtils.hasText(currentPluginRevision);
        }
        if (!StringUtils.hasText(currentPluginRevision)) {
            return false;
        }
        return !currentPluginRevision.equals(published.getPluginRevision());
    }

    @Transactional
    public FormSchemaAggregate saveDraftFromMaterialized(
            long domainId,
            long ticketTypeId,
            Map<String, Object> snapshot,
            String pluginRevision) {
        String formSchemaJson = validateAndSerialize(snapshot);
        upsertDraftMaterialized(domainId, ticketTypeId, formSchemaJson, pluginRevision);
        return loadAggregate(domainId, ticketTypeId, pluginRevision);
    }

    @Transactional
    public FormSchemaAggregate publishFromMaterialized(
            long domainId,
            long ticketTypeId,
            Map<String, Object> snapshot,
            String pluginRevision,
            Long publishedBy) {
        String formSchemaJson = validateAndSerialize(snapshot);
        int nextVersion = ticketFormSchemaRepository.findMaxPublishedVersionNo(ticketTypeId, domainId) + 1;
        insertPublished(domainId, ticketTypeId, formSchemaJson, nextVersion, publishedBy, pluginRevision);
        upsertDraftMaterialized(domainId, ticketTypeId, formSchemaJson, pluginRevision);
        ticketFormSchemaRepository.trimPublishedHistory(ticketTypeId, domainId);
        return loadAggregate(domainId, ticketTypeId, pluginRevision);
    }

    private void upsertDraftMaterialized(
            long domainId,
            long ticketTypeId,
            String formSchemaJson,
            String pluginRevision) {
        TicketFormSchemaPo existing = ticketFormSchemaRepository.findDraft(ticketTypeId, domainId);
        if (existing == null) {
            insertDraft(domainId, ticketTypeId, formSchemaJson, pluginRevision);
            return;
        }
        ticketFormSchemaRepository.updateDraftMaterialized(
                ticketTypeId, domainId, formSchemaJson, pluginRevision);
    }

    private void insertDraft(long domainId, long ticketTypeId, String formSchemaJson) {
        insertDraft(domainId, ticketTypeId, formSchemaJson, null);
    }

    private void insertDraft(long domainId, long ticketTypeId, String formSchemaJson, String pluginRevision) {
        TicketFormSchemaPo po = new TicketFormSchemaPo();
        po.setBusinessDomainId(domainId);
        po.setTicketTypeId(ticketTypeId);
        po.setRecordType(TicketFormSchemaPo.RECORD_DRAFT);
        po.setVersionNo(0);
        po.setFormSchema(formSchemaJson);
        po.setPluginRevision(pluginRevision);
        ticketFormSchemaRepository.insert(po);
    }

    private void insertPublished(long domainId, long ticketTypeId, String formSchemaJson, int versionNo, Long publishedBy) {
        insertPublished(domainId, ticketTypeId, formSchemaJson, versionNo, publishedBy, null);
    }

    private void insertPublished(
            long domainId,
            long ticketTypeId,
            String formSchemaJson,
            int versionNo,
            Long publishedBy,
            String pluginRevision) {
        TicketFormSchemaPo po = new TicketFormSchemaPo();
        po.setBusinessDomainId(domainId);
        po.setTicketTypeId(ticketTypeId);
        po.setRecordType(TicketFormSchemaPo.RECORD_PUBLISHED);
        po.setVersionNo(versionNo);
        po.setFormSchema(formSchemaJson);
        po.setPluginRevision(pluginRevision);
        po.setPublishedBy(publishedBy);
        po.setPublishedAt(LocalDateTime.now());
        ticketFormSchemaRepository.insert(po);
    }

    private boolean hasUnpublished(
            String draftJson,
            String publishedJson,
            TicketFormSchemaPo published,
            String currentPluginRevision) {
        if (StringUtils.hasText(currentPluginRevision)) {
            if (published == null) {
                return true;
            }
            return !currentPluginRevision.equals(published.getPluginRevision());
        }
        return hasUnpublished(draftJson, publishedJson);
    }

    private String resolvePublishSchemaJson(long domainId, long ticketTypeId, Object schema) {
        if (schema != null) {
            return validateAndSerialize(schema);
        }
        TicketFormSchemaPo draft = ticketFormSchemaRepository.findDraft(ticketTypeId, domainId);
        if (draft != null && StringUtils.hasText(draft.getFormSchema())) {
            return validateAndSerialize(readJsonObject(draft.getFormSchema()));
        }
        TicketFormSchemaPo published = ticketFormSchemaRepository.findLatestPublished(ticketTypeId, domainId);
        if (published != null && StringUtils.hasText(published.getFormSchema())) {
            return validateAndSerialize(readJsonObject(published.getFormSchema()));
        }
        return validateAndSerialize(null);
    }

    private void upsertDraft(long domainId, long ticketTypeId, String formSchemaJson) {
        TicketFormSchemaPo existing = ticketFormSchemaRepository.findDraft(ticketTypeId, domainId);
        if (existing == null) {
            insertDraft(domainId, ticketTypeId, formSchemaJson);
            return;
        }
        ticketFormSchemaRepository.updateDraftSchema(ticketTypeId, domainId, formSchemaJson);
    }

    private String validateAndSerialize(Object schema) {
        Map<String, Object> validated = FormSchemaValidator.mergeAndValidate(schema, objectMapper);
        return toJson(validated);
    }

    private boolean hasUnpublished(String draftJson, String publishedJson) {
        if (!StringUtils.hasText(draftJson)) {
            return false;
        }
        if (!StringUtils.hasText(publishedJson)) {
            return true;
        }
        return !Objects.equals(normalizeJson(draftJson), normalizeJson(publishedJson));
    }

    private String normalizeJson(String json) {
        try {
            Object parsed = objectMapper.readValue(json, Object.class);
            return objectMapper.writeValueAsString(parsed);
        }
        catch (JsonProcessingException ex) {
            return json;
        }
    }

    private Object readJsonObject(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, Object.class);
        }
        catch (JsonProcessingException ex) {
            throw new IllegalStateException("invalid json payload", ex);
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        }
        catch (JsonProcessingException ex) {
            throw new IllegalStateException("failed to serialize json payload", ex);
        }
    }

    private String toDateTimeString(LocalDateTime value) {
        return value == null ? null : value.toString();
    }

    public record FormSchemaAggregate(
            Object publishedSchema,
            Object draftSchema,
            int currentVersionNo,
            boolean hasUnpublished) {
    }
}
