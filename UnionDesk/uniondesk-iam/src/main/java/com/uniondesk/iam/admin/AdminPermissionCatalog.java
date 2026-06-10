package com.uniondesk.iam.admin;

import com.uniondesk.iam.core.PermissionCodes;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.util.AntPathMatcher;

public final class AdminPermissionCatalog {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private static final List<PermissionDefinition> DEFINITIONS = List.of(
            new PermissionDefinition(PermissionCodes.PLATFORM_MENU_READ, "View menus", "platform", "GET", "/api/v1/iam/menus/tree"),
            new PermissionDefinition(PermissionCodes.PLATFORM_DASHBOARD_READ, "View dashboard", "platform", "GET", "/api/v1/dashboard"),
            new PermissionDefinition(PermissionCodes.PLATFORM_MENU_CREATE, "Create menu", "platform", "POST", "/api/v1/iam/menus"),
            new PermissionDefinition(PermissionCodes.PLATFORM_MENU_UPDATE, "Update menu", "platform", "PUT", "/api/v1/iam/menus/*"),
            new PermissionDefinition(PermissionCodes.PLATFORM_MENU_DELETE, "Delete menu", "platform", "DELETE", "/api/v1/iam/menus/*"),
            new PermissionDefinition(
                    PermissionCodes.PLATFORM_ADMIN_PERMISSION_CODES_READ,
                    "View admin permission codes",
                    "platform",
                    "GET",
                    "/api/v1/iam/admin-permission-codes"),
            new PermissionDefinition(
                    PermissionCodes.PLATFORM_IAM_RESOURCE_READ,
                    "View IAM resources",
                    "platform",
                    "GET",
                    "/api/v1/iam/resources"),
            new PermissionDefinition(
                    PermissionCodes.PLATFORM_IAM_RESOURCE_CREATE,
                    "Create IAM resource",
                    "platform",
                    "POST",
                    "/api/v1/iam/resources"),
            new PermissionDefinition(
                    PermissionCodes.PLATFORM_IAM_RESOURCE_UPDATE,
                    "Update IAM resource",
                    "platform",
                    "PUT",
                    "/api/v1/iam/resources/*"),
            new PermissionDefinition(
                    PermissionCodes.PLATFORM_IAM_ROLE_RESOURCE_READ,
                    "View role IAM resources",
                    "platform",
                    "GET",
                    "/api/v1/iam/roles/*/resources"),
            new PermissionDefinition(
                    PermissionCodes.PLATFORM_IAM_ROLE_RESOURCE_UPDATE,
                    "Update role IAM resources",
                    "platform",
                    "PUT",
                    "/api/v1/iam/roles/*/resources"),
            new PermissionDefinition(PermissionCodes.PLATFORM_ROLE_READ, "View roles", "platform", "GET", "/api/v1/iam/roles"),
            new PermissionDefinition(PermissionCodes.PLATFORM_ROLE_CREATE, "Create role", "platform", "POST", "/api/v1/iam/roles"),
            new PermissionDefinition(PermissionCodes.PLATFORM_ROLE_UPDATE, "Update role", "platform", "PUT", "/api/v1/iam/roles/*"),
            new PermissionDefinition(PermissionCodes.PLATFORM_ROLE_DELETE, "Delete role", "platform", "DELETE", "/api/v1/iam/roles/*"),
            new PermissionDefinition(PermissionCodes.PLATFORM_ROLE_PERMISSION_READ, "View role permissions", "platform", "GET", "/api/v1/iam/roles/*/permissions"),
            new PermissionDefinition(PermissionCodes.PLATFORM_ROLE_PERMISSION_UPDATE, "Update role permissions", "platform", "PUT", "/api/v1/iam/roles/*/permissions"),
            new PermissionDefinition(PermissionCodes.PLATFORM_ROLE_BIND, "Bind platform role", "platform", "PUT", "/api/v1/admin/staff/*/platform-roles"),
            new PermissionDefinition(PermissionCodes.PLATFORM_ORGANIZATION_READ, "View organization structure", "platform", "GET", "/api/v1/iam/organizations"),
            new PermissionDefinition(PermissionCodes.PLATFORM_ORGANIZATION_CREATE, "Create organization", "platform", "POST", "/api/v1/iam/organizations"),
            new PermissionDefinition(PermissionCodes.PLATFORM_ORGANIZATION_UPDATE, "Update organization", "platform", "PUT", "/api/v1/iam/organizations/*"),
            new PermissionDefinition(PermissionCodes.PLATFORM_ORGANIZATION_DELETE, "Delete organization", "platform", "DELETE", "/api/v1/iam/organizations/*"),
            new PermissionDefinition(PermissionCodes.PLATFORM_USER_READ, "View platform users", "platform", "GET", "/api/v1/iam/users"),
            new PermissionDefinition(PermissionCodes.DOMAIN_USER_CREATE, "Create domain user", "domain", "POST", "/api/v1/iam/users"),
            new PermissionDefinition(PermissionCodes.DOMAIN_USER_READ, "View domain users", "domain", "GET", "/api/v1/iam/users"),
            new PermissionDefinition(PermissionCodes.DOMAIN_USER_UPDATE, "Update domain user", "domain", "PUT", "/api/v1/iam/users/*"),
            new PermissionDefinition(PermissionCodes.DOMAIN_USER_REMOVE, "Remove domain user", "domain", "DELETE", "/api/v1/iam/users/*"),
            new PermissionDefinition(PermissionCodes.PLATFORM_USER_CREATE, "Create platform user", "platform", "POST", "/api/v1/iam/users"),
            new PermissionDefinition(PermissionCodes.PLATFORM_USER_IMPORT, "Import platform users", "platform", null, null),
            new PermissionDefinition(PermissionCodes.PLATFORM_USER_UPDATE, "Update platform user", "platform", "PUT", "/api/v1/iam/users/*"),
            new PermissionDefinition(PermissionCodes.PLATFORM_USER_DISABLE, "Disable platform user", "platform", "POST", "/api/v1/iam/users/*/offboard"),
            new PermissionDefinition(PermissionCodes.PLATFORM_USER_RESET_PASSWORD, "Reset platform user password", "platform", null, null),
            new PermissionDefinition(PermissionCodes.PLATFORM_USER_RESTORE, "Restore platform user", "platform", "POST", "/api/v1/iam/users/*/restore"),
            new PermissionDefinition(PermissionCodes.PLATFORM_USER_OFFBOARD_POOL_READ, "View offboard pool", "platform", "GET", "/api/v1/iam/users/offboard-pool"),
            new PermissionDefinition(PermissionCodes.PLATFORM_USER_OFFBOARD_POOL_EXPORT, "Export offboard pool", "platform", null, null),
            new PermissionDefinition(PermissionCodes.PLATFORM_USER_OFFBOARD_POOL_BATCH_RESTORE, "Batch restore offboard pool", "platform", null, null),
            new PermissionDefinition(PermissionCodes.PLATFORM_USER_DELETE, "Delete platform user", "platform", "DELETE", "/api/v1/iam/users/*"),
            new PermissionDefinition(PermissionCodes.PLATFORM_PERMISSION_MANAGE, "Manage permissions", "platform", "PUT", "/api/v1/auth/login-config"),
            new PermissionDefinition(PermissionCodes.DOMAIN_READ, "View business domains", "domain", "GET", "/api/v1/domains/**"),
            new PermissionDefinition(PermissionCodes.PLATFORM_LOG_AUDIT_READ, "View platform audit logs", "platform", "GET", "/api/v1/admin/audit-logs"),
            new PermissionDefinition(PermissionCodes.PLATFORM_LOG_LOGIN_READ, "View platform login logs", "platform", "GET", "/api/v1/admin/login-logs"),
            new PermissionDefinition(PermissionCodes.PLATFORM_DOMAIN_CONTROL_AUDIT_LOG_READ, "View domain audit logs", "platform", "GET", "/api/v1/admin/domains/*/audit-logs"),
            new PermissionDefinition(PermissionCodes.PLATFORM_DOMAIN_CONTROL_LOGIN_LOG_READ, "View domain login logs", "platform", "GET", "/api/v1/admin/domains/*/login-logs"),
            new PermissionDefinition(PermissionCodes.DOMAIN_ADMIN_LIST_READ, "View domain admin list", "platform", "GET", "/api/v1/admin/domains"),
            new PermissionDefinition(PermissionCodes.PLATFORM_DOMAIN_CONTROL_ENTRY, "Enter domain console", "platform", "GET", "/api/v1/admin/domains/*"),
            new PermissionDefinition(PermissionCodes.PLATFORM_DOMAIN_CONTROL_OVERVIEW, "View domain console overview", "platform", null, null),
            new PermissionDefinition(PermissionCodes.DOMAIN_ADMIN_READ, "View domain admin domains", "platform", "GET", "/api/v1/admin/domains/**"),
            new PermissionDefinition(PermissionCodes.DOMAIN_ADMIN_CREATE, "Create business domain", "platform", "POST", "/api/v1/admin/domains"),
            new PermissionDefinition(PermissionCodes.PLATFORM_DOMAIN_CONTROL_READ, "Enter business domain console", "platform", null, null),
            new PermissionDefinition(PermissionCodes.PLATFORM_DOMAIN_CONTROL_GENERAL_UPDATE, "Update domain general info", "platform", "PUT", "/api/v1/admin/domains/*"),
            new PermissionDefinition(
                    PermissionCodes.PLATFORM_DOMAIN_CONTROL_GENERAL_UPDATE_STATUS,
                    "Update domain status",
                    "platform",
                    "PUT",
                    "/api/v1/admin/domains/*"),
            new PermissionDefinition(PermissionCodes.PLATFORM_DOMAIN_CONTROL_GENERAL_DELETE, "Delete business domain", "platform", "DELETE", "/api/v1/admin/domains/*"),
            new PermissionDefinition(
                    PermissionCodes.PLATFORM_DOMAIN_CONTROL_CUSTOMER_READ,
                    "View domain customers",
                    "platform",
                    "GET",
                    "/api/v1/admin/domains/*/customers/**"),
            new PermissionDefinition(
                    PermissionCodes.PLATFORM_DOMAIN_CONTROL_CUSTOMER_CREATE,
                    "Create domain customer",
                    "platform",
                    "POST",
                    "/api/v1/admin/domains/*/customers/**"),
            new PermissionDefinition(
                    PermissionCodes.PLATFORM_DOMAIN_CONTROL_CUSTOMER_UPDATE_STATUS,
                    "Update domain customer status",
                    "platform",
                    "PATCH",
                    "/api/v1/admin/domains/*/customers/*/status"),
            new PermissionDefinition(PermissionCodes.PLATFORM_DOMAIN_ROLES_READ, "View platform domain roles", "platform", "GET", "/api/v1/admin/domains/*/platform-roles"),
            new PermissionDefinition(
                    PermissionCodes.PLATFORM_DOMAIN_ROLES_PERMISSIONS_READ,
                    "View platform domain role permissions",
                    "platform",
                    "GET",
                    "/api/v1/admin/domains/*/platform-roles/*/permissions"),
            new PermissionDefinition(PermissionCodes.DOMAIN_INVITATION_CODE_READ, "View invitation codes", "domain", "GET", "/api/v1/admin/domains/*/invitation-codes"),
            new PermissionDefinition(PermissionCodes.DOMAIN_INVITATION_CODE_CREATE, "Create invitation code", "domain", "POST", "/api/v1/admin/domains/*/invitation-codes"),
            new PermissionDefinition(PermissionCodes.DOMAIN_INVITATION_CODE_DELETE, "Delete invitation code", "domain", "DELETE", "/api/v1/admin/domains/*/invitation-codes/*"),
            new PermissionDefinition(
                    PermissionCodes.PLATFORM_BLOCKED_WORD_READ,
                    "View platform blocked words",
                    "platform",
                    "GET",
                    "/api/v1/admin/blocked-words"),
            new PermissionDefinition(
                    PermissionCodes.PLATFORM_BLOCKED_WORD_CREATE,
                    "Create platform blocked word",
                    "platform",
                    "POST",
                    "/api/v1/admin/blocked-words"),
            new PermissionDefinition(
                    PermissionCodes.PLATFORM_BLOCKED_WORD_DELETE,
                    "Delete platform blocked word",
                    "platform",
                    "DELETE",
                    "/api/v1/admin/blocked-words/*"),
            new PermissionDefinition(
                    PermissionCodes.PLATFORM_DOMAIN_CONTROL_BLOCKED_WORD_READ,
                    "View domain blocked words",
                    "platform",
                    "GET",
                    "/api/v1/admin/domains/*/blocked-words"),
            new PermissionDefinition(
                    PermissionCodes.PLATFORM_DOMAIN_CONTROL_BLOCKED_WORD_CREATE,
                    "Create domain blocked word",
                    "platform",
                    "POST",
                    "/api/v1/admin/domains/*/blocked-words"),
            new PermissionDefinition(
                    PermissionCodes.PLATFORM_DOMAIN_CONTROL_BLOCKED_WORD_DELETE,
                    "Delete domain blocked word",
                    "platform",
                    "DELETE",
                    "/api/v1/admin/domains/*/blocked-words/*"),
            new PermissionDefinition(PermissionCodes.DOMAIN_CONFIG_READ, "View domain config", "domain", "GET", "/api/v1/admin/domains/*/config"),
            new PermissionDefinition(PermissionCodes.DOMAIN_CONFIG_UPDATE, "Update domain config", "domain", "PUT", "/api/v1/admin/domains/*/config"),
            new PermissionDefinition(PermissionCodes.PLATFORM_SYSTEM_CONFIG_READ, "View system config", "platform", "GET", "/api/v1/admin/system-config"),
            new PermissionDefinition(PermissionCodes.PLATFORM_SYSTEM_CONFIG_UPDATE, "Update system config", "platform", "PUT", "/api/v1/admin/system-config"),
            new PermissionDefinition(PermissionCodes.DOMAIN_SLA_READ, "View SLA rules", "domain", "GET", "/api/v1/admin/domains/*/sla-rules"),
            new PermissionDefinition(PermissionCodes.DOMAIN_SLA_CREATE, "Create SLA rule", "domain", "POST", "/api/v1/admin/domains/*/sla-rules"),
            new PermissionDefinition(PermissionCodes.DOMAIN_SLA_UPDATE, "Update SLA rule", "domain", "PUT", "/api/v1/admin/domains/*/sla-rules/*"),
            new PermissionDefinition(PermissionCodes.DOMAIN_MEMBER_READ, "View domain members", "domain", "GET", "/api/v1/admin/domains/*/members"),
            new PermissionDefinition(PermissionCodes.DOMAIN_MEMBER_CREATE, "Create domain member", "domain", "POST", "/api/v1/admin/domains/*/members/**"),
            new PermissionDefinition(PermissionCodes.DOMAIN_MEMBER_UPDATE_ROLES, "Update domain member roles", "domain", "PUT", "/api/v1/admin/domains/*/members/*/roles"),
            new PermissionDefinition(PermissionCodes.DOMAIN_MEMBER_UPDATE_STATUS, "Update domain member status", "domain", "PUT", "/api/v1/admin/domains/*/members/*/status"),
            new PermissionDefinition(PermissionCodes.DOMAIN_MEMBER_DELETE, "Delete domain member", "domain", "DELETE", "/api/v1/admin/domains/*/members/*"),
            new PermissionDefinition(PermissionCodes.DOMAIN_ROLE_READ, "View domain roles", "domain", "GET", "/api/v1/admin/domains/*/roles"),
            new PermissionDefinition(PermissionCodes.DOMAIN_ROLE_CREATE, "Create domain role", "domain", "POST", "/api/v1/admin/domains/*/roles"),
            new PermissionDefinition(PermissionCodes.DOMAIN_ROLE_UPDATE, "Update domain role", "domain", "PUT", "/api/v1/admin/domains/*/roles/*"),
            new PermissionDefinition(PermissionCodes.DOMAIN_ROLE_DELETE, "Delete domain role", "domain", "DELETE", "/api/v1/admin/domains/*/roles/*"),
            new PermissionDefinition(PermissionCodes.DOMAIN_ROLE_PERMISSION_READ, "View domain role permissions", "domain", "GET", "/api/v1/admin/domains/*/roles/*/permissions"),
            new PermissionDefinition(PermissionCodes.DOMAIN_ROLE_PERMISSION_UPDATE, "Update domain role permissions", "domain", "PUT", "/api/v1/admin/domains/*/roles/*/permissions"),
            new PermissionDefinition(PermissionCodes.DOMAIN_TICKET_TYPE_READ, "View ticket types", "domain", "GET", "/api/v1/admin/domains/*/ticket-types"),
            new PermissionDefinition(PermissionCodes.DOMAIN_TICKET_TYPE_CREATE, "Create ticket type", "domain", "POST", "/api/v1/admin/domains/*/ticket-types"),
            new PermissionDefinition(PermissionCodes.DOMAIN_TICKET_TYPE_UPDATE, "Update ticket type", "domain", "PUT", "/api/v1/admin/domains/*/ticket-types/*"),
            new PermissionDefinition(PermissionCodes.DOMAIN_TICKET_TYPE_DELETE, "Delete ticket type", "domain", "DELETE", "/api/v1/admin/domains/*/ticket-types/*"),
            new PermissionDefinition(PermissionCodes.DOMAIN_TICKET_TEMPLATE_READ, "View ticket templates", "domain", "GET", "/api/v1/admin/domains/*/ticket-templates"),
            new PermissionDefinition(PermissionCodes.DOMAIN_TICKET_TEMPLATE_CREATE, "Create ticket template", "domain", "POST", "/api/v1/admin/domains/*/ticket-templates"),
            new PermissionDefinition(PermissionCodes.DOMAIN_TICKET_TEMPLATE_UPDATE, "Update ticket template", "domain", "PUT", "/api/v1/admin/domains/*/ticket-templates/*"),
            new PermissionDefinition(PermissionCodes.DOMAIN_TICKET_TEMPLATE_DELETE, "Delete ticket template", "domain", "DELETE", "/api/v1/admin/domains/*/ticket-templates/*"),
            new PermissionDefinition(PermissionCodes.DOMAIN_QUICK_REPLY_READ, "View quick replies", "domain", "GET", "/api/v1/admin/domains/*/quick-reply*"),
            new PermissionDefinition(PermissionCodes.DOMAIN_QUICK_REPLY_CREATE, "Create quick reply", "domain", "POST", "/api/v1/admin/domains/*/quick-reply*"),
            new PermissionDefinition(PermissionCodes.DOMAIN_QUICK_REPLY_UPDATE, "Update quick reply", "domain", "PUT", "/api/v1/admin/domains/*/quick-reply*/*"),
            new PermissionDefinition(PermissionCodes.DOMAIN_QUICK_REPLY_DELETE, "Delete quick reply", "domain", "DELETE", "/api/v1/admin/domains/*/quick-reply*/*"),
            new PermissionDefinition(PermissionCodes.DOMAIN_PRIORITY_LEVEL_READ, "View priority levels", "domain", "GET", "/api/v1/admin/domains/*/priority-levels"),
            new PermissionDefinition(PermissionCodes.DOMAIN_PRIORITY_LEVEL_CREATE, "Create priority level", "domain", "POST", "/api/v1/admin/domains/*/priority-levels"),
            new PermissionDefinition(PermissionCodes.DOMAIN_PRIORITY_LEVEL_UPDATE, "Update priority level", "domain", "PUT", "/api/v1/admin/domains/*/priority-levels/*"),
            new PermissionDefinition(PermissionCodes.DOMAIN_PRIORITY_LEVEL_DELETE, "Delete priority level", "domain", "DELETE", "/api/v1/admin/domains/*/priority-levels/*"),
            new PermissionDefinition(PermissionCodes.DOMAIN_NOTIFICATION_TEMPLATE_READ, "View notification templates", "domain", "GET", "/api/v1/admin/domains/*/notification-templates"),
            new PermissionDefinition(PermissionCodes.DOMAIN_NOTIFICATION_TEMPLATE_UPDATE, "Update notification templates", "domain", "PUT", "/api/v1/admin/domains/*/notification-templates/*"),
            new PermissionDefinition(PermissionCodes.TICKET_READ, "View tickets", "shared", "GET", "/api/v1/admin/domains/*/tickets"),
            new PermissionDefinition(PermissionCodes.TICKET_CREATE, "Create ticket", "shared", "POST", "/api/v1/domains/*/tickets"),
            new PermissionDefinition(PermissionCodes.TICKET_VIEW_SELF, "View my tickets", "shared", "GET", "/api/v1/domains/*/tickets/my/**"),
            new PermissionDefinition(PermissionCodes.TICKET_VIEW_DOMAIN_ALL, "View domain tickets", "shared", "GET", "/api/v1/admin/domains/*/tickets/**"),
            new PermissionDefinition(PermissionCodes.TICKET_CLAIM, "Claim ticket", "shared", "POST", "/api/v1/admin/domains/*/tickets/*/claim"),
            new PermissionDefinition(PermissionCodes.TICKET_ASSIGN, "Assign ticket", "shared", "POST", "/api/v1/admin/domains/*/tickets/*/assign"),
            new PermissionDefinition(PermissionCodes.TICKET_REPLY_SELF, "Reply my ticket", "shared", "POST", "/api/v1/domains/*/tickets/my/**/replies"),
            new PermissionDefinition(PermissionCodes.TICKET_REPLY, "Reply ticket", "shared", "POST", "/api/v1/admin/domains/*/tickets/*/replies"),
            new PermissionDefinition(PermissionCodes.TICKET_CLOSE, "Close ticket", "shared", "PATCH", "/api/v1/admin/domains/*/tickets/*/status"),
            new PermissionDefinition(PermissionCodes.TICKET_WITHDRAW_SELF, "Withdraw my ticket", "shared", "POST", "/api/v1/domains/*/tickets/my/**/withdraw"),
            new PermissionDefinition(PermissionCodes.TICKET_MERGE, "Merge ticket", "shared", "POST", "/api/v1/admin/domains/*/tickets/*/merge"),
            new PermissionDefinition(PermissionCodes.ATTACHMENT_UPLOAD, "Attachment upload", "shared", "POST", "/api/v1/attachments/upload"),
            new PermissionDefinition(PermissionCodes.ATTACHMENT_DOWNLOAD, "Attachment download", "shared", "GET", "/api/v1/attachments/*/download"),
            new PermissionDefinition(PermissionCodes.INBOX_READ, "View inbox", "shared", "GET", "/api/v1/inbox/**"),
            new PermissionDefinition(PermissionCodes.INBOX_MARK_READ, "Mark inbox read", "shared", "POST", "/api/v1/inbox/**"),
            new PermissionDefinition(PermissionCodes.CONSULTATION_REPLY, "Reply consultation", "shared", "POST", "/api/v1/consultations/*/messages"));

    private static final Map<String, PermissionDefinition> BY_CODE = buildByCode();

    private AdminPermissionCatalog() {
    }

    public static List<PermissionDefinition> list() {
        return DEFINITIONS;
    }

    public static Optional<PermissionDefinition> findByCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(BY_CODE.get(code.trim()));
    }

    public static Optional<PermissionDefinition> findByRequest(String method, String requestPath) {
        if (method == null || requestPath == null) {
            return Optional.empty();
        }
        String normalizedMethod = method.trim().toUpperCase(Locale.ROOT);
        return DEFINITIONS.stream()
                .filter(definition -> definition.httpMethod() != null)
                .filter(definition -> definition.pathPattern() != null)
                .filter(definition -> definition.httpMethod().equals(normalizedMethod))
                .filter(definition -> PATH_MATCHER.match(definition.pathPattern(), requestPath))
                .findFirst();
    }

    private static Map<String, PermissionDefinition> buildByCode() {
        Map<String, PermissionDefinition> definitions = new LinkedHashMap<>();
        for (PermissionDefinition definition : DEFINITIONS) {
            PermissionDefinition previous = definitions.put(definition.code(), definition);
            if (previous != null) {
                throw new IllegalStateException("duplicate permission code: " + definition.code());
            }
        }
        return definitions;
    }

    public record PermissionDefinition(
            String code,
            String name,
            String permissionScope,
            String httpMethod,
            String pathPattern) {

        public PermissionDefinition {
            Objects.requireNonNull(code, "code");
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(permissionScope, "permissionScope");
        }
    }
}
