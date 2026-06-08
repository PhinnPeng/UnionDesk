package com.uniondesk.ticket.mapper;

import com.uniondesk.ticket.entity.UserAccountPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserAccountMapper {

    UserAccountPo findById(@Param("id") long id);
}
