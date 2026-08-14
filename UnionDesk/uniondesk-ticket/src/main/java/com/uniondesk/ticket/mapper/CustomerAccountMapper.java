package com.uniondesk.ticket.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.uniondesk.ticket.entity.CustomerAccountPo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CustomerAccountMapper extends BaseMapper<CustomerAccountPo> {

    default Long findSubjectIdById(long id) {
        CustomerAccountPo po = selectOneByQuery(QueryWrapper.create()
                .from(CustomerAccountPo.class)
                .where(CustomerAccountPo::getId).eq(id));
        return po == null ? null : po.getSubjectId();
    }
}
