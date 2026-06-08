package com.uniondesk.domain.core;

import com.uniondesk.domain.repository.DomainBootstrapRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DomainBootstrapService {

    private static final List<PresetRole> PRESET_ROLES = List.of(
            new PresetRole("super_admin", "业务域所有人"),
            new PresetRole("domain_admin", "业务域管理员"),
            new PresetRole("agent", "客服"));

    private final DomainBootstrapRepository domainBootstrapRepository;

    public DomainBootstrapService(DomainBootstrapRepository domainBootstrapRepository) {
        this.domainBootstrapRepository = domainBootstrapRepository;
    }

    public BootstrapResult bootstrapNewDomain(long domainId, long creatorUserId) {
        seedPresetRoles(domainId);
        seedSuperAdminAllPermissionItems(domainId);
        long staffAccountId = resolveStaffAccountId(creatorUserId);
        grantCreatorSuperAdmin(domainId, creatorUserId, staffAccountId);
        return new BootstrapResult(staffAccountId, "super_admin");
    }

    void seedPresetRoles(long domainId) {
        for (PresetRole preset : PRESET_ROLES) {
            domainBootstrapRepository.insertRoleIfNotExists(domainId, preset.code(), preset.name());
        }
    }

    long resolveStaffAccountId(long userId) {
        Long staffId = domainBootstrapRepository.findStaffAccountIdById(userId);
        if (staffId != null) {
            return staffId;
        }

        String username = domainBootstrapRepository.findUsernameByUserId(userId);
        if (username == null) {
            throw new IllegalStateException("creator user account not found: " + userId);
        }
        if (!StringUtils.hasText(username)) {
            throw new IllegalStateException("creator user account has no username: " + userId);
        }

        staffId = domainBootstrapRepository.findStaffAccountIdByUsername(username.trim());
        if (staffId == null) {
            throw new IllegalStateException("staff account not found for creator user: " + userId);
        }
        return staffId;
    }

    void grantCreatorSuperAdmin(long domainId, long creatorUserId, long staffAccountId) {
        Long memberId = domainBootstrapRepository.findActiveMemberId(domainId, staffAccountId);
        if (memberId == null) {
            domainBootstrapRepository.insertDomainMember(staffAccountId, domainId);
            memberId = requireActiveMemberId(domainId, staffAccountId);
        }

        long superAdminRoleId = requireDomainRoleId(domainId, "super_admin");
        domainBootstrapRepository.insertMemberRoleIfNotExists(memberId, superAdminRoleId);

        int legacySuperAdminRoleId = requireLegacyRoleId("super_admin");
        syncCreatorDomainSuperAdminBinding(domainId, creatorUserId, legacySuperAdminRoleId);
    }

    void seedSuperAdminAllPermissionItems(long domainId) {
        long superAdminRoleId = requireDomainRoleId(domainId, "super_admin");
        domainBootstrapRepository.seedSuperAdminAllPermissions(superAdminRoleId);
    }

    void syncCreatorDomainSuperAdminBinding(long domainId, long creatorUserId, int legacySuperAdminRoleId) {
        domainBootstrapRepository.insertIamRoleBindingIfNotExists(creatorUserId, legacySuperAdminRoleId, domainId);
    }

    private long requireActiveMemberId(long domainId, long staffAccountId) {
        Long memberId = domainBootstrapRepository.findActiveMemberId(domainId, staffAccountId);
        if (memberId == null) {
            throw new IllegalStateException("domain member bootstrap failed");
        }
        return memberId;
    }

    private long requireDomainRoleId(long domainId, String code) {
        Long roleId = domainBootstrapRepository.findDomainRoleId(domainId, code);
        if (roleId == null) {
            throw new IllegalStateException("domain role not found: " + code);
        }
        return roleId;
    }

    private int requireLegacyRoleId(String code) {
        Integer roleId = domainBootstrapRepository.findLegacyRoleId(code);
        if (roleId == null) {
            throw new IllegalStateException("legacy role not found: " + code);
        }
        return roleId;
    }

    public record BootstrapResult(long staffAccountId, String grantedRole) {
    }

    private record PresetRole(String code, String name) {
    }
}
