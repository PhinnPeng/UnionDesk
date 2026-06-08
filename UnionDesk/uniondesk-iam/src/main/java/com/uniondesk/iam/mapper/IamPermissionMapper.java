package com.uniondesk.iam.mapper;

import com.uniondesk.iam.entity.EffectivePermissionGrantPo;
import com.uniondesk.iam.entity.IamPermissionPo;
import com.uniondesk.iam.entity.RoutePermissionPo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface IamPermissionMapper {

    List<RoutePermissionPo> selectRoutePermissions(@Param("httpMethod") String httpMethod);

    List<EffectivePermissionGrantPo> selectEffectiveGrants(@Param("userId") long userId, @Param("codes") List<String> codes);

    void deleteRolePermissionsByCatalog(@Param("roleId") int roleId, @Param("catalogCodes") List<String> catalogCodes);

    void insertRolePermissionsByCodes(@Param("roleId") int roleId, @Param("codes") List<String> codes);
}
