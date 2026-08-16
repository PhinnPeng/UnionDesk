package com.uniondesk.sla.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import com.uniondesk.sla.entity.SlaConfigPo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SlaConfigMapper extends BaseMapper<SlaConfigPo> {

    default SlaConfigPo selectByDomainId(long domainId) {
        return selectOneByQuery(QueryWrapper.create()
                .from(SlaConfigPo.class)
                .where(SlaConfigPo::getBusinessDomainId).eq(domainId));
    }

    /**
     * 按 business_domain_id 更新（不含 id），返回受影响行数。
     * 显式写入 null（set 第三参 false），保证「空时限 = 不启用」可落库清空。
     */
    default int updateByDomainId(SlaConfigPo po) {
        return UpdateChain.of(SlaConfigPo.class)
                .set(SlaConfigPo::getFirstResponseMinutes, po.getFirstResponseMinutes(), false)
                .set(SlaConfigPo::getResolutionMinutes, po.getResolutionMinutes(), false)
                .set(SlaConfigPo::getBreachActionJson, po.getBreachActionJson(), false)
                .set(SlaConfigPo::getCalendarJson, po.getCalendarJson(), false)
                .set(SlaConfigPo::getUrgentFirstResponseMinutes, po.getUrgentFirstResponseMinutes(), false)
                .set(SlaConfigPo::getUrgentResolutionMinutes, po.getUrgentResolutionMinutes(), false)
                .where(SlaConfigPo::getBusinessDomainId).eq(po.getBusinessDomainId())
                .update() ? 1 : 0;
    }
}
