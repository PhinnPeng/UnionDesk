package com.uniondesk.domain.repository;

import com.uniondesk.domain.entity.DomainRolePo;
import com.uniondesk.domain.entity.PermissionItemPo;
import com.uniondesk.domain.mapper.DomainRoleMapper;
import com.uniondesk.domain.mapper.PermissionItemMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class DomainRoleRepository {

    private final DomainRoleMapper domainRoleMapper;
    private final PermissionItemMapper permissionItemMapper;

    public DomainRoleRepository(
            DomainRoleMapper domainRoleMapper,
            PermissionItemMapper permissionItemMapper) {
        this.domainRoleMapper = domainRoleMapper;
        this.permissionItemMapper = permissionItemMapper;
    }

    public List<DomainRolePo> findRolesByDomainId(long domainId) {
        return domainRoleMapper.selectByDomainId(domainId);
    }

    public DomainRolePo findRoleByIdAndDomain(long id, long domainId) {
        return domainRoleMapper.selectByIdAndDomain(id, domainId);
    }

    public Long findRoleIdByDomainAndCode(long domainId, String code) {
        return domainRoleMapper.selectIdByDomainAndCode(domainId, code);
    }

    public void insertRole(long domainId, String code, String name, int preset) {
        domainRoleMapper.insert(domainId, code, name, preset);
    }

    public int updateRole(String code, String name, long id, long domainId) {
        return domainRoleMapper.update(code, name, id, domainId);
    }

    public void deleteRolePermissions(long roleId) {
        domainRoleMapper.deleteRolePermissions(roleId);
    }

    public void insertRolePermission(long roleId, long permissionItemId) {
        domainRoleMapper.insertRolePermission(roleId, permissionItemId);
    }

    public int countRoleMembers(long roleId, long domainId) {
        return domainRoleMapper.countRoleMembers(roleId, domainId);
    }

    public void deleteRoleByIdAndDomain(long id, long domainId) {
        domainRoleMapper.deleteByIdAndDomain(id, domainId);
    }

    public List<PermissionItemPo> findAllPermissionItems() {
        return permissionItemMapper.selectAll();
    }

    public List<PermissionItemPo> findPermissionItemsByRoleId(long roleId) {
        return permissionItemMapper.selectByRoleId(roleId);
    }

    public long countPermissionItemsByIds(List<Long> ids) {
        return permissionItemMapper.countByIds(ids);
    }
}
