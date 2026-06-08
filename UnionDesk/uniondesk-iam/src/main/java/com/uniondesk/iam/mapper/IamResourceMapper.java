package com.uniondesk.iam.mapper;

import com.uniondesk.iam.entity.IamResourcePo;
import com.uniondesk.iam.entity.ApiGrantPo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface IamResourceMapper {

    IamResourcePo selectById(@Param("id") long id);

    IamResourcePo selectByCode(@Param("resourceCode") String resourceCode);

    List<IamResourcePo> selectByFilters(@Param("resourceType") String resourceType, @Param("clientScope") String clientScope);

    void insert(IamResourcePo po);

    int update(IamResourcePo po);

    int deleteById(@Param("id") long id);

    int countByParentId(@Param("parentId") long parentId);

    int countBindingsByResourceId(@Param("resourceId") long resourceId);

    List<IamResourcePo> selectMenuByRoleAndClient(@Param("roleCode") String roleCode, @Param("clientCode") String clientCode);

    List<IamResourcePo> selectActionByRoleAndClient(@Param("roleCode") String roleCode, @Param("clientCode") String clientCode);

    List<IamResourcePo> selectByRoleId(@Param("roleId") int roleId);

    void deleteRoleResources(@Param("roleId") int roleId);

    void insertRoleResource(@Param("roleId") int roleId, @Param("resourceId") long resourceId);

    List<IamResourcePo> selectMenuTree(@Param("clientScope") String clientScope);

    List<IamResourcePo> selectForRoles(@Param("roleCodes") List<String> roleCodes, @Param("resourceTypes") List<String> resourceTypes, @Param("clientCode") String clientCode);

    List<ApiGrantPo> selectActionGrants(@Param("roleCode") String roleCode, @Param("clientCode") String clientCode);

    int countByIds(@Param("ids") List<Long> ids);

    int countByIdsAndType(@Param("ids") List<Long> ids, @Param("resourceType") String resourceType);

    List<Long> selectMenuResourceIdsByRole(@Param("roleId") int roleId);

    List<Long> selectActionResourceIdsByRole(@Param("roleId") int roleId);
}
