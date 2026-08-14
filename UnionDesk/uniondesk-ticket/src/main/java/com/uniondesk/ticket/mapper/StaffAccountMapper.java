package com.uniondesk.ticket.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.uniondesk.ticket.entity.StaffAccountPo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StaffAccountMapper extends BaseMapper<StaffAccountPo> {

    default Long findSubjectIdById(long id) {
        StaffAccountPo po = selectOneByQuery(QueryWrapper.create()
                .from(StaffAccountPo.class)
                .where(StaffAccountPo::getId).eq(id));
        return po == null ? null : po.getSubjectId();
    }
}
