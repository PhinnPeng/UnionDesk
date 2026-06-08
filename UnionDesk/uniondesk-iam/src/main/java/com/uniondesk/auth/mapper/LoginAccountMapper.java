package com.uniondesk.auth.mapper;

import com.uniondesk.auth.entity.LoginAccountPo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LoginAccountMapper {

    LoginAccountPo selectStaffByIdentifier(@Param("column") String column, @Param("identifier") String identifier);

    LoginAccountPo selectCustomerByIdentifier(@Param("column") String column, @Param("identifier") String identifier);

    LoginAccountPo selectStaffById(@Param("id") long id);

    LoginAccountPo selectCustomerById(@Param("id") long id);

    List<String> selectStaffDomainRoleCodes(@Param("userId") long userId);

    List<String> selectStaffPlatformRoleCodes(@Param("userId") long userId);

    List<Long> selectCustomerAccessibleDomainIds(@Param("userId") long userId);

    List<Long> selectAllDomainIds();

    List<Long> selectStaffAccessibleDomainIds(@Param("userId") long userId);

    int updateStaffPassword(@Param("id") long id, @Param("passwordHash") String passwordHash);

    int updateCustomerPassword(@Param("id") long id, @Param("passwordHash") String passwordHash);
}
