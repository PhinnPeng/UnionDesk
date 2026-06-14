package com.uniondesk.common.audit;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class AuditActionCatalog {

    public record ActionOption(String code, String label) {
    }

    private static final Map<String, String> LABELS = new LinkedHashMap<>();

    static {
        register(AuditActionCodes.PLATFORM_DOMAIN_CREATE, "业务域创建");
        register(AuditActionCodes.PLATFORM_DOMAIN_UPDATE, "业务域更新");
        register(AuditActionCodes.PLATFORM_DOMAIN_UPDATE_STATUS, "业务域状态变更");
        register(AuditActionCodes.PLATFORM_DOMAIN_DELETE, "业务域删除");
        register(AuditActionCodes.PLATFORM_ROLE_PERMISSIONS_UPDATE, "角色权限更新");
        register(AuditActionCodes.PLATFORM_DOMAIN_MEMBER_UPDATE_STATUS, "域成员状态变更");

        alias(AuditActionCodes.LEGACY_DOMAIN_CREATE, AuditActionCodes.PLATFORM_DOMAIN_CREATE);
        alias(AuditActionCodes.LEGACY_DOMAIN_UPDATE, AuditActionCodes.PLATFORM_DOMAIN_UPDATE);
        alias(AuditActionCodes.LEGACY_DOMAIN_DELETE, AuditActionCodes.PLATFORM_DOMAIN_DELETE);
        alias(AuditActionCodes.LEGACY_DOMAIN_MEMBER_UPDATE_STATUS, AuditActionCodes.PLATFORM_DOMAIN_MEMBER_UPDATE_STATUS);
    }

    private AuditActionCatalog() {
    }

    public static String label(String actionCode) {
        if (actionCode == null || actionCode.isBlank()) {
            return actionCode;
        }
        return LABELS.getOrDefault(actionCode.trim(), actionCode.trim());
    }

    public static String labelForDomainStatusChange(boolean enabled) {
        return enabled ? "业务域启用" : "业务域禁用";
    }

    public static List<ActionOption> platformFilterOptions() {
        return List.of(
                new ActionOption(AuditActionCodes.PLATFORM_DOMAIN_CREATE, label(AuditActionCodes.PLATFORM_DOMAIN_CREATE)),
                new ActionOption(AuditActionCodes.PLATFORM_DOMAIN_UPDATE, label(AuditActionCodes.PLATFORM_DOMAIN_UPDATE)),
                new ActionOption(AuditActionCodes.PLATFORM_DOMAIN_UPDATE_STATUS, label(AuditActionCodes.PLATFORM_DOMAIN_UPDATE_STATUS)),
                new ActionOption(AuditActionCodes.PLATFORM_DOMAIN_DELETE, label(AuditActionCodes.PLATFORM_DOMAIN_DELETE)),
                new ActionOption(AuditActionCodes.PLATFORM_ROLE_PERMISSIONS_UPDATE, label(AuditActionCodes.PLATFORM_ROLE_PERMISSIONS_UPDATE)),
                new ActionOption(AuditActionCodes.PLATFORM_DOMAIN_MEMBER_UPDATE_STATUS, label(AuditActionCodes.PLATFORM_DOMAIN_MEMBER_UPDATE_STATUS)));
    }

    private static void register(String code, String label) {
        LABELS.put(code, label);
    }

    private static void alias(String legacyCode, String canonicalCode) {
        String canonicalLabel = LABELS.get(canonicalCode);
        if (canonicalLabel != null) {
            LABELS.put(legacyCode, canonicalLabel);
        }
    }

    public static String resolveActionLabel(String actionCode, String detail) {
        Objects.requireNonNull(actionCode, "actionCode");
        if (AuditActionCodes.PLATFORM_DOMAIN_UPDATE_STATUS.equals(actionCode)) {
            if (detail != null && detail.contains("禁用 → 启用")) {
                return labelForDomainStatusChange(true);
            }
            if (detail != null && detail.contains("启用 → 禁用")) {
                return labelForDomainStatusChange(false);
            }
        }
        return label(actionCode);
    }
}
