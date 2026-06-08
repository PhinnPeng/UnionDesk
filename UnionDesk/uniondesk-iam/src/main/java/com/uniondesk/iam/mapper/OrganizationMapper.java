package com.uniondesk.iam.mapper;

import com.uniondesk.iam.entity.OrganizationPo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OrganizationMapper {

    List<OrganizationPo> selectAll();

    OrganizationPo selectById(@Param("id") long id);

    OrganizationPo selectByCode(@Param("code") String code);

    void insert(OrganizationPo po);

    int update(OrganizationPo po);

    int deleteById(@Param("id") long id);

    int countById(@Param("id") long id);

    int countByParentId(@Param("parentId") long parentId);

    Long selectParentId(@Param("id") long id);

    List<Long> selectChildIds(@Param("parentId") long parentId);

    List<Long> selectUserOrganizationIds(@Param("userId") long userId);

    void deleteUserOrganizations(@Param("userId") long userId);

    void insertUserOrganization(@Param("userId") long userId, @Param("organizationId") long organizationId);

    int countUserAccountById(@Param("id") long id);
}
