package com.uniondesk.ticket.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.If;
import com.mybatisflex.core.query.QueryMethods;
import com.mybatisflex.core.query.QueryWrapper;
import com.uniondesk.ticket.entity.TicketStatusPo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TicketStatusMapper extends BaseMapper<TicketStatusPo> {

    default List<TicketStatusPo> findByPlatform(String keywordLike) {
        return selectListByQuery(platformScopeQuery(keywordLike)
                .orderBy(TicketStatusPo::getSortOrder, true)
                .orderBy(TicketStatusPo::getId, true));
    }

    default Page<TicketStatusPo> selectPageByPlatform(Page<TicketStatusPo> page, String keywordLike) {
        return paginate(page, platformScopeQuery(keywordLike)
                .orderBy(TicketStatusPo::getSortOrder, true)
                .orderBy(TicketStatusPo::getId, true));
    }

    default TicketStatusPo findById(long id) {
        return selectOneByQuery(QueryWrapper.create()
                .from(TicketStatusPo.class)
                .where(TicketStatusPo::getId).eq(id));
    }

    default TicketStatusPo findPlatformByCode(String code) {
        return selectOneByQuery(QueryWrapper.create()
                .from(TicketStatusPo.class)
                .where(TicketStatusPo::getScope).eq(TicketStatusPo.SCOPE_PLATFORM)
                .and(TicketStatusPo::getCode).eq(code));
    }

    default TicketStatusPo findPlatformByName(String name) {
        return selectOneByQuery(QueryWrapper.create()
                .from(TicketStatusPo.class)
                .where(TicketStatusPo::getScope).eq(TicketStatusPo.SCOPE_PLATFORM)
                .and(TicketStatusPo::getName).eq(name));
    }

    default Integer findMaxSortOrderPlatform() {
        return selectObjectByQueryAs(QueryWrapper.create()
                .select(QueryMethods.max(TicketStatusPo::getSortOrder))
                .from(TicketStatusPo.class)
                .where(TicketStatusPo::getScope).eq(TicketStatusPo.SCOPE_PLATFORM), Integer.class);
    }

    default List<TicketStatusPo> findByDomain(long domainId, String keywordLike) {
        return selectListByQuery(domainScopeQuery(domainId, keywordLike)
                .orderBy(TicketStatusPo::getSortOrder, true)
                .orderBy(TicketStatusPo::getId, true));
    }

    default Page<TicketStatusPo> selectPageByDomain(long domainId, Page<TicketStatusPo> page, String keywordLike) {
        return paginate(page, domainScopeQuery(domainId, keywordLike)
                .orderBy(TicketStatusPo::getSortOrder, true)
                .orderBy(TicketStatusPo::getId, true));
    }

    default TicketStatusPo findDomainByCode(long domainId, String code) {
        return selectOneByQuery(QueryWrapper.create()
                .from(TicketStatusPo.class)
                .where(TicketStatusPo::getScope).eq(TicketStatusPo.SCOPE_DOMAIN)
                .and(TicketStatusPo::getBusinessDomainId).eq(domainId)
                .and(TicketStatusPo::getCode).eq(code));
    }

    default TicketStatusPo findDomainByName(long domainId, String name) {
        return selectOneByQuery(QueryWrapper.create()
                .from(TicketStatusPo.class)
                .where(TicketStatusPo::getScope).eq(TicketStatusPo.SCOPE_DOMAIN)
                .and(TicketStatusPo::getBusinessDomainId).eq(domainId)
                .and(TicketStatusPo::getName).eq(name));
    }

    default TicketStatusPo findDomainBySourceStatusId(long domainId, long sourceStatusId) {
        return selectOneByQuery(QueryWrapper.create()
                .from(TicketStatusPo.class)
                .where(TicketStatusPo::getScope).eq(TicketStatusPo.SCOPE_DOMAIN)
                .and(TicketStatusPo::getBusinessDomainId).eq(domainId)
                .and(TicketStatusPo::getSourceStatusId).eq(sourceStatusId));
    }

    default Integer findMaxSortOrderDomain(long domainId) {
        return selectObjectByQueryAs(QueryWrapper.create()
                .select(QueryMethods.max(TicketStatusPo::getSortOrder))
                .from(TicketStatusPo.class)
                .where(TicketStatusPo::getScope).eq(TicketStatusPo.SCOPE_DOMAIN)
                .and(TicketStatusPo::getBusinessDomainId).eq(domainId), Integer.class);
    }

    @Override
    default int update(TicketStatusPo po) {
        TicketStatusPo set = new TicketStatusPo();
        set.setName(po.getName());
        set.setDescription(po.getDescription());
        set.setCategory(po.getCategory());
        set.setStateType(po.getStateType());
        set.setConfigJson(po.getConfigJson());
        set.setStatus(po.getStatus());
        set.setUpdatedBy(po.getUpdatedBy());
        return updateByQuery(set, QueryWrapper.create()
                .from(TicketStatusPo.class)
                .where(TicketStatusPo::getId).eq(po.getId()));
    }

    default int deleteByIdPlatform(long id) {
        return deleteByQuery(QueryWrapper.create()
                .from(TicketStatusPo.class)
                .where(TicketStatusPo::getId).eq(id)
                .and(TicketStatusPo::getScope).eq(TicketStatusPo.SCOPE_PLATFORM)
                .and(TicketStatusPo::isSystem).eq(false));
    }

    default int deleteByIdDomain(long domainId, long id) {
        return deleteByQuery(QueryWrapper.create()
                .from(TicketStatusPo.class)
                .where(TicketStatusPo::getId).eq(id)
                .and(TicketStatusPo::getScope).eq(TicketStatusPo.SCOPE_DOMAIN)
                .and(TicketStatusPo::getBusinessDomainId).eq(domainId)
                .and(TicketStatusPo::isSystem).eq(false));
    }

    private static QueryWrapper platformScopeQuery(String keywordLike) {
        QueryWrapper qw = QueryWrapper.create()
                .from(TicketStatusPo.class)
                .where(TicketStatusPo::getScope).eq(TicketStatusPo.SCOPE_PLATFORM);
        if (If.hasText(keywordLike)) {
            qw.and(qw2 -> {
                qw2.where(TicketStatusPo::getName).like(keywordLike)
                        .or(TicketStatusPo::getDescription).like(keywordLike)
                        .or(TicketStatusPo::getCode).like(keywordLike);
            });
        }
        return qw;
    }

    private static QueryWrapper domainScopeQuery(long domainId, String keywordLike) {
        QueryWrapper qw = QueryWrapper.create()
                .from(TicketStatusPo.class)
                .where(TicketStatusPo::getScope).eq(TicketStatusPo.SCOPE_DOMAIN)
                .and(TicketStatusPo::getBusinessDomainId).eq(domainId);
        if (If.hasText(keywordLike)) {
            qw.and(qw2 -> {
                qw2.where(TicketStatusPo::getName).like(keywordLike)
                        .or(TicketStatusPo::getDescription).like(keywordLike)
                        .or(TicketStatusPo::getCode).like(keywordLike);
            });
        }
        return qw;
    }
}
