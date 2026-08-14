package com.uniondesk.ticket.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.If;
import com.mybatisflex.core.query.QueryMethods;
import com.mybatisflex.core.query.QueryWrapper;
import com.uniondesk.ticket.entity.TicketAttributePo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TicketAttributeMapper extends BaseMapper<TicketAttributePo> {

    default List<TicketAttributePo> findByPlatform(String keywordLike) {
        return selectListByQuery(platformScopeQuery(keywordLike)
                .orderBy(TicketAttributePo::getSortOrder, true)
                .orderBy(TicketAttributePo::getId, true));
    }

    default List<TicketAttributePo> findByDomain(long domainId, String keywordLike) {
        return selectListByQuery(domainScopeQuery(domainId, keywordLike)
                .orderBy(TicketAttributePo::getSortOrder, true)
                .orderBy(TicketAttributePo::getId, true));
    }

    default Page<TicketAttributePo> selectPageByPlatform(Page<TicketAttributePo> page, String keywordLike) {
        return paginate(page, platformScopeQuery(keywordLike)
                .orderBy(TicketAttributePo::getSortOrder, true)
                .orderBy(TicketAttributePo::getId, true));
    }

    default Page<TicketAttributePo> selectPageByDomain(long domainId, Page<TicketAttributePo> page, String keywordLike) {
        return paginate(page, domainScopeQuery(domainId, keywordLike)
                .orderBy(TicketAttributePo::getSortOrder, true)
                .orderBy(TicketAttributePo::getId, true));
    }

    default TicketAttributePo findById(long id) {
        return selectOneByQuery(QueryWrapper.create()
                .from(TicketAttributePo.class)
                .where(TicketAttributePo::getId).eq(id));
    }

    default TicketAttributePo findPlatformByName(String name) {
        return selectOneByQuery(QueryWrapper.create()
                .from(TicketAttributePo.class)
                .where(TicketAttributePo::getScope).eq(TicketAttributePo.SCOPE_PLATFORM)
                .and(TicketAttributePo::getName).eq(name));
    }

    default TicketAttributePo findPlatformBySystemKey(String systemKey) {
        return selectOneByQuery(QueryWrapper.create()
                .from(TicketAttributePo.class)
                .where(TicketAttributePo::getScope).eq(TicketAttributePo.SCOPE_PLATFORM)
                .and(TicketAttributePo::getBusinessDomainId).isNull()
                .and(TicketAttributePo::getSystemKey).eq(systemKey));
    }

    default TicketAttributePo findDomainByName(long domainId, String name) {
        return selectOneByQuery(QueryWrapper.create()
                .from(TicketAttributePo.class)
                .where(TicketAttributePo::getScope).eq(TicketAttributePo.SCOPE_DOMAIN)
                .and(TicketAttributePo::getBusinessDomainId).eq(domainId)
                .and(TicketAttributePo::getName).eq(name));
    }

    default TicketAttributePo findDomainBySystemKey(long domainId, String systemKey) {
        return selectOneByQuery(QueryWrapper.create()
                .from(TicketAttributePo.class)
                .where(TicketAttributePo::getScope).eq(TicketAttributePo.SCOPE_DOMAIN)
                .and(TicketAttributePo::getBusinessDomainId).eq(domainId)
                .and(TicketAttributePo::getSystemKey).eq(systemKey));
    }

    default TicketAttributePo findDomainBySourceAttributeId(long domainId, long sourceAttributeId) {
        return selectOneByQuery(QueryWrapper.create()
                .from(TicketAttributePo.class)
                .where(TicketAttributePo::getScope).eq(TicketAttributePo.SCOPE_DOMAIN)
                .and(TicketAttributePo::getBusinessDomainId).eq(domainId)
                .and(TicketAttributePo::getSourceAttributeId).eq(sourceAttributeId));
    }

    default Integer findMaxSortOrderPlatform() {
        return selectObjectByQueryAs(QueryWrapper.create()
                .select(QueryMethods.max(TicketAttributePo::getSortOrder))
                .from(TicketAttributePo.class)
                .where(TicketAttributePo::getScope).eq(TicketAttributePo.SCOPE_PLATFORM), Integer.class);
    }

    default Integer findMaxSortOrderDomain(long domainId) {
        return selectObjectByQueryAs(QueryWrapper.create()
                .select(QueryMethods.max(TicketAttributePo::getSortOrder))
                .from(TicketAttributePo.class)
                .where(TicketAttributePo::getScope).eq(TicketAttributePo.SCOPE_DOMAIN)
                .and(TicketAttributePo::getBusinessDomainId).eq(domainId), Integer.class);
    }

    @Override
    default int update(TicketAttributePo po) {
        TicketAttributePo set = new TicketAttributePo();
        set.setName(po.getName());
        set.setDescription(po.getDescription());
        set.setFieldType(po.getFieldType());
        set.setTypeConfig(po.getTypeConfig());
        set.setStatus(po.getStatus());
        set.setUpdatedBy(po.getUpdatedBy());
        return updateByQuery(set, QueryWrapper.create()
                .from(TicketAttributePo.class)
                .where(TicketAttributePo::getId).eq(po.getId()));
    }

    default int deleteByIdPlatform(long id) {
        return deleteByQuery(QueryWrapper.create()
                .from(TicketAttributePo.class)
                .where(TicketAttributePo::getId).eq(id)
                .and(TicketAttributePo::getScope).eq(TicketAttributePo.SCOPE_PLATFORM));
    }

    default int deleteByIdDomain(long id, long domainId) {
        return deleteByQuery(QueryWrapper.create()
                .from(TicketAttributePo.class)
                .where(TicketAttributePo::getId).eq(id)
                .and(TicketAttributePo::getScope).eq(TicketAttributePo.SCOPE_DOMAIN)
                .and(TicketAttributePo::getBusinessDomainId).eq(domainId));
    }

    default int updateSortOrder(long id, int sortOrder, Long updatedBy) {
        TicketAttributePo set = new TicketAttributePo();
        set.setSortOrder(sortOrder);
        set.setUpdatedBy(updatedBy);
        return updateByQuery(set, QueryWrapper.create()
                .from(TicketAttributePo.class)
                .where(TicketAttributePo::getId).eq(id));
    }

    private static QueryWrapper platformScopeQuery(String keywordLike) {
        QueryWrapper qw = QueryWrapper.create()
                .from(TicketAttributePo.class)
                .where(TicketAttributePo::getScope).eq(TicketAttributePo.SCOPE_PLATFORM);
        if (If.hasText(keywordLike)) {
            qw.and(qw2 -> {
                qw2.where(TicketAttributePo::getName).like(keywordLike)
                        .or(TicketAttributePo::getDescription).like(keywordLike);
            });
        }
        return qw;
    }

    private static QueryWrapper domainScopeQuery(long domainId, String keywordLike) {
        QueryWrapper qw = QueryWrapper.create()
                .from(TicketAttributePo.class)
                .where(TicketAttributePo::getScope).eq(TicketAttributePo.SCOPE_DOMAIN)
                .and(TicketAttributePo::getBusinessDomainId).eq(domainId);
        if (If.hasText(keywordLike)) {
            qw.and(qw2 -> {
                qw2.where(TicketAttributePo::getName).like(keywordLike)
                        .or(TicketAttributePo::getDescription).like(keywordLike);
            });
        }
        return qw;
    }
}
