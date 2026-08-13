package com.uniondesk.domain.core;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniondesk.auth.core.UserContextHolder;
import com.uniondesk.domain.entity.DomainRolePo;
import com.uniondesk.domain.entity.PermissionItemPo;
import com.uniondesk.domain.entity.RoleTemplateDomainPo;
import com.uniondesk.domain.entity.RoleTemplatePo;
import com.uniondesk.domain.repository.DomainRepository;
import com.uniondesk.domain.repository.DomainRoleRepository;
import com.uniondesk.domain.repository.RoleTemplateRepository;
import com.uniondesk.domain.web.RoleTemplateDtos;
import com.uniondesk.iam.core.StaffAccountService;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 角色模板服务（design §5）：模板 CRUD + apply/sync/unapply/bind-members。
 *
 * <p>apply/sync 按逐域事务（TR-04 部分成功）：每域调用 {@link DomainRoleService}
 * 的独立事务方法，失败域收集到结果列表，不做整批回滚。
 */
@Service
public class RoleTemplateService {

    private static final String DEFAULT_LOCKED_FIELDS_JSON = "[\"permissions\"]";

    private final RoleTemplateRepository roleTemplateRepository;
    private final DomainRoleRepository domainRoleRepository;
    private final DomainRoleService domainRoleService;
    private final DomainRepository domainRepository;
    private final StaffAccountService staffAccountService;
    private final ObjectMapper objectMapper;

    public RoleTemplateService(
            RoleTemplateRepository roleTemplateRepository,
            DomainRoleRepository domainRoleRepository,
            DomainRoleService domainRoleService,
            DomainRepository domainRepository,
            StaffAccountService staffAccountService,
            ObjectMapper objectMapper) {
        this.roleTemplateRepository = roleTemplateRepository;
        this.domainRoleRepository = domainRoleRepository;
        this.domainRoleService = domainRoleService;
        this.domainRepository = domainRepository;
        this.staffAccountService = staffAccountService;
        this.objectMapper = objectMapper;
    }

    // --- 模板 CRUD ---

    public List<RoleTemplateDtos.RoleTemplateView> listTemplates() {
        return roleTemplateRepository.findAll().stream()
                .map(this::toTemplateView)
                .toList();
    }

    public RoleTemplateDtos.RoleTemplateDetailView detail(long templateId) {
        RoleTemplatePo template = requireTemplate(templateId);
        List<RoleTemplateDtos.AppliedDomainView> appliedDomains = roleTemplateRepository
                .findDomainsByTemplateId(templateId).stream()
                .map(this::toAppliedDomainView)
                .toList();
        List<RoleTemplateDtos.PermissionItemView> permissionItems = roleTemplateRepository
                .findPermissionItemsByTemplateId(templateId).stream()
                .map(this::toPermissionItemView)
                .toList();
        return new RoleTemplateDtos.RoleTemplateDetailView(toTemplateView(template), appliedDomains, permissionItems);
    }

    public List<RoleTemplateDtos.PermissionItemView> listPermissionItems() {
        return domainRoleRepository.findAllPermissionItems().stream()
                .map(this::toPermissionItemView)
                .toList();
    }

    @Transactional
    public RoleTemplateDtos.RoleTemplateView createTemplate(RoleTemplateDtos.CreateRoleTemplateRequest request) {
        String code = requireText(request.code(), "模板编码不能为空");
        String name = requireText(request.name(), "模板名称不能为空");
        if (roleTemplateRepository.findByCode(code) != null) {
            throw new IllegalArgumentException("模板编码已存在");
        }
        String syncStrategy = normalizeSyncStrategy(request.sync_strategy());
        String lockedFieldsJson = toLockedFieldsJson(request.locked_fields());
        List<Long> permissionItemIds = normalizeIds(request.permission_item_ids());
        ensurePermissionItemsExist(permissionItemIds);

        RoleTemplatePo po = new RoleTemplatePo();
        po.setCode(code);
        po.setName(name);
        po.setDescription(trimToNull(request.description()));
        po.setSyncStrategy(syncStrategy);
        po.setLockedFields(lockedFieldsJson);
        po.setPreset(0);
        po.setVersion(1);
        po.setCreatedBy(UserContextHolder.current()
                .map(context -> context.userId())
                .orElse(0L));
        roleTemplateRepository.insert(po);
        if (po.getId() == null) {
            throw new IllegalStateException("角色模板创建失败");
        }
        replaceTemplatePermissions(po.getId(), permissionItemIds);
        return toTemplateView(roleTemplateRepository.findById(po.getId()));
    }

    @Transactional
    public RoleTemplateDtos.RoleTemplateView updateTemplate(
            long templateId,
            RoleTemplateDtos.UpdateRoleTemplateRequest request) {
        RoleTemplatePo existing = requireTemplate(templateId);
        String name = StringUtils.hasText(request.name()) ? request.name().trim() : existing.getName();
        String description = request.description() != null ? trimToNull(request.description()) : existing.getDescription();
        String syncStrategy = request.sync_strategy() != null
                ? normalizeSyncStrategy(request.sync_strategy())
                : existing.getSyncStrategy();
        String lockedFieldsJson = request.locked_fields() != null
                ? toLockedFieldsJson(request.locked_fields())
                : existing.getLockedFields();
        List<Long> permissionItemIds = request.permission_item_ids() != null
                ? normalizeIds(request.permission_item_ids())
                : loadTemplatePermissionItemIds(templateId);
        ensurePermissionItemsExist(permissionItemIds);

        int nextVersion = existing.getVersion() == null ? 1 : existing.getVersion() + 1;
        roleTemplateRepository.update(templateId, name, description, syncStrategy, lockedFieldsJson, nextVersion);
        if (request.permission_item_ids() != null) {
            replaceTemplatePermissions(templateId, permissionItemIds);
        }
        RoleTemplateDtos.RoleTemplateView view = toTemplateView(roleTemplateRepository.findById(templateId));
        if (RoleTemplatePo.SYNC_IMMEDIATE.equals(syncStrategy)) {
            syncInstances(templateId, nextVersion, lockedFieldsJson, permissionItemIds);
        }
        return view;
    }

    @Transactional
    public void deleteTemplate(long templateId) {
        requireTemplate(templateId);
        int appliedCount = roleTemplateRepository.countDomainsByTemplateId(templateId);
        if (appliedCount > 0) {
            throw new IllegalArgumentException("模板已下发至 " + appliedCount + " 个业务域，请先解绑后再删除");
        }
        roleTemplateRepository.deletePermissionsByTemplateId(templateId);
        roleTemplateRepository.deleteById(templateId);
    }

    // --- apply / sync / unapply / bind-members ---

    /**
     * 模板下发：一次 apply 多个业务域，逐域生成 domain_role 实例（部分成功）。
     * 满额域（每域自定义角色 ≤20）跳过并返回中文提示。
     */
    public RoleTemplateDtos.BatchResult apply(long templateId, RoleTemplateDtos.ApplyRequest request) {
        RoleTemplatePo template = requireTemplate(templateId);
        List<Long> domainIds = normalizeIds(request.domain_ids());
        if (domainIds.isEmpty()) {
            throw new IllegalArgumentException("请选择需要下发的业务域");
        }
        String syncMode = normalizeSyncMode(request.sync_mode());
        List<Long> permissionItemIds = loadTemplatePermissionItemIds(templateId);

        List<Long> success = new ArrayList<>();
        List<RoleTemplateDtos.DomainResult> skipped = new ArrayList<>();
        List<RoleTemplateDtos.DomainResult> failed = new ArrayList<>();
        for (Long domainId : domainIds) {
            try {
                requireDomain(domainId);
                if (roleTemplateRepository.findDomainByTemplateAndDomain(templateId, domainId) != null) {
                    skipped.add(new RoleTemplateDtos.DomainResult(domainId, "模板已下发至该域，如需更新请使用同步"));
                    continue;
                }
                if (domainRoleRepository.findRoleIdByDomainAndCode(domainId, template.getCode()) != null) {
                    skipped.add(new RoleTemplateDtos.DomainResult(
                            domainId, "该业务域已存在同名角色编码：" + template.getCode()));
                    continue;
                }
                if (domainRoleRepository.countCustomRoles(domainId) >= DomainRoleService.MAX_CUSTOM_ROLES_PER_DOMAIN) {
                    skipped.add(new RoleTemplateDtos.DomainResult(domainId, "该业务域自定义角色已达上限（20 个），已跳过"));
                    continue;
                }
                long roleId = domainRoleService.createTemplateInstance(
                        domainId,
                        template.getCode(),
                        template.getName(),
                        templateId,
                        template.getVersion(),
                        template.getLockedFields(),
                        permissionItemIds);
                roleTemplateRepository.insertDomain(templateId, domainId, roleId, syncMode);
                success.add(domainId);
            }
            catch (RuntimeException ex) {
                failed.add(new RoleTemplateDtos.DomainResult(domainId, toChineseMessage(ex)));
            }
        }
        return new RoleTemplateDtos.BatchResult(success, skipped, failed);
    }

    /**
     * 模板同步（manual 触发）：将模板当前版本与权限包同步到已下发实例。
     */
    public RoleTemplateDtos.BatchResult sync(long templateId, RoleTemplateDtos.SyncRequest request) {
        RoleTemplatePo template = requireTemplate(templateId);
        Set<Long> filterDomainIds = new LinkedHashSet<>(normalizeIds(request == null ? null : request.domain_ids()));
        List<Long> permissionItemIds = loadTemplatePermissionItemIds(templateId);
        int templateVersion = template.getVersion() == null ? 1 : template.getVersion();

        List<Long> success = new ArrayList<>();
        List<RoleTemplateDtos.DomainResult> skipped = new ArrayList<>();
        List<RoleTemplateDtos.DomainResult> failed = new ArrayList<>();
        for (RoleTemplateDomainPo applied : roleTemplateRepository.findDomainsByTemplateId(templateId)) {
            long domainId = applied.getBusinessDomainId();
            if (!filterDomainIds.isEmpty() && !filterDomainIds.contains(domainId)) {
                continue;
            }
            try {
                DomainRolePo instance = domainRoleRepository.findRoleByIdAndDomain(
                        applied.getInstanceDomainRoleId(), domainId);
                if (instance == null) {
                    failed.add(new RoleTemplateDtos.DomainResult(domainId, "该域的角色实例不存在，可能已被删除"));
                    continue;
                }
                domainRoleService.syncTemplateInstance(
                        domainId,
                        applied.getInstanceDomainRoleId(),
                        templateVersion,
                        template.getLockedFields(),
                        permissionItemIds);
                roleTemplateRepository.touchDomain(templateId, domainId);
                success.add(domainId);
            }
            catch (RuntimeException ex) {
                failed.add(new RoleTemplateDtos.DomainResult(domainId, toChineseMessage(ex)));
            }
        }
        return new RoleTemplateDtos.BatchResult(success, skipped, failed);
    }

    /**
     * 模板解绑：清除实例的模板关联（转独立角色），删除下发记录。
     */
    public RoleTemplateDtos.BatchResult unapply(long templateId, RoleTemplateDtos.UnapplyRequest request) {
        requireTemplate(templateId);
        List<Long> domainIds = normalizeIds(request.domain_ids());
        if (domainIds.isEmpty()) {
            throw new IllegalArgumentException("请选择需要解绑的业务域");
        }
        List<Long> success = new ArrayList<>();
        List<RoleTemplateDtos.DomainResult> skipped = new ArrayList<>();
        List<RoleTemplateDtos.DomainResult> failed = new ArrayList<>();
        for (Long domainId : domainIds) {
            try {
                RoleTemplateDomainPo applied = roleTemplateRepository.findDomainByTemplateAndDomain(templateId, domainId);
                if (applied == null) {
                    skipped.add(new RoleTemplateDtos.DomainResult(domainId, "模板未下发至该域"));
                    continue;
                }
                domainRoleRepository.clearTemplateBinding(applied.getInstanceDomainRoleId(), domainId);
                roleTemplateRepository.deleteDomainByTemplateAndDomain(templateId, domainId);
                success.add(domainId);
            }
            catch (RuntimeException ex) {
                failed.add(new RoleTemplateDtos.DomainResult(domainId, toChineseMessage(ex)));
            }
        }
        return new RoleTemplateDtos.BatchResult(success, skipped, failed);
    }

    /**
     * 绑定成员：将员工绑定到模板在各域的实例角色（复用 StaffAccountService.bindDomainMemberships）。
     */
    public RoleTemplateDtos.BatchResult bindMembers(long templateId, RoleTemplateDtos.BindMembersRequest request) {
        RoleTemplatePo template = requireTemplate(templateId);
        List<Long> staffIds = normalizeIds(request.staff_ids());
        List<Long> domainIds = normalizeIds(request.domain_ids());
        if (staffIds.isEmpty()) {
            throw new IllegalArgumentException("请选择需要绑定的员工");
        }
        if (domainIds.isEmpty()) {
            throw new IllegalArgumentException("请选择需要绑定的业务域");
        }
        List<Long> appliedDomainIds = roleTemplateRepository.findDomainsByTemplateId(templateId).stream()
                .filter(applied -> domainIds.contains(applied.getBusinessDomainId()))
                .map(RoleTemplateDomainPo::getBusinessDomainId)
                .toList();
        List<RoleTemplateDtos.DomainResult> skipped = domainIds.stream()
                .filter(domainId -> !appliedDomainIds.contains(domainId))
                .map(domainId -> new RoleTemplateDtos.DomainResult(domainId, "模板未下发至该域"))
                .toList();
        if (appliedDomainIds.isEmpty()) {
            return new RoleTemplateDtos.BatchResult(List.of(), skipped, List.of());
        }

        List<Long> success = new ArrayList<>(appliedDomainIds);
        List<RoleTemplateDtos.DomainResult> failed = new ArrayList<>();
        for (Long staffId : staffIds) {
            try {
                staffAccountService.findById(staffId)
                        .orElseThrow(() -> new IllegalArgumentException("员工不存在"));
                staffAccountService.bindDomainMemberships(staffId, appliedDomainIds, List.of(template.getCode()));
            }
            catch (RuntimeException ex) {
                String reason = toChineseMessage(ex);
                for (Long domainId : appliedDomainIds) {
                    success.remove(domainId);
                    if (failed.stream().noneMatch(result -> result.domain_id() == domainId)) {
                        failed.add(new RoleTemplateDtos.DomainResult(domainId, reason));
                    }
                }
            }
        }
        return new RoleTemplateDtos.BatchResult(success, skipped, failed);
    }

    // --- 内部工具 ---

    private void syncInstances(long templateId, int templateVersion, String lockedFieldsJson, List<Long> permissionItemIds) {
        for (RoleTemplateDomainPo applied : roleTemplateRepository.findDomainsByTemplateId(templateId)) {
            DomainRolePo instance = domainRoleRepository.findRoleByIdAndDomain(
                    applied.getInstanceDomainRoleId(), applied.getBusinessDomainId());
            if (instance == null) {
                continue;
            }
            domainRoleService.syncTemplateInstance(
                    applied.getBusinessDomainId(),
                    applied.getInstanceDomainRoleId(),
                    templateVersion,
                    lockedFieldsJson,
                    permissionItemIds);
            roleTemplateRepository.touchDomain(templateId, applied.getBusinessDomainId());
        }
    }

    private List<Long> loadTemplatePermissionItemIds(long templateId) {
        return roleTemplateRepository.findPermissionsByTemplateId(templateId).stream()
                .map(permission -> permission.getPermissionItemId())
                .toList();
    }

    private void replaceTemplatePermissions(long templateId, List<Long> permissionItemIds) {
        roleTemplateRepository.deletePermissionsByTemplateId(templateId);
        for (Long permissionItemId : permissionItemIds) {
            roleTemplateRepository.insertPermission(templateId, permissionItemId);
        }
    }

    private void ensurePermissionItemsExist(List<Long> permissionItemIds) {
        if (permissionItemIds.isEmpty()) {
            return;
        }
        long count = domainRoleRepository.countPermissionItemsByIds(permissionItemIds);
        if (count != permissionItemIds.size()) {
            throw new IllegalArgumentException("权限项不存在");
        }
    }

    private RoleTemplatePo requireTemplate(long templateId) {
        RoleTemplatePo po = roleTemplateRepository.findById(templateId);
        if (po == null) {
            throw new IllegalArgumentException("角色模板不存在");
        }
        return po;
    }

    private void requireDomain(long domainId) {
        if (domainRepository.findById(domainId) == null) {
            throw new IllegalArgumentException("业务域不存在");
        }
    }

    private String normalizeSyncStrategy(String value) {
        String strategy = StringUtils.hasText(value) ? value.trim().toLowerCase() : RoleTemplatePo.SYNC_IMMEDIATE;
        if (!RoleTemplatePo.SYNC_IMMEDIATE.equals(strategy)
                && !RoleTemplatePo.SYNC_MANUAL.equals(strategy)
                && !RoleTemplatePo.SYNC_NONE.equals(strategy)) {
            throw new IllegalArgumentException("同步策略无效");
        }
        return strategy;
    }

    private String normalizeSyncMode(String value) {
        String mode = StringUtils.hasText(value) ? value.trim().toLowerCase() : RoleTemplatePo.SYNC_IMMEDIATE;
        if (!RoleTemplatePo.SYNC_IMMEDIATE.equals(mode) && !RoleTemplatePo.SYNC_MANUAL.equals(mode)) {
            throw new IllegalArgumentException("同步模式无效");
        }
        return mode;
    }

    private String toLockedFieldsJson(List<String> lockedFields) {
        if (lockedFields == null || lockedFields.isEmpty()) {
            return DEFAULT_LOCKED_FIELDS_JSON;
        }
        List<String> normalized = lockedFields.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
        try {
            return objectMapper.writeValueAsString(normalized);
        }
        catch (Exception ex) {
            throw new IllegalArgumentException("锁定字段格式无效");
        }
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

    private String toChineseMessage(RuntimeException ex) {
        String message = ex.getMessage();
        return StringUtils.hasText(message) ? message : "操作失败";
    }

    private RoleTemplateDtos.RoleTemplateView toTemplateView(RoleTemplatePo po) {
        return new RoleTemplateDtos.RoleTemplateView(
                po.getId(),
                po.getCode(),
                po.getName(),
                po.getDescription(),
                po.getSyncStrategy(),
                parseLockedFields(po.getLockedFields()),
                po.getPreset() != null && po.getPreset() == 1,
                po.getVersion() == null ? 1 : po.getVersion(),
                po.getCreatedBy(),
                po.getCreatedAt(),
                po.getUpdatedAt(),
                roleTemplateRepository.countDomainsByTemplateId(po.getId()));
    }

    private RoleTemplateDtos.AppliedDomainView toAppliedDomainView(RoleTemplateDomainPo po) {
        DomainRolePo instance = domainRoleRepository.findRoleByIdAndDomain(
                po.getInstanceDomainRoleId(), po.getBusinessDomainId());
        Integer instanceVersion = instance == null ? null : instance.getTemplateVersion();
        return new RoleTemplateDtos.AppliedDomainView(
                po.getBusinessDomainId(),
                po.getInstanceDomainRoleId(),
                po.getSyncMode(),
                instanceVersion,
                po.getAppliedAt());
    }

    private RoleTemplateDtos.PermissionItemView toPermissionItemView(PermissionItemPo po) {
        return new RoleTemplateDtos.PermissionItemView(
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

    private static String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
