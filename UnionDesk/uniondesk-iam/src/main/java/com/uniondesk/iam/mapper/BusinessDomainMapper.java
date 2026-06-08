package com.uniondesk.iam.mapper;

import com.uniondesk.iam.entity.BusinessDomainPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BusinessDomainMapper {

    int countActiveById(@Param("id") long id);

    int countByIds(@Param("ids") List<Long> ids);

    List<BusinessDomainPo> selectAll();
}
