package com.uniondesk.domain.core;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniondesk.auth.core.UserContextHolder;
import com.uniondesk.common.event.DomainRoleChangedEvent;
import com.uniondesk.common.event.UnionDeskEventPublisher;
import com.uniondesk.domain.entity.DomainRolePo;
import com.uniondesk.domain.entity.PermissionItemPo;
import com.uniondesk.domain.entity.RoleTemplateDomainPo;
import com.uniondesk.domain.entity.RoleTemplatePo;
import com.uniondesk.domain.repository.DomainRoleRepository;
import com.uniondesk.domain.repository.RoleTemplateRepository;
import com.uniondesk.domain.web.DomainRoleDtos;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class DomainRoleService {

    /** foundation-rules §3.2：每业务域自定义角色最多 20 个（不含预设角色） */
    public static final int MAX_CUSTOM_ROLES_PER_DOMAIN = 20;

    private final DomainRoleRepository domainRoleRepository;
    private final DomainService domainService;
    private final UnionDeskEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final RoleTemplateRepository roleTemplateRepository;

    public DomainRoleService(
            DomainRoleRepository domainRoleRepository,
            DomainService domainService,
            UnionDeskEventPublisher eventPublisher,
            ObjectMapper objectMapper,
            RoleTemplateRepository roleTemplateRepository) {
        this.domainRoleRepository = domainRoleRepository;
        this.domainService = domainService;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
        this.roleTemplateRepository = roleTemplateRepository;
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
        ensureCustomRoleCapacity(domainId);
        domainRoleRepository.insertRole(domainId, request.code().trim(), request.name().trim(), 0);
        Long roleId = domainRoleRepository.findRoleIdByDomainAndCode(domainId, request.code().trim());
        if (roleId == null) {
            throw new IllegalStateException("域角色创建失败");
        }
        publishRoleChanged(domainId, roleId, request.name().trim(), request.code().trim(),
                "create", null, null);
        return loadRole(domainId, roleId);
    }

    @Transactional
    public DomainRoleDtos.DomainRoleView updateRole(long domainId, long roleId, DomainRoleDtos.UpdateDomainRoleRequest request) {
        DomainRoleDtos.DomainRoleView existing = loadRole(domainId, roleId);
        if (existing.preset()) {
            throw new IllegalArgumentException("preset role cannot be updated");
        }
        String code = StringUtils.hasText(request.code()) ? request.code().trim() : existing.code();
        String name = StringUtils.hasText(request.name()) ? request.name().trim() : existing.name();
        domainRoleRepository.updateRole(code, name, roleId, domainId);
        publishRoleChanged(domainId, roleId, name, code, "update", existing.name(), existing.code());
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
        DomainRolePo role = requireRole(domainId, roleId);
        if (role.getPreset() != null && role.getPreset() == 1) {
            throw new IllegalArgumentException("preset role cannot be updated");
        }
        if (role.getTemplateId() != null && isLockedField(role.getLockedFields(), RoleTemplatePo.LOCK_FIELD_PERMISSIONS)) {
            throw DomainErrorCodes.ROLE_TEMPLATE_LOCKED_FIELD.toException();
        }
        List<Long> permissionItemIds = normalizeIds(request.permission_item_ids());
        if (!permissionItemIds.isEmpty()) {
            ensurePermissionItemsExist(permissionItemIds);
        }
        domainRoleRepository.deleteRolePermissions(roleId);
        for (Long permissionItemId : permissionItemIds) {
            domainRoleRepository.insertRolePermission(roleId, permissionItemId);
        }
        publishRoleChanged(domainId, roleId, role.getName(), role.getCode(), "update_permissions", null, null);
        return getRolePermissions(domainId, roleId);
    }

    /**
     * 模板下发：在指定业务域创建角色模板实例（含模板字段回填与权限包复制），逐域调用（每域独立事务）。
     */
    @Transactional
    public long createTemplateInstance(
            long domainId,
            String code,
            String name,
            long templateId,
            int templateVersion,
            String lockedFields,
            List<Long> permissionItemIds) {
        requireDomain(domainId);
        ensureCustomRoleCapacity(domainId);
        domainRoleRepository.insertTemplateInstance(domainId, code, name, templateId, templateVersion, lockedFields);
        Long roleId = domainRoleRepository.findRoleIdByDomainAndCode(domainId, code);
        if (roleId == null) {
            throw new IllegalStateException("域角色实例创建失败");
        }
        replaceRolePermissions(roleId, permissionItemIds);
        publishRoleChanged(domainId, roleId, name, code, "create", null, null);
        return roleId;
    }

    /**
     * 模板同步：将模板当前版本与权限包同步到已下发的域角色实例（每域独立事务）。
     */
    @Transactional
    public void syncTemplateInstance(
            long domainId,
            long roleId,
            int templateVersion,
            String lockedFields,
            List<Long> permissionItemIds) {
        requireDomain(domainId);
        DomainRolePo role = requireRole(domainId, roleId);
        domainRoleRepository.updateTemplateBinding(roleId, domainId, templateVersion, lockedFields);
        replaceRolePermissions(roleId, permissionItemIds);
        publishRoleChanged(domainId, roleId, role.getName(), role.getCode(), "update_permissions", null, null);
    }

    private void replaceRolePermissions(long roleId, List<Long> permissionItemIds) {
        domainRoleRepository.deleteRolePermissions(roleId);
        for (Long permissionItemId : permissionItemIds) {
            domainRoleRepository.insertRolePermission(roleId, permissionItemId);
        }
    }

    private void ensureCustomRoleCapacity(long domainId) {
        if (domainRoleRepository.countCustomRoles(domainId) >= MAX_CUSTOM_ROLES_PER_DOMAIN) {
            throw new IllegalArgumentException("该业务域自定义角色数量已达上限（20 个）");
        }
    }

    private boolean isLockedField(String lockedFieldsJson, String field) {
        if (!StringUtils.hasText(lockedFieldsJson)) {
            return false;
        }
        try {
            List<String> fields = objectMapper.readValue(lockedFieldsJson, new TypeReference<List<String>>() { });
            return fields != null && fields.contains(field);
        }
        catch (Exception ex) {
            return false;
        }
    }

    private DomainRolePo requireRole(long domainId, long roleId) {
        DomainRolePo po = domainRoleRepository.findRoleByIdAndDomain(roleId, domainId);
        if (po == null) {
            throw new IllegalArgumentException("域角色不存在");
        }
        return po;
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
        publishRoleChanged(domainId, roleId, role.name(), role.code(), "delete", null, null);
    }

    private void publishRoleChanged(
            long domainId,
            long roleId,
            String roleName,
            String roleCode,
            String changeType,
            String previousName,
            String previousCode) {
        long operatorUserId = UserContextHolder.current()
                .map(context -> context.userId())
                .orElse(0L);
        eventPublisher.publish(new DomainRoleChangedEvent(
                domainId,
                roleId,
                roleName,
                roleCode,
                operatorUserId,
                changeType,
                previousName,
                previousCode));
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
        Long templateId = po.getTemplateId();
        String templateName = null;
        Integer templateLatestVersion = null;
        String syncMode = null;
        List<String> lockedFields = null;
        if (templateId != null) {
            RoleTemplatePo template = roleTemplateRepository.findById(templateId);
            if (template != null) {
                templateName = template.getName();
                templateLatestVersion = template.getVersion() == null ? 1 : template.getVersion();
            }
            RoleTemplateDomainPo applied = roleTemplateRepository.findDomainByTemplateAndDomain(
                    templateId, po.getBusinessDomainId());
            if (applied != null) {
                syncMode = applied.getSyncMode();
            }
            lockedFields = parseLockedFields(po.getLockedFields());
        }
        return new DomainRoleDtos.DomainRoleView(
                po.getId(),
                po.getBusinessDomainId(),
                po.getCode(),
                po.getName(),
                po.getPreset() != null && po.getPreset() == 1,
                templateId,
                po.getTemplateVersion(),
                lockedFields,
                templateName,
                templateLatestVersion,
                syncMode);
    }

    private List<String> parseLockedFields(String lockedFieldsJson) {
        if (!StringUtils.hasText(lockedFieldsJson)) {
            return List.of();
        }
        try {
            List<String> fields = objectMapper.readValue(lockedFieldsJson, new TypeReference<List<String>>() { });
            return fields == null ? List.of() : fields;
        }
        catch (Exception ex) {
            return List.of();
        }
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
