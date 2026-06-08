package com.uniondesk.ticket.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface StaffAccountMapper {

    Long findSubjectIdById(@Param("id") long id);
}
