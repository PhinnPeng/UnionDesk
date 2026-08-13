package com.uniondesk.common.audit;

public final class AuditActionCodes {

    public static final String PLATFORM_DOMAIN_CREATE = "platform.domain.create";
    public static final String PLATFORM_DOMAIN_UPDATE = "platform.domain.update";
    public static final String PLATFORM_DOMAIN_UPDATE_STATUS = "platform.domain.update_status";
    public static final String PLATFORM_DOMAIN_DELETE = "platform.domain.delete";
    public static final String PLATFORM_ROLE_PERMISSIONS_UPDATE = "platform.role.permissions.update";
    public static final String PLATFORM_DOMAIN_MEMBER_UPDATE_STATUS = "platform.domain.member.update_status";
    public static final String PLATFORM_USER_DOMAIN_BATCH_STATUS = "platform.user.domain_batch_status";
    public static final String DOMAIN_MEMBER_CREATE = "domain.member.create";
    public static final String DOMAIN_MEMBER_UPDATE_ROLES = "domain.member.update_roles";
    public static final String DOMAIN_MEMBER_REMOVE = "domain.member.remove";
    public static final String DOMAIN_ROLE_CREATE = "domain.role.create";
    public static final String DOMAIN_ROLE_UPDATE = "domain.role.update";
    public static final String DOMAIN_ROLE_UPDATE_PERMISSIONS = "domain.role.update_permissions";
    public static final String DOMAIN_ROLE_DELETE = "domain.role.delete";

    /** @deprecated 新写入请使用 {@link #PLATFORM_DOMAIN_CREATE} */
    public static final String LEGACY_DOMAIN_CREATE = "domain.create";
    /** @deprecated 新写入请使用 {@link #PLATFORM_DOMAIN_UPDATE} */
    public static final String LEGACY_DOMAIN_UPDATE = "domain.update";
    /** @deprecated 新写入请使用 {@link #PLATFORM_DOMAIN_DELETE} */
    public static final String LEGACY_DOMAIN_DELETE = "domain.delete";
    /** @deprecated 新写入请使用 {@link #PLATFORM_DOMAIN_MEMBER_UPDATE_STATUS} */
    public static final String LEGACY_DOMAIN_MEMBER_UPDATE_STATUS = "domain.member.update_status";

    private AuditActionCodes() {
    }
}
