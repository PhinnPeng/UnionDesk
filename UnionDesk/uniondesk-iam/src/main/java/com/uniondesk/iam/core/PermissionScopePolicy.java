package com.uniondesk.iam.core;

import java.util.Locale;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class PermissionScopePolicy {

    private static final String ROLE_LEVEL_GLOBAL = "global";
    private static final String ROLE_LEVEL_DOMAIN = "domain";
    private static final String PERMISSION_SCOPE_PLATFORM = "platform";
    private static final String PERMISSION_SCOPE_DOMAIN = "domain";

    public boolean canRoleOwnPermission(String roleLevel, String permissionScope) {
        return canRoleOwnPermission(roleLevel, permissionScope, null);
    }

    public boolean canRoleOwnPermission(String roleLevel, String permissionScope, String permissionCode) {
        String normalizedRoleLevel = normalize(roleLevel);
        String normalizedPermissionScope = normalize(permissionScope);
        String normalizedCode = normalizeOptionalCode(permissionCode);
        if (ROLE_LEVEL_GLOBAL.equals(normalizedRoleLevel)) {
            if (!PERMISSION_SCOPE_PLATFORM.equals(normalizedPermissionScope)) {
                return false;
            }
            return normalizedCode == null || normalizedCode.startsWith("platform.");
        }
        if (!ROLE_LEVEL_DOMAIN.equals(normalizedRoleLevel)
                || !PERMISSION_SCOPE_DOMAIN.equals(normalizedPermissionScope)) {
            return false;
        }
        return normalizedCode == null || !normalizedCode.startsWith("platform.");
    }

    public boolean isPermissionEffective(
            String roleLevel,
            String bindingScope,
            Long bindingBusinessDomainId,
            String permissionScope,
            Long targetBusinessDomainId) {
        String normalizedRoleLevel = normalize(roleLevel);
        String normalizedBindingScope = normalize(bindingScope);
        String normalizedPermissionScope = normalize(permissionScope);
        if (!canRoleOwnPermission(normalizedRoleLevel, normalizedPermissionScope)) {
            return false;
        }
        if (PERMISSION_SCOPE_PLATFORM.equals(normalizedPermissionScope)) {
            return ROLE_LEVEL_GLOBAL.equals(normalizedRoleLevel)
                    && ROLE_LEVEL_GLOBAL.equals(normalizedBindingScope);
        }
        if (ROLE_LEVEL_DOMAIN.equals(normalizedRoleLevel)
                && ROLE_LEVEL_DOMAIN.equals(normalizedBindingScope)) {
            if (bindingBusinessDomainId == null) {
                return false;
            }
            return targetBusinessDomainId == null || bindingBusinessDomainId.equals(targetBusinessDomainId);
        }
        return false;
    }

    private static String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeOptionalCode(String permissionCode) {
        if (!StringUtils.hasText(permissionCode)) {
            return null;
        }
        return permissionCode.trim();
    }
}
