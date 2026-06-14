package com.uniondesk.common.audit;

import org.springframework.util.StringUtils;

public final class AuditTargetFormatter {

    private AuditTargetFormatter() {
    }

    public static String formatDomain(String name, String code) {
        return "%s-%s".formatted(trim(name), trim(code));
    }

    public static String formatRole(String name, String code) {
        return "%s-%s".formatted(trim(name), trim(code));
    }

    public static String formatMember(String displayName, String loginName, long memberId) {
        if (StringUtils.hasText(displayName) && StringUtils.hasText(loginName)) {
            return "%s-%s".formatted(displayName.trim(), loginName.trim());
        }
        if (StringUtils.hasText(displayName)) {
            return displayName.trim();
        }
        if (StringUtils.hasText(loginName)) {
            return loginName.trim();
        }
        return "成员-" + memberId;
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
