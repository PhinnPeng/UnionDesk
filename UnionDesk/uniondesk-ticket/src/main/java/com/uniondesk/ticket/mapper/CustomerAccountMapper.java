package com.uniondesk.ticket.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CustomerAccountMapper {

    Long findSubjectIdById(@Param("id") long id);
}
