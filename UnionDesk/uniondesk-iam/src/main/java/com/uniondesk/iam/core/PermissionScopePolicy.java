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
        String normalizedRoleLevel = normalize(roleLevel);
        String normalizedPermissionScope = normalize(permissionScope);
        if (ROLE_LEVEL_GLOBAL.equals(normalizedRoleLevel)) {
            return PERMISSION_SCOPE_PLATFORM.equals(normalizedPermissionScope)
                    || PERMISSION_SCOPE_DOMAIN.equals(normalizedPermissionScope);
        }
        return ROLE_LEVEL_DOMAIN.equals(normalizedRoleLevel)
                && PERMISSION_SCOPE_DOMAIN.equals(normalizedPermissionScope);
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
        if (ROLE_LEVEL_GLOBAL.equals(normalizedRoleLevel)
                && ROLE_LEVEL_GLOBAL.equals(normalizedBindingScope)) {
            return true;
        }
        if (!ROLE_LEVEL_DOMAIN.equals(normalizedRoleLevel)
                || !ROLE_LEVEL_DOMAIN.equals(normalizedBindingScope)
                || bindingBusinessDomainId == null) {
            return false;
        }
        return targetBusinessDomainId == null || bindingBusinessDomainId.equals(targetBusinessDomainId);
    }

    private static String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
