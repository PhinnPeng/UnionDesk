package com.uniondesk.ticket.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryMethods;
import com.mybatisflex.core.query.QueryWrapper;
import com.uniondesk.ticket.entity.TicketFormSchemaPo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TicketFormSchemaMapper extends BaseMapper<TicketFormSchemaPo> {

    default TicketFormSchemaPo findDraftByTicketTypeId(long ticketTypeId, long domainId) {
        return selectOneByQuery(QueryWrapper.create()
                .from(TicketFormSchemaPo.class)
                .where(TicketFormSchemaPo::getTicketTypeId).eq(ticketTypeId)
                .and(TicketFormSchemaPo::getBusinessDomainId).eq(domainId)
                .and(TicketFormSchemaPo::getRecordType).eq(TicketFormSchemaPo.RECORD_DRAFT)
                .and(TicketFormSchemaPo::getVersionNo).eq(0));
    }

    default TicketFormSchemaPo findLatestPublishedByTicketTypeId(long ticketTypeId, long domainId) {
        return selectOneByQuery(QueryWrapper.create()
                .from(TicketFormSchemaPo.class)
                .where(TicketFormSchemaPo::getTicketTypeId).eq(ticketTypeId)
                .and(TicketFormSchemaPo::getBusinessDomainId).eq(domainId)
                .and(TicketFormSchemaPo::getRecordType).eq(TicketFormSchemaPo.RECORD_PUBLISHED)
                .orderBy(TicketFormSchemaPo::getVersionNo, false));
    }

    default TicketFormSchemaPo findPublishedByVersionNo(long ticketTypeId, long domainId, int versionNo) {
        return selectOneByQuery(QueryWrapper.create()
                .from(TicketFormSchemaPo.class)
                .where(TicketFormSchemaPo::getTicketTypeId).eq(ticketTypeId)
                .and(TicketFormSchemaPo::getBusinessDomainId).eq(domainId)
                .and(TicketFormSchemaPo::getRecordType).eq(TicketFormSchemaPo.RECORD_PUBLISHED)
                .and(TicketFormSchemaPo::getVersionNo).eq(versionNo));
    }

    default List<TicketFormSchemaPo> listPublishedByTicketTypeId(long ticketTypeId, long domainId, int limit) {
        return selectListByQuery(QueryWrapper.create()
                .from(TicketFormSchemaPo.class)
                .where(TicketFormSchemaPo::getTicketTypeId).eq(ticketTypeId)
                .and(TicketFormSchemaPo::getBusinessDomainId).eq(domainId)
                .and(TicketFormSchemaPo::getRecordType).eq(TicketFormSchemaPo.RECORD_PUBLISHED)
                .orderBy(TicketFormSchemaPo::getVersionNo, false)
                .limit(limit));
    }

    default Integer findMaxPublishedVersionNo(long ticketTypeId, long domainId) {
        return selectObjectByQueryAs(QueryWrapper.create()
                .select(QueryMethods.max(TicketFormSchemaPo::getVersionNo))
                .from(TicketFormSchemaPo.class)
                .where(TicketFormSchemaPo::getTicketTypeId).eq(ticketTypeId)
                .and(TicketFormSchemaPo::getBusinessDomainId).eq(domainId)
                .and(TicketFormSchemaPo::getRecordType).eq(TicketFormSchemaPo.RECORD_PUBLISHED), Integer.class);
    }

    default void updateDraftSchema(long ticketTypeId, long domainId, String formSchema) {
        TicketFormSchemaPo set = new TicketFormSchemaPo();
        set.setFormSchema(formSchema);
        updateByQuery(set, QueryWrapper.create()
                .from(TicketFormSchemaPo.class)
                .where(TicketFormSchemaPo::getTicketTypeId).eq(ticketTypeId)
                .and(TicketFormSchemaPo::getBusinessDomainId).eq(domainId)
                .and(TicketFormSchemaPo::getRecordType).eq(TicketFormSchemaPo.RECORD_DRAFT)
                .and(TicketFormSchemaPo::getVersionNo).eq(0));
    }

    default void updateDraftMaterialized(long ticketTypeId, long domainId, String formSchema, String pluginRevision) {
        TicketFormSchemaPo set = new TicketFormSchemaPo();
        set.setFormSchema(formSchema);
        set.setPluginRevision(pluginRevision);
        updateByQuery(set, QueryWrapper.create()
                .from(TicketFormSchemaPo.class)
                .where(TicketFormSchemaPo::getTicketTypeId).eq(ticketTypeId)
                .and(TicketFormSchemaPo::getBusinessDomainId).eq(domainId)
                .and(TicketFormSchemaPo::getRecordType).eq(TicketFormSchemaPo.RECORD_DRAFT)
                .and(TicketFormSchemaPo::getVersionNo).eq(0));
    }

    default int deleteByTicketTypeId(long ticketTypeId, long domainId) {
        return deleteByQuery(QueryWrapper.create()
                .from(TicketFormSchemaPo.class)
                .where(TicketFormSchemaPo::getTicketTypeId).eq(ticketTypeId)
                .and(TicketFormSchemaPo::getBusinessDomainId).eq(domainId));
    }

    default int deletePublishedByVersionNo(long ticketTypeId, long domainId, int versionNo) {
        return deleteByQuery(QueryWrapper.create()
                .from(TicketFormSchemaPo.class)
                .where(TicketFormSchemaPo::getTicketTypeId).eq(ticketTypeId)
                .and(TicketFormSchemaPo::getBusinessDomainId).eq(domainId)
                .and(TicketFormSchemaPo::getRecordType).eq(TicketFormSchemaPo.RECORD_PUBLISHED)
                .and(TicketFormSchemaPo::getVersionNo).eq(versionNo));
    }
}
