package com.uniondesk.ticket.mapper;

import com.uniondesk.ticket.entity.IdentitySubjectPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface IdentitySubjectMapper {

    IdentitySubjectPo findById(@Param("id") long id);

    Long findIdByPhone(@Param("phone") String phone);

    void insert(IdentitySubjectPo po);
}
