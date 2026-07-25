package com.uniondesk.ticket.repository;

import com.uniondesk.ticket.entity.TicketFormSchemaPo;
import com.uniondesk.ticket.mapper.TicketFormSchemaMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class TicketFormSchemaRepository {

    public static final int MAX_PUBLISHED_VERSIONS = 10;
    public static final long PLATFORM_SCHEMA_DOMAIN_KEY = 0L;

    private final TicketFormSchemaMapper mapper;

    public TicketFormSchemaRepository(TicketFormSchemaMapper mapper) {
        this.mapper = mapper;
    }

    public TicketFormSchemaPo findDraft(long ticketTypeId, long domainId) {
        return mapper.findDraftByTicketTypeId(ticketTypeId, domainId);
    }

    public TicketFormSchemaPo findLatestPublished(long ticketTypeId, long domainId) {
        return mapper.findLatestPublishedByTicketTypeId(ticketTypeId, domainId);
    }

    public TicketFormSchemaPo findPublishedByVersionNo(long ticketTypeId, long domainId, int versionNo) {
        return mapper.findPublishedByVersionNo(ticketTypeId, domainId, versionNo);
    }

    public List<TicketFormSchemaPo> listPublished(long ticketTypeId, long domainId, int limit) {
        return mapper.listPublishedByTicketTypeId(ticketTypeId, domainId, limit);
    }

    public int findMaxPublishedVersionNo(long ticketTypeId, long domainId) {
        Integer max = mapper.findMaxPublishedVersionNo(ticketTypeId, domainId);
        return max == null ? 0 : max;
    }

    public void insert(TicketFormSchemaPo po) {
        mapper.insert(po);
    }

    public void updateDraftSchema(long ticketTypeId, long domainId, String formSchemaJson) {
        mapper.updateDraftSchema(ticketTypeId, domainId, formSchemaJson);
    }

    public void updateDraftMaterialized(
            long ticketTypeId,
            long domainId,
            String formSchemaJson,
            String pluginRevision) {
        mapper.updateDraftMaterialized(ticketTypeId, domainId, formSchemaJson, pluginRevision);
    }

    public void deleteByTicketTypeId(long ticketTypeId, long domainId) {
        mapper.deleteByTicketTypeId(ticketTypeId, domainId);
    }

    public void trimPublishedHistory(long ticketTypeId, long domainId) {
        List<TicketFormSchemaPo> published = listPublished(ticketTypeId, domainId, MAX_PUBLISHED_VERSIONS + 50);
        if (published.size() <= MAX_PUBLISHED_VERSIONS) {
            return;
        }
        for (int index = MAX_PUBLISHED_VERSIONS; index < published.size(); index++) {
            TicketFormSchemaPo row = published.get(index);
            mapper.deletePublishedByVersionNo(ticketTypeId, domainId, row.getVersionNo());
        }
    }
}
