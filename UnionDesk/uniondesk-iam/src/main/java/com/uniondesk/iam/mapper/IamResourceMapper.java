package com.uniondesk.iam.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.If;
import com.mybatisflex.core.query.QueryWrapper;
import com.uniondesk.iam.entity.ApiGrantPo;
import com.uniondesk.iam.entity.IamResourcePo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface IamResourceMapper extends BaseMapper<IamResourcePo> {

    default IamResourcePo selectById(long id) {
        return selectOneByQuery(QueryWrapper.create()
                .from(IamResourcePo.class)
                .where(IamResourcePo::getId).eq(id));
    }

    default IamResourcePo selectByCode(String resourceCode) {
        return selectOneByQuery(QueryWrapper.create()
                .from(IamResourcePo.class)
                .where(IamResourcePo::getResourceCode).eq(resourceCode));
    }

    default List<IamResourcePo> selectByFilters(String resourceType, String clientScope) {
        return selectListByQuery(QueryWrapper.create()
                .from(IamResourcePo.class)
                .where(IamResourcePo::getResourceType).eq(resourceType, If::notNull)
                .and(IamResourcePo::getClientScope).eq(clientScope, If::notNull)
                .orderBy(IamResourcePo::getResourceType, true)
                .orderBy(IamResourcePo::getOrderNo, true)
                .orderBy(IamResourcePo::getId, true));
    }

    int insertRow(IamResourcePo po);

    int updateRow(IamResourcePo po);

    int deleteRowById(@Param("id") long id);

    default int countByParentId(long parentId) {
        return (int) selectCountByQuery(QueryWrapper.create()
                .from(IamResourcePo.class)
                .where(IamResourcePo::getParentId).eq(parentId));
    }

    int countBindingsByResourceId(@Param("resourceId") long resourceId);

    List<IamResourcePo> selectMenuByRoleAndClient(@Param("roleCode") String roleCode, @Param("clientCode") String clientCode);

    List<IamResourcePo> selectActionByRoleAndClient(@Param("roleCode") String roleCode, @Param("clientCode") String clientCode);

    List<IamResourcePo> selectByRoleId(@Param("roleId") int roleId);

    void deleteRoleResources(@Param("roleId") int roleId);

    void insertRoleResource(@Param("roleId") int roleId, @Param("resourceId") long resourceId);

    List<IamResourcePo> selectMenuTree(@Param("clientScope") String clientScope);

    List<IamResourcePo> selectForRoles(@Param("roleCodes") List<String> roleCodes,
                                       @Param("resourceTypes") List<String> resourceTypes,
                                       @Param("clientCode") String clientCode);

    List<ApiGrantPo> selectActionGrants(@Param("roleCode") String roleCode, @Param("clientCode") String clientCode);

    default int countByIds(List<Long> ids) {
        return (int) selectCountByQuery(QueryWrapper.create()
                .from(IamResourcePo.class)
                .where(IamResourcePo::getId).in(ids));
    }

    default int countByIdsAndType(List<Long> ids, String resourceType) {
        return (int) selectCountByQuery(QueryWrapper.create()
                .from(IamResourcePo.class)
                .where(IamResourcePo::getId).in(ids)
                .and(IamResourcePo::getResourceType).eq(resourceType));
    }

    List<Long> selectMenuResourceIdsByRole(@Param("roleId") int roleId);

    List<Long> selectActionResourceIdsByRole(@Param("roleId") int roleId);
}
