package com.uniondesk.ticket.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.If;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryMethods;
import com.mybatisflex.core.query.QueryWrapper;
import com.uniondesk.ticket.entity.TicketTypePo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TicketTypeMapper extends BaseMapper<TicketTypePo> {

    default List<TicketTypePo> findByDomainId(long domainId) {
        return selectListByQuery(QueryWrapper.create()
                .from(TicketTypePo.class)
                .where(TicketTypePo::getScope).eq(TicketTypePo.SCOPE_DOMAIN)
                .and(TicketTypePo::getBusinessDomainId).eq(domainId)
                .orderBy(TicketTypePo::getId, true));
    }

    default TicketTypePo findByIdAndDomainId(long id, long domainId) {
        return selectOneByQuery(QueryWrapper.create()
                .from(TicketTypePo.class)
                .where(TicketTypePo::getId).eq(id)
                .and(TicketTypePo::getScope).eq(TicketTypePo.SCOPE_DOMAIN)
                .and(TicketTypePo::getBusinessDomainId).eq(domainId));
    }

    default TicketTypePo findByDomainIdAndCode(long domainId, String code) {
        return selectOneByQuery(QueryWrapper.create()
                .from(TicketTypePo.class)
                .where(TicketTypePo::getScope).eq(TicketTypePo.SCOPE_DOMAIN)
                .and(TicketTypePo::getBusinessDomainId).eq(domainId)
                .and(TicketTypePo::getCode).eq(code));
    }

    default TicketTypePo findByDomainIdAndName(long domainId, String name) {
        return selectOneByQuery(QueryWrapper.create()
                .from(TicketTypePo.class)
                .where(TicketTypePo::getScope).eq(TicketTypePo.SCOPE_DOMAIN)
                .and(TicketTypePo::getBusinessDomainId).eq(domainId)
                .and(TicketTypePo::getName).eq(name));
    }

    default TicketTypePo findByDomainIdAndSourceGlobalTypeId(long domainId, long sourceGlobalTypeId) {
        return selectOneByQuery(QueryWrapper.create()
                .from(TicketTypePo.class)
                .where(TicketTypePo::getScope).eq(TicketTypePo.SCOPE_DOMAIN)
                .and(TicketTypePo::getBusinessDomainId).eq(domainId)
                .and(TicketTypePo::getSourceGlobalTypeId).eq(sourceGlobalTypeId));
    }

    default Integer findMaxSortOrderDomain(long domainId) {
        return selectObjectByQueryAs(QueryWrapper.create()
                .select(QueryMethods.max(TicketTypePo::getSortOrder))
                .from(TicketTypePo.class)
                .where(TicketTypePo::getScope).eq(TicketTypePo.SCOPE_DOMAIN)
                .and(TicketTypePo::getBusinessDomainId).eq(domainId), Integer.class);
    }

    default List<TicketTypePo> findByPlatform(String keywordLike) {
        return selectListByQuery(platformScopeQuery(keywordLike)
                .orderBy(TicketTypePo::getSortOrder, true)
                .orderBy(TicketTypePo::getId, true));
    }

    default Page<TicketTypePo> selectPageByPlatform(Page<TicketTypePo> page, String keywordLike) {
        return paginate(page, platformScopeQuery(keywordLike)
                .orderBy(TicketTypePo::getSortOrder, true)
                .orderBy(TicketTypePo::getId, true));
    }

    default TicketTypePo findPlatformById(long id) {
        return selectOneByQuery(QueryWrapper.create()
                .from(TicketTypePo.class)
                .where(TicketTypePo::getId).eq(id)
                .and(TicketTypePo::getScope).eq(TicketTypePo.SCOPE_PLATFORM));
    }

    default TicketTypePo findPlatformByCode(String code) {
        return selectOneByQuery(QueryWrapper.create()
                .from(TicketTypePo.class)
                .where(TicketTypePo::getScope).eq(TicketTypePo.SCOPE_PLATFORM)
                .and(TicketTypePo::getCode).eq(code));
    }

    default TicketTypePo findPlatformByName(String name) {
        return selectOneByQuery(QueryWrapper.create()
                .from(TicketTypePo.class)
                .where(TicketTypePo::getScope).eq(TicketTypePo.SCOPE_PLATFORM)
                .and(TicketTypePo::getName).eq(name));
    }

    default Integer findMaxSortOrderPlatform() {
        return selectObjectByQueryAs(QueryWrapper.create()
                .select(QueryMethods.max(TicketTypePo::getSortOrder))
                .from(TicketTypePo.class)
                .where(TicketTypePo::getScope).eq(TicketTypePo.SCOPE_PLATFORM), Integer.class);
    }

    default long countLinkedDomainsByGlobalTypeId(long globalTypeId) {
        Long count = selectObjectByQueryAs(QueryWrapper.create()
                .select("COUNT(DISTINCT business_domain_id)")
                .from(TicketTypePo.class)
                .where(TicketTypePo::getScope).eq(TicketTypePo.SCOPE_DOMAIN)
                .and(TicketTypePo::getSourceGlobalTypeId).eq(globalTypeId), Long.class);
        return count == null ? 0L : count;
    }

    default void updateMetadata(long id, long domainId, String name, String description,
            String descriptionTemplateMd, String icon, String status) {
        TicketTypePo set = new TicketTypePo();
        set.setName(name);
        set.setDescription(description);
        set.setDescriptionTemplateMd(descriptionTemplateMd);
        set.setIcon(icon);
        set.setStatus(status);
        updateByQuery(set, QueryWrapper.create()
                .from(TicketTypePo.class)
                .where(TicketTypePo::getId).eq(id)
                .and(TicketTypePo::getScope).eq(TicketTypePo.SCOPE_DOMAIN)
                .and(TicketTypePo::getBusinessDomainId).eq(domainId));
    }

    default void updatePlatformMetadata(long id, String name, String description,
            String descriptionTemplateMd, String icon, String status) {
        TicketTypePo set = new TicketTypePo();
        set.setName(name);
        set.setDescription(description);
        set.setDescriptionTemplateMd(descriptionTemplateMd);
        set.setIcon(icon);
        set.setStatus(status);
        updateByQuery(set, QueryWrapper.create()
                .from(TicketTypePo.class)
                .where(TicketTypePo::getId).eq(id)
                .and(TicketTypePo::getScope).eq(TicketTypePo.SCOPE_PLATFORM));
    }

    default void updateSortOrder(long id, int sortOrder) {
        TicketTypePo set = new TicketTypePo();
        set.setSortOrder(sortOrder);
        updateByQuery(set, QueryWrapper.create()
                .from(TicketTypePo.class)
                .where(TicketTypePo::getId).eq(id)
                .and(TicketTypePo::getScope).eq(TicketTypePo.SCOPE_PLATFORM));
    }

    default int deleteByIdAndDomainId(long id, long domainId) {
        return deleteByQuery(QueryWrapper.create()
                .from(TicketTypePo.class)
                .where(TicketTypePo::getId).eq(id)
                .and(TicketTypePo::getScope).eq(TicketTypePo.SCOPE_DOMAIN)
                .and(TicketTypePo::getBusinessDomainId).eq(domainId));
    }

    default int deletePlatformById(long id) {
        return deleteByQuery(QueryWrapper.create()
                .from(TicketTypePo.class)
                .where(TicketTypePo::getId).eq(id)
                .and(TicketTypePo::getScope).eq(TicketTypePo.SCOPE_PLATFORM));
    }

    default Long findFirstIdByDomainId(long domainId) {
        TicketTypePo po = selectOneByQuery(QueryWrapper.create()
                .from(TicketTypePo.class)
                .where(TicketTypePo::getScope).eq(TicketTypePo.SCOPE_DOMAIN)
                .and(TicketTypePo::getBusinessDomainId).eq(domainId)
                .orderBy(TicketTypePo::getId, true));
        return po == null ? null : po.getId();
    }

    default int countTicketsByTypeId(long domainId, long typeId) {
        return (int) selectCountByQuery(QueryWrapper.create()
                .from("ticket")
                .where(new QueryColumn("business_domain_id").eq(domainId))
                .and(new QueryColumn("ticket_type_id").eq(typeId)));
    }

    private static QueryWrapper platformScopeQuery(String keywordLike) {
        QueryWrapper qw = QueryWrapper.create()
                .from(TicketTypePo.class)
                .where(TicketTypePo::getScope).eq(TicketTypePo.SCOPE_PLATFORM);
        if (If.hasText(keywordLike)) {
            qw.and(qw2 -> {
                qw2.where(TicketTypePo::getName).like(keywordLike)
                        .or(TicketTypePo::getDescription).like(keywordLike)
                        .or(TicketTypePo::getCode).like(keywordLike);
            });
        }
        return qw;
    }
}
