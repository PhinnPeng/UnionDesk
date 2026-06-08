package com.uniondesk.iam.mapper;

import com.uniondesk.iam.entity.IdentitySubjectPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface IdentitySubjectMapper {

    Long selectIdByPhone(@Param("phone") String phone);

    IdentitySubjectPo selectById(@Param("id") long id);

    Long selectMergedIntoId(@Param("id") long id);

    void insert(IdentitySubjectPo po);
}
