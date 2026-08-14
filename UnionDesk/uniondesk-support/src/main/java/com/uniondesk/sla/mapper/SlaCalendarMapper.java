package com.uniondesk.sla.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.uniondesk.sla.entity.SlaCalendarPo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SlaCalendarMapper extends BaseMapper<SlaCalendarPo> {

    default Page<SlaCalendarPo> selectPageByDomainId(Page<SlaCalendarPo> page, long domainId) {
        return paginate(page, QueryWrapper.create()
                .from(SlaCalendarPo.class)
                .where(SlaCalendarPo::getBusinessDomainId).eq(domainId)
                .orderBy(SlaCalendarPo::getId, false));
    }

    default SlaCalendarPo selectByIdAndDomainId(long id, long domainId) {
        return selectOneByQuery(QueryWrapper.create()
                .from(SlaCalendarPo.class)
                .where(SlaCalendarPo::getId).eq(id)
                .and(SlaCalendarPo::getBusinessDomainId).eq(domainId));
    }

    default int updateByIdAndDomainId(SlaCalendarPo po) {
        return updateByQuery(po, QueryWrapper.create()
                .from(SlaCalendarPo.class)
                .where(SlaCalendarPo::getId).eq(po.getId())
                .and(SlaCalendarPo::getBusinessDomainId).eq(po.getBusinessDomainId()));
    }

    default int deleteByIdAndDomainId(long id, long domainId) {
        return deleteByQuery(QueryWrapper.create()
                .from(SlaCalendarPo.class)
                .where(SlaCalendarPo::getId).eq(id)
                .and(SlaCalendarPo::getBusinessDomainId).eq(domainId));
    }
}
