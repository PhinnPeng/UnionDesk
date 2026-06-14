package com.uniondesk.common.audit;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.util.StringUtils;

public final class AuditDetailTextBuilder {

    private static final int MAX_DETAIL_LENGTH = 8192;

    private AuditDetailTextBuilder() {
    }

    public static String buildDomainCreateDetail(String name, String code) {
        List<String> lines = new ArrayList<>();
        lines.add("业务域：%s（%s）".formatted(name, code));
        return finalizeDetail(lines);
    }

    public static String buildDomainDeleteDetail(String name, String code) {
        List<String> lines = new ArrayList<>();
        lines.add("业务域：%s（%s）".formatted(name, code));
        return finalizeDetail(lines);
    }

    public static String buildDomainUpdateDetail(
            String name,
            String code,
            String previousName,
            String newName,
            String previousDescription,
            String newDescription,
            List<String> previousVisibilityCodes,
            List<String> newVisibilityCodes,
            String previousRegistration,
            String newRegistration,
            String previousInvitation,
            String newInvitation) {
        List<String> lines = new ArrayList<>();
        lines.add("业务域：%s（%s）".formatted(name, code));
        appendChange(lines, "名称", previousName, newName);
        appendChange(lines, "描述", blankToDash(previousDescription), blankToDash(newDescription));
        appendChange(
                lines,
                "可见策略",
                formatVisibilityPolicies(previousVisibilityCodes),
                formatVisibilityPolicies(newVisibilityCodes));
        appendChange(
                lines,
                "注册策略",
                formatAccessPolicy(previousRegistration, true),
                formatAccessPolicy(newRegistration, true));
        appendChange(
                lines,
                "邀请策略",
                formatAccessPolicy(previousInvitation, false),
                formatAccessPolicy(newInvitation, false));
        return finalizeDetail(lines);
    }

    public static String buildDomainStatusDetail(String name, String code, int previousStatus, int newStatus) {
        List<String> lines = new ArrayList<>();
        lines.add("业务域：%s（%s）".formatted(name, code));
        appendChange(lines, "状态", formatDomainStatus(previousStatus), formatDomainStatus(newStatus));
        return finalizeDetail(lines);
    }

    public static String buildRolePermissionDetail(
            String roleName,
            String roleCode,
            List<String> addedPaths,
            List<String> removedPaths) {
        List<String> lines = new ArrayList<>();
        lines.add("角色：%s（%s）".formatted(roleName, roleCode));
        appendPathSection(lines, "新增菜单权限", addedPaths);
        appendPathSection(lines, "移除菜单权限", removedPaths);
        return finalizeDetail(lines);
    }

    public static String buildMemberStatusDetail(
            String memberTarget,
            String previousStatus,
            String newStatus) {
        List<String> lines = new ArrayList<>();
        lines.add("成员：%s".formatted(memberTarget));
        appendChange(lines, "状态", formatMemberStatus(previousStatus), formatMemberStatus(newStatus));
        return finalizeDetail(lines);
    }

    private static void appendPathSection(List<String> lines, String title, List<String> paths) {
        if (paths == null || paths.isEmpty()) {
            return;
        }
        lines.add("");
        lines.add(title + "：");
        for (String path : paths) {
            lines.add("- " + path);
        }
    }

    private static void appendChange(List<String> lines, String fieldLabel, String before, String after) {
        if (Objects.equals(before, after)) {
            return;
        }
        lines.add("%s：%s → %s".formatted(fieldLabel, before, after));
    }

    private static String formatVisibilityPolicies(List<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return "公开";
        }
        return String.join("、", codes.stream().map(AuditDetailTextBuilder::visibilityLabel).toList());
    }

    private static String visibilityLabel(String code) {
        if (!StringUtils.hasText(code)) {
            return "公开";
        }
        return switch (code.trim()) {
            case "public" -> "公开";
            case "domain_customer_only" -> "仅域内客户可见";
            case "staff_only" -> "仅员工可见";
            default -> code.trim();
        };
    }

    private static String formatAccessPolicy(String value, boolean registration) {
        boolean allowed = DomainAccessPolicyLabels.isAllowed(value);
        if (registration) {
            return allowed ? "开放注册" : "关闭注册";
        }
        return allowed ? "开放邀请" : "关闭邀请";
    }

    private static String formatDomainStatus(int status) {
        return status == 1 ? "启用" : "禁用";
    }

    private static String formatMemberStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return "未知";
        }
        return switch (status.trim().toLowerCase()) {
            case "active", "1", "enabled" -> "启用";
            case "disabled", "0" -> "禁用";
            default -> status.trim();
        };
    }

    private static String blankToDash(String value) {
        return StringUtils.hasText(value) ? value.trim() : "—";
    }

    private static String finalizeDetail(List<String> lines) {
        if (lines.isEmpty()) {
            return "";
        }
        String detail = String.join("\n", lines);
        if (detail.length() <= MAX_DETAIL_LENGTH) {
            return detail;
        }
        return detail.substring(0, MAX_DETAIL_LENGTH - 1) + "…";
    }

    /** 供 AuditDetailTextBuilder 使用的入域策略标签，避免 domain 模块依赖。 */
    static final class DomainAccessPolicyLabels {
        private DomainAccessPolicyLabels() {
        }

        static boolean isAllowed(String value) {
            if (!StringUtils.hasText(value)) {
                return true;
            }
            return !"disallowed".equalsIgnoreCase(value.trim());
        }
    }
}
