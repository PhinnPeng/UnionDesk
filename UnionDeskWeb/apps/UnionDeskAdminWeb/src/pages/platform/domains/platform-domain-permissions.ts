/**
 * 业务域相关权限码（与后端 PermissionCodes 对齐，勿使用已弃用旧码）。
 */
export const PLATFORM_DOMAIN_LIST_READ = "platform.domain.list.read";
export const PLATFORM_DOMAIN_CREATE = "platform.domain.create";
export const PLATFORM_DOMAIN_CONTROL_READ = "platform.domain.control.read";
export const PLATFORM_DOMAIN_CONTROL_ENTRY = "platform.domain.control.entry";
export const PLATFORM_DOMAIN_CONTROL_OVERVIEW = "platform.domain.control.overview";
export const PLATFORM_DOMAIN_CONTROL_GENERAL_UPDATE = "platform.domain.control.general.update";
export const PLATFORM_DOMAIN_CONTROL_GENERAL_UPDATE_STATUS =
	"platform.domain.control.general.update-status";
export const PLATFORM_DOMAIN_CONTROL_GENERAL_DELETE = "platform.domain.control.general.delete";
export const PLATFORM_DOMAIN_CONTROL_CUSTOMER_READ = "platform.domain.control.customer.read";
export const PLATFORM_DOMAIN_CONTROL_CUSTOMER_CREATE = "platform.domain.control.customer.create";
export const PLATFORM_DOMAIN_CONTROL_CUSTOMER_UPDATE_STATUS =
	"platform.domain.control.customer.update-status";
export const PLATFORM_DOMAIN_CONTROL_BLOCKED_WORD_READ = "platform.domain.control.blocked_word.read";
export const PLATFORM_DOMAIN_CONTROL_BLOCKED_WORD_CREATE = "platform.domain.control.blocked_word.create";
export const PLATFORM_DOMAIN_CONTROL_BLOCKED_WORD_DELETE = "platform.domain.control.blocked_word.delete";
export const PLATFORM_LOG_AUDIT_READ = "platform.log.audit.read";
export const PLATFORM_LOG_LOGIN_READ = "platform.log.login.read";
export const PLATFORM_DOMAIN_CONTROL_AUDIT_LOG_READ = "platform.domain.control.audit_log.read";
export const PLATFORM_DOMAIN_CONTROL_LOGIN_LOG_READ = "platform.domain.control.login_log.read";
/** @deprecated 使用 PLATFORM_DOMAIN_CONTROL_CUSTOMER_READ */
export const PLATFORM_DOMAIN_CUSTOMER_READ = PLATFORM_DOMAIN_CONTROL_CUSTOMER_READ;
/** @deprecated 使用 PLATFORM_DOMAIN_CONTROL_CUSTOMER_CREATE */
export const PLATFORM_DOMAIN_CUSTOMER_CREATE = PLATFORM_DOMAIN_CONTROL_CUSTOMER_CREATE;
/** @deprecated 使用 PLATFORM_DOMAIN_CONTROL_CUSTOMER_UPDATE_STATUS */
export const PLATFORM_DOMAIN_CUSTOMER_UPDATE = PLATFORM_DOMAIN_CONTROL_CUSTOMER_UPDATE_STATUS;
export const PLATFORM_DOMAIN_ROLES_READ = "platform.domain.roles.read";
export const PLATFORM_DOMAIN_ROLES_PERMISSIONS_READ = "platform.domain.roles.permissions.read";
export const DOMAIN_MEMBER_READ = "domain.member.read";
export const DOMAIN_MEMBER_CREATE = "domain.member.create";
export const DOMAIN_MEMBER_UPDATE_ROLES = "domain.member.update_roles";
export const DOMAIN_MEMBER_UPDATE_STATUS = "domain.member.update_status";
export const DOMAIN_MEMBER_DELETE = "domain.member.delete";
