package com.uniondesk.domain.core;

import com.uniondesk.domain.entity.DomainRolePo;
import com.uniondesk.domain.entity.PermissionItemPo;
import com.uniondesk.domain.repository.DomainRoleRepository;
import com.uniondesk.domain.web.DomainRoleDtos;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class DomainRoleService {

    private final DomainRoleRepository domainRoleRepository;
    private final DomainService domainService;

    public DomainRoleService(DomainRoleRepository domainRoleRepository, DomainService domainService) {
        this.domainRoleRepository = domainRoleRepository;
        this.domainService = domainService;
    }

    public List<DomainRoleDtos.DomainRoleView> listRoles(long domainId) {
        requireDomain(domainId);
        return domainRoleRepository.findRolesByDomainId(domainId).stream()
                .map(this::toRoleView)
                .toList();
    }

    @Transactional
    public DomainRoleDtos.DomainRoleView createRole(long domainId, DomainRoleDtos.CreateDomainRoleRequest request) {
        requireDomain(domainId);
        domainRoleRepository.insertRole(domainId, request.code().trim(), request.name().trim(), 0);
        Long roleId = domainRoleRepository.findRoleIdByDomainAndCode(domainId, request.code().trim());
        if (roleId == null) {
            throw new IllegalStateException("domain role create failed");
        }
        return loadRole(domainId, roleId);
    }

    @Transactional
    public DomainRoleDtos.DomainRoleView updateRole(long domainId, long roleId, DomainRoleDtos.UpdateDomainRoleRequest request) {
        DomainRoleDtos.DomainRoleView existing = loadRole(domainId, roleId);
        String code = StringUtils.hasText(request.code()) ? request.code().trim() : existing.code();
        String name = StringUtils.hasText(request.name()) ? request.name().trim() : existing.name();
        domainRoleRepository.updateRole(code, name, roleId, domainId);
        return loadRole(domainId, roleId);
    }

    public DomainRoleDtos.DomainRolePermissionView getRolePermissions(long domainId, long roleId) {
        DomainRoleDtos.DomainRoleView role = loadRole(domainId, roleId);
        return new DomainRoleDtos.DomainRolePermissionView(
                role.id(),
                role.code(),
                role.name(),
                loadPermissionItems(roleId));
    }

    @Transactional
    public DomainRoleDtos.DomainRolePermissionView updateRolePermissions(
            long domainId,
            long roleId,
            DomainRoleDtos.UpdateDomainRolePermissionRequest request) {
        loadRole(domainId, roleId);
        List<Long> permissionItemIds = normalizeIds(request.permission_item_ids());
        if (!permissionItemIds.isEmpty()) {
            ensurePermissionItemsExist(permissionItemIds);
        }
        domainRoleRepository.deleteRolePermissions(roleId);
        for (Long permissionItemId : permissionItemIds) {
            domainRoleRepository.insertRolePermission(roleId, permissionItemId);
        }
        return getRolePermissions(domainId, roleId);
    }

    @Transactional
    public void deleteRole(long domainId, long roleId) {
        DomainRoleDtos.DomainRoleView role = loadRole(domainId, roleId);
        if (role.preset()) {
            throw new IllegalArgumentException("preset role cannot be deleted");
        }
        int memberCount = domainRoleRepository.countRoleMembers(roleId, domainId);
        if (memberCount > 0) {
            throw new IllegalStateException("role is still bound to members");
        }
        domainRoleRepository.deleteRolePermissions(roleId);
        domainRoleRepository.deleteRoleByIdAndDomain(roleId, domainId);
    }

    public List<DomainRoleDtos.PermissionItemView> listPermissionItems(long domainId) {
        requireDomain(domainId);
        return domainRoleRepository.findAllPermissionItems().stream()
                .map(this::toPermissionItemView)
                .toList();
    }

    private DomainRoleDtos.DomainRoleView loadRole(long domainId, long roleId) {
        DomainRolePo po = domainRoleRepository.findRoleByIdAndDomain(roleId, domainId);
        if (po == null) {
            throw new IllegalArgumentException("domain role not found");
        }
        return toRoleView(po);
    }

    private List<DomainRoleDtos.PermissionItemView> loadPermissionItems(long roleId) {
        return domainRoleRepository.findPermissionItemsByRoleId(roleId).stream()
                .map(this::toPermissionItemView)
                .toList();
    }

    private void ensurePermissionItemsExist(List<Long> permissionItemIds) {
        long count = domainRoleRepository.countPermissionItemsByIds(permissionItemIds);
        if (count != permissionItemIds.size()) {
            throw new IllegalArgumentException("permission item not found");
        }
    }

    private DomainRoleDtos.DomainRoleView toRoleView(DomainRolePo po) {
        return new DomainRoleDtos.DomainRoleView(
                po.getId(),
                po.getBusinessDomainId(),
                po.getCode(),
                po.getName(),
                po.getPreset() != null && po.getPreset() == 1);
    }

    private DomainRoleDtos.PermissionItemView toPermissionItemView(PermissionItemPo po) {
        return new DomainRoleDtos.PermissionItemView(
                po.getId(),
                po.getCode(),
                po.getName(),
                po.getModule(),
                po.getType());
    }

    private List<Long> normalizeIds(List<Long> values) {
        if (values == null) {
            return List.of();
        }
        Set<Long> unique = new LinkedHashSet<>();
        for (Long value : values) {
            if (value != null) {
                unique.add(value);
            }
        }
        return List.copyOf(unique);
    }

    private void requireDomain(long domainId) {
        domainService.getDomain(domainId);
    }
}
