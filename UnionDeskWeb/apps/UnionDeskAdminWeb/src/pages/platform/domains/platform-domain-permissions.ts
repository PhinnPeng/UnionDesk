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
export const PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_READ = "platform.domain.control.ticket_type.read";
export const PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_CREATE = "platform.domain.control.ticket_type.create";
export const PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_UPDATE = "platform.domain.control.ticket_type.update";
export const PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_DELETE = "platform.domain.control.ticket_type.delete";
export const PLATFORM_LOG_AUDIT_READ = "platform.log.audit.read";
export const PLATFORM_LOG_LOGIN_READ = "platform.log.login.read";
export const PLATFORM_DOMAIN_CONTROL_AUDIT_LOG_READ = "platform.domain.control.audit_log.read";
export const PLATFORM_DOMAIN_CONTROL_LOGIN_LOG_READ = "platform.domain.control.login_log.read";
export const PLATFORM_DOMAIN_ROLES_READ = "platform.domain.roles.read";
export const PLATFORM_DOMAIN_ROLES_PERMISSIONS_READ = "platform.domain.roles.permissions.read";
export const PLATFORM_DOMAIN_CONTROL_MEMBER_READ = "platform.domain.control.member.read";
export const PLATFORM_DOMAIN_CONTROL_MEMBER_CREATE = "platform.domain.control.member.create";
export const PLATFORM_DOMAIN_CONTROL_MEMBER_UPDATE_ROLES =
	"platform.domain.control.member.update_roles";
export const PLATFORM_DOMAIN_CONTROL_MEMBER_UPDATE_STATUS =
	"platform.domain.control.member.update_status";
export const PLATFORM_DOMAIN_CONTROL_MEMBER_DELETE = "platform.domain.control.member.delete";
export const DOMAIN_MEMBER_READ = PLATFORM_DOMAIN_CONTROL_MEMBER_READ;
export const DOMAIN_MEMBER_CREATE = PLATFORM_DOMAIN_CONTROL_MEMBER_CREATE;
export const DOMAIN_MEMBER_UPDATE_ROLES = PLATFORM_DOMAIN_CONTROL_MEMBER_UPDATE_ROLES;
export const DOMAIN_MEMBER_UPDATE_STATUS = PLATFORM_DOMAIN_CONTROL_MEMBER_UPDATE_STATUS;
export const DOMAIN_MEMBER_DELETE = PLATFORM_DOMAIN_CONTROL_MEMBER_DELETE;
