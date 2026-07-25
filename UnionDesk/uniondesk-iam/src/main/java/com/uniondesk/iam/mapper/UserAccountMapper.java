package com.uniondesk.iam.mapper;

import com.uniondesk.iam.entity.UserSummaryPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserAccountMapper {

    UserSummaryPo selectSummaryById(@Param("id") long id);
}
