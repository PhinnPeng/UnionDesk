package com.uniondesk.iam.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.uniondesk.iam.entity.DomainRolePo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DomainRoleMapper extends BaseMapper<DomainRolePo> {

    default Long selectIdByDomainAndCode(long businessDomainId, String code) {
        return selectObjectByQueryAs(QueryWrapper.create()
                .from(DomainRolePo.class)
                .select(DomainRolePo::getId)
                .where(DomainRolePo::getBusinessDomainId).eq(businessDomainId)
                .and(DomainRolePo::getCode).eq(code), Long.class);
    }
}
