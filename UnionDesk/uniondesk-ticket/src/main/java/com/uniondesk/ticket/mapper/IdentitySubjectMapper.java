package com.uniondesk.ticket.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.uniondesk.ticket.entity.IdentitySubjectPo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IdentitySubjectMapper extends BaseMapper<IdentitySubjectPo> {

    default IdentitySubjectPo findById(long id) {
        return selectOneByQuery(QueryWrapper.create()
                .from(IdentitySubjectPo.class)
                .where(IdentitySubjectPo::getId).eq(id));
    }

    default Long findIdByPhone(String phone) {
        IdentitySubjectPo po = selectOneByQuery(QueryWrapper.create()
                .from(IdentitySubjectPo.class)
                .where(IdentitySubjectPo::getPhone).eq(phone));
        return po == null ? null : po.getId();
    }
}
