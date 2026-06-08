package com.uniondesk.iam.mapper;

import com.uniondesk.iam.entity.CustomerAccountPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CustomerAccountMapper {

    CustomerAccountPo selectById(@Param("id") long id);

    int countByUsername(@Param("username") String username);

    void insert(CustomerAccountPo po);

    Long selectIdByUsernameOrPhone(@Param("username") String username, @Param("phone") String phone);
}
