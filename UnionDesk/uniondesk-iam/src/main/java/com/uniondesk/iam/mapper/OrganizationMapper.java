package com.uniondesk.iam.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.uniondesk.iam.entity.OrganizationPo;
import com.uniondesk.iam.entity.StaffAccountPo;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OrganizationMapper extends BaseMapper<OrganizationPo> {

    List<OrganizationPo> selectAllRows();

    OrganizationPo selectById(@Param("id") long id);

    OrganizationPo selectByCode(@Param("code") String code);

    int insertRow(OrganizationPo po);

    int updateRow(OrganizationPo po);

    int deleteRowById(@Param("id") long id);

    default int countById(long id) {
        return (int) selectCountByQuery(QueryWrapper.create()
                .from(OrganizationPo.class)
                .where(OrganizationPo::getId).eq(id));
    }

    default int countByParentId(long parentId) {
        return (int) selectCountByQuery(QueryWrapper.create()
                .from(OrganizationPo.class)
                .where(OrganizationPo::getParentId).eq(parentId));
    }

    default Long selectParentId(long id) {
        return selectObjectByQueryAs(QueryWrapper.create()
                .from(OrganizationPo.class)
                .select(OrganizationPo::getParentId)
                .where(OrganizationPo::getId).eq(id), Long.class);
    }

    default List<Long> selectChildIds(long parentId) {
        return selectObjectListByQueryAs(QueryWrapper.create()
                .from(OrganizationPo.class)
                .select(OrganizationPo::getId)
                .where(OrganizationPo::getParentId).eq(parentId), Long.class);
    }

    List<Long> selectUserOrganizationIds(@Param("userId") long userId);

    void deleteUserOrganizations(@Param("userId") long userId);

    void insertUserOrganization(@Param("userId") long userId, @Param("organizationId") long organizationId);

    default int countUserAccountById(long id) {
        return (int) selectCountByQuery(QueryWrapper.create()
                .from(StaffAccountPo.class)
                .where(StaffAccountPo::getId).eq(id));
    }
}
