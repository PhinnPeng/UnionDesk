package com.uniondesk.domain.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.uniondesk.domain.entity.DomainRolePo;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface DomainRoleMapper extends BaseMapper<DomainRolePo> {

    default List<DomainRolePo> selectByDomainId(long domainId) {
        return selectListByQuery(QueryWrapper.create()
                .from(DomainRolePo.class)
                .where(DomainRolePo::getBusinessDomainId).eq(domainId)
                .orderBy(DomainRolePo::getPreset, false)
                .orderBy(DomainRolePo::getId, true));
    }

    default DomainRolePo selectByIdAndDomain(long id, long domainId) {
        return selectOneByQuery(QueryWrapper.create()
                .from(DomainRolePo.class)
                .where(DomainRolePo::getId).eq(id)
                .and(DomainRolePo::getBusinessDomainId).eq(domainId));
    }

    default Long selectIdByDomainAndCode(long domainId, String code) {
        DomainRolePo po = selectOneByQuery(QueryWrapper.create()
                .from(DomainRolePo.class)
                .select(DomainRolePo::getId)
                .where(DomainRolePo::getBusinessDomainId).eq(domainId)
                .and(DomainRolePo::getCode).eq(code)
                .orderBy(DomainRolePo::getId, false));
        return po == null ? null : po.getId();
    }

    default void insert(long domainId, String code, String name, int preset) {
        DomainRolePo po = new DomainRolePo();
        po.setBusinessDomainId(domainId);
        po.setCode(code);
        po.setName(name);
        po.setPreset(preset);
        insert(po);
    }

    @Insert("INSERT INTO domain_role ("
            + " business_domain_id, code, name, preset, created_at, updated_at"
            + ")"
            + " SELECT #{domainId}, #{code}, #{name}, 1, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)"
            + " FROM DUAL"
            + " WHERE NOT EXISTS ("
            + "  SELECT 1 FROM domain_role"
            + "  WHERE business_domain_id = #{domainId} AND code = #{code}"
            + " )")
    void insertIfNotExists(
            @Param("domainId") long domainId,
            @Param("code") String code,
            @Param("name") String name);

    default int countCustomRoles(long domainId) {
        return (int) selectCountByQuery(QueryWrapper.create()
                .from(DomainRolePo.class)
                .where(DomainRolePo::getBusinessDomainId).eq(domainId)
                .and(DomainRolePo::getPreset).eq(0));
    }

    default void insertTemplateInstance(
            long domainId,
            String code,
            String name,
            long templateId,
            int templateVersion,
            String lockedFields) {
        DomainRolePo po = new DomainRolePo();
        po.setBusinessDomainId(domainId);
        po.setCode(code);
        po.setName(name);
        po.setPreset(0);
        po.setTemplateId(templateId);
        po.setTemplateVersion(templateVersion);
        po.setLockedFields(lockedFields);
        insert(po);
    }

    default void updateTemplateBinding(
            long roleId, long domainId, int templateVersion, String lockedFields) {
        DomainRolePo set = new DomainRolePo();
        set.setTemplateVersion(templateVersion);
        set.setLockedFields(lockedFields);
        updateByQuery(set, QueryWrapper.create()
                .where(DomainRolePo::getId).eq(roleId)
                .and(DomainRolePo::getBusinessDomainId).eq(domainId));
    }

    @Update("UPDATE domain_role"
            + " SET template_id = NULL, template_version = NULL, locked_fields = NULL,"
            + " updated_at = CURRENT_TIMESTAMP(3)"
            + " WHERE id = #{roleId} AND business_domain_id = #{domainId}")
    void clearTemplateBinding(@Param("roleId") long roleId, @Param("domainId") long domainId);

    default int update(String code, String name, long id, long domainId) {
        DomainRolePo set = new DomainRolePo();
        set.setCode(code);
        set.setName(name);
        return updateByQuery(set, QueryWrapper.create()
                .where(DomainRolePo::getId).eq(id)
                .and(DomainRolePo::getBusinessDomainId).eq(domainId));
    }

    @Delete("DELETE FROM domain_role_permission WHERE domain_role_id = #{roleId}")
    void deleteRolePermissions(@Param("roleId") long roleId);

    @Insert("INSERT INTO domain_role_permission (domain_role_id, permission_item_id, created_at)"
            + " VALUES (#{roleId}, #{permissionItemId}, CURRENT_TIMESTAMP(3))")
    void insertRolePermission(@Param("roleId") long roleId, @Param("permissionItemId") long permissionItemId);

    @Select("SELECT COUNT(*)"
            + " FROM domain_member_role dmr"
            + " JOIN domain_member dm ON dm.id = dmr.domain_member_id"
            + " WHERE dmr.domain_role_id = #{roleId}"
            + " AND dm.business_domain_id = #{domainId}"
            + " AND dm.deleted_at IS NULL")
    int countRoleMembers(@Param("roleId") long roleId, @Param("domainId") long domainId);

    default void deleteByIdAndDomain(long id, long domainId) {
        deleteByQuery(QueryWrapper.create()
                .from(DomainRolePo.class)
                .where(DomainRolePo::getId).eq(id)
                .and(DomainRolePo::getBusinessDomainId).eq(domainId));
    }

    @Insert("INSERT INTO domain_role_permission (domain_role_id, permission_item_id, created_at)"
            + " SELECT #{superAdminRoleId}, pi.id, CURRENT_TIMESTAMP(3)"
            + " FROM permission_item pi"
            + " WHERE NOT EXISTS ("
            + "  SELECT 1 FROM domain_role_permission drp"
            + "  WHERE drp.domain_role_id = #{superAdminRoleId} AND drp.permission_item_id = pi.id"
            + " )")
    void seedSuperAdminAllPermissions(@Param("superAdminRoleId") long superAdminRoleId);
}
