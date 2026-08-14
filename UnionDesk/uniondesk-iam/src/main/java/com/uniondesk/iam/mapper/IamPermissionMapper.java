package com.uniondesk.iam.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.uniondesk.iam.entity.EffectivePermissionGrantPo;
import com.uniondesk.iam.entity.IamPermissionPo;
import com.uniondesk.iam.entity.RoutePermissionPo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface IamPermissionMapper extends BaseMapper<IamPermissionPo> {

    default List<RoutePermissionPo> selectRoutePermissions(String httpMethod) {
        return selectListByQueryAs(QueryWrapper.create()
                .from(IamPermissionPo.class)
                .select(IamPermissionPo::getCode, IamPermissionPo::getPathPattern)
                .where(IamPermissionPo::getStatus).eq(1)
                .and(IamPermissionPo::getHttpMethod).eq(httpMethod)
                .and(IamPermissionPo::getPathPattern).isNotNull(), RoutePermissionPo.class);
    }

    List<EffectivePermissionGrantPo> selectEffectiveGrants(@Param("userId") long userId, @Param("codes") List<String> codes);

    void deleteRolePermissionsByCatalog(@Param("roleId") int roleId, @Param("catalogCodes") List<String> catalogCodes);

    void insertRolePermissionsByCodes(@Param("roleId") int roleId, @Param("codes") List<String> codes);
}
