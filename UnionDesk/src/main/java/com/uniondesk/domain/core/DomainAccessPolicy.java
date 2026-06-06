package com.uniondesk.domain.core;

import org.springframework.util.StringUtils;

/**
 * 业务域入域配置：registration_enabled / invitation_enabled，取值 allowed / disallowed。
 */
public final class DomainAccessPolicy {

    public static final String ALLOWED = "allowed";
    public static final String DISALLOWED = "disallowed";

    private DomainAccessPolicy() {
    }

    public static String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return ALLOWED;
        }
        return DISALLOWED.equalsIgnoreCase(value.trim()) ? DISALLOWED : ALLOWED;
    }

    public static boolean isAllowed(String value) {
        return ALLOWED.equals(normalize(value));
    }
}
