export type BackendHealthResponse = {
  status: string;
};

export type ClientCode = "ud-admin-web" | "ud-customer-web" | string;

export type LoginRequest = {
  username: string;
  password: string;
  captchaToken?: string;
};

export type CaptchaTrackPoint = {
  x: number;
  t: number;
};

export type CaptchaChallengeResponse = {
  challengeId: string;
  expiresInSeconds: number;
};

export type CaptchaVerifyRequest = {
  challengeId: string;
  track: CaptchaTrackPoint[];
};

export type CaptchaVerifyResponse = {
  captchaToken: string;
  expiresInSeconds: number;
};

export type AuthPublicKeyResponse = {
  publicKey: string;
};

export type LoginResponse = {
  accessToken: string;
  refreshToken: string;
  sid: string;
  role: string;
  clientCode: ClientCode;
  tokenType: string;
  expiresInSeconds: number;
  user: LoginUserView;
  accessibleDomains: BusinessDomainView[];
  /** 本次会话落点域（兼容字段名） */
  defaultBusinessDomainId: number;
  /** 用户默认业务域偏好；未设置时为 null/undefined */
  preferredDefaultDomainId?: number | null;
  /** 本次登录是否因新 IP 写入了站内风险提醒 */
  riskLoginNotified?: boolean;
  /** 是否需强制修改密码（管理员重置密码后为 true，首次登录强制改密） */
  mustChangePassword?: boolean;
};

export type RegisterRequest = {
  loginName: string;
  password: string;
  displayName?: string;
  phone: string;
  email?: string;
  domainId?: number | null;
  invitationCode?: string;
  captchaToken?: string;
};

export type RegisterResponse = {
  accessToken: string;
  refreshToken: string;
  accountId: number;
};

export type SetDefaultDomainRequest = {
  domainId: number;
};

export type SetDefaultDomainResponse = {
  preferredDefaultDomainId: number;
};

export type SwitchDomainRequest = {
  domainId: number;
};

export type SwitchDomainResponse = {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresInSeconds: number;
  businessDomainId: number;
  accessibleDomains?: BusinessDomainView[];
};

export type LoginConfig = {
  passwordLoginEnabled: boolean;
  usernameLoginEnabled: boolean;
  emailLoginEnabled: boolean;
  mobileLoginEnabled: boolean;
  captchaEnabled: boolean;
  wechatLoginEnabled: boolean;
  wechatLoginUrl?: string | null;
  wechatHint?: string | null;
  captchaHint?: string | null;
  sessionTtlSeconds: number;
  maxActiveSessionsPerUser: number;
  updatedAt?: string | null;
};

export type AuthSessionStatus = {
  authenticated: boolean;
  username?: string | null;
  role?: string | null;
  clientCode?: ClientCode | null;
  sid?: string | null;
  userId?: number | null;
  businessDomainId?: number | null;
  expiresAt?: string | null;
};

export type AuthPersistMode = "local" | "session";

export type AuthSessionState = {
  username: string;
  accessToken: string;
  refreshToken: string;
  role: string;
  clientCode: ClientCode;
  authenticatedAt: string;
  persistMode?: AuthPersistMode;
  sid?: string | null;
  userId?: number | null;
  businessDomainId?: number | null;
  expiresAt?: string | null;
  /** 是否需强制修改密码（管理员重置密码后为 true，首次登录强制改密） */
  mustChangePassword?: boolean;
};

export type LoginUserView = {
  id: number;
  username: string;
  mobile?: string | null;
  email?: string | null;
  roles: string[];
};

export type BusinessDomainView = {
  id: number;
  code: string;
  name: string;
  visibilityPolicy?: string | null;
  status?: number | null;
};

export type UpdateLoginConfigRequest = {
  passwordLoginEnabled?: boolean;
  usernameLoginEnabled?: boolean;
  emailLoginEnabled?: boolean;
  mobileLoginEnabled?: boolean;
  captchaEnabled?: boolean;
  wechatLoginEnabled?: boolean;
  captchaHint?: string | null;
  wechatHint?: string | null;
  sessionTtlSeconds?: number;
  maxActiveSessionsPerUser?: number;
};

export type SessionView = {
  userId: number;
  role: string;
  businessDomainId?: number | null;
  sid: string;
  clientCode: ClientCode;
};

export type OnlineSessionView = {
  sid: string;
  userId: number;
  clientCode: ClientCode;
  username: string;
  mobile?: string | null;
  email?: string | null;
  role: string;
  businessDomainId?: number | null;
  loginIdentifierMasked: string;
  sessionStatus: string;
  issuedAt: string;
  expiresAt: string;
  lastSeenAt?: string | null;
  clientIp?: string | null;
  userAgent?: string | null;
};

export type LoginLogView = {
  id: number;
  sid?: string | null;
  userId?: number | null;
  username?: string | null;
  loginIdentifierMasked: string;
  loginIdentifierType: string;
  eventType: string;
  result: string;
  reason?: string | null;
  clientIp?: string | null;
  userAgent?: string | null;
  createdAt: string;
};

export type PlatformOrganizationView = {
  id: number;
  code: string;
  name: string;
  parentId?: number | null;
  parentName?: string | null;
  leaderUserId?: number | null;
  leaderName?: string | null;
  orderNo: number;
  status: number;
  remark?: string | null;
  createdAt: string;
};

export type IamResource = {
  id: number;
  resourceType: "menu" | "action" | "api" | string;
  resourceCode: string;
  resourceName: string;
  clientScope: ClientCode | "all" | string;
  httpMethod?: string | null;
  pathPattern?: string | null;
  parentId?: number | null;
  orderNo?: number;
  icon?: string | null;
  component?: string | null;
  hidden?: boolean;
  status: number;
};

export type PermissionSnapshotUser = {
  id: number;
  username: string;
  mobile?: string | null;
  email?: string | null;
};

export type PermissionSnapshotDomain = {
  id: number;
  code: string;
  name: string;
};

export type PermissionSnapshotMenu = {
  id?: number;
  code: string;
  name: string;
  path: string | null;
  parentId?: number | null;
  orderNo?: number;
  icon?: string | null;
  component?: string | null;
  scope?: "platform" | "business" | string;
  hidden?: boolean;
  permissionCode?: string | null;
  children?: PermissionSnapshotMenu[];
};

export type PermissionSnapshotAction = {
  code: string;
  name?: string;
  httpMethod?: string | null;
  pathPattern?: string | null;
};

export type PermissionSnapshot = {
  user: PermissionSnapshotUser;
  clientCode: ClientCode;
  roles: string[];
  domains: PermissionSnapshotDomain[];
  menuTree: PermissionSnapshotMenu[];
  actions: PermissionSnapshotAction[];
  issuedAt: string;
};

export type MenuTreeNode = {
  id: number;
  code: string;
  nodeType: "catalog" | "menu" | "button" | string;
  scope?: "platform" | "business" | string;
  name: string;
  routePath?: string | null;
  componentKey?: string | null;
  permissionCode?: string | null;
  parentId?: number | null;
  orderNo: number;
  icon?: string | null;
  hidden: boolean;
  status: number;
  required: boolean;
  children: MenuTreeNode[];
};

export type AdminPermissionCode = {
  code: string;
  name: string;
  permissionScope?: "platform" | "domain" | string;
  httpMethod: string;
  pathPattern: string;
};

export type CreateMenuPayload = {
  nodeType: "catalog" | "menu" | "button" | string;
  name: string;
  routePath?: string | null;
  componentKey?: string | null;
  permissionCode?: string | null;
  scope?: "platform" | "business" | string;
  parentId?: number | null;
  orderNo?: number;
  icon?: string | null;
  hidden?: boolean;
  status?: number;
};

export type UpdateMenuPayload = Partial<CreateMenuPayload>;

export type IamRole = {
  id: number;
  code: string;
  name: string;
  scope: "global" | "domain" | string;
  system: boolean;
};

export type CreateRolePayload = {
  code: string;
  name: string;
  scope: "global" | "domain" | string;
};

export type UpdateRolePayload = Partial<CreateRolePayload>;

export type RolePermissions = {
  roleId: number;
  menuIds: number[];
  buttonIds: number[];
};

export type UpdateRolePermissionsPayload = {
  menuIds: number[];
  buttonIds: number[];
};

export type IamUser = {
  id: number;
  username: string;
  mobile: string;
  email?: string | null;
  remark?: string | null;
  accountType: "admin" | "customer" | string;
  status: number;
  employmentStatus: "active" | "offboarded" | string;
  roleCodes: string[];
  businessDomainIds: number[];
  organizationIds: number[];
  offboardedAt?: string | null;
  offboardedBy?: number | null;
  offboardReason?: string | null;
};

export type CreateIamUserPayload = {
  username: string;
  mobile: string;
  email?: string | null;
  remark?: string | null;
  password: string;
  accountType: "admin" | "customer" | string;
  roleCodes: string[];
  businessDomainIds: number[];
  organizationIds?: number[];
};

export type UpdateIamUserPayload = {
  username?: string;
  mobile?: string;
  email?: string | null;
  remark?: string | null;
  password?: string;
  accountType?: "admin" | "customer" | string;
  roleCodes?: string[];
  businessDomainIds?: number[];
  status?: number;
  organizationIds?: number[];
};

export type TicketStatus = "open" | "processing" | "waiting_customer" | "resolved" | "closed" | string;

export type TicketPriority = "low" | "normal" | "high" | "urgent";

export type TicketRecord = {
  id: number;
  ticketNo: string;
  title: string;
  status: TicketStatus;
  createdAt: string;
};

export type CreateTicketRequest = {
  title: string;
  description: string;
  ticketTypeId: number;
};

export type TicketActionResponse = {
  ok: boolean;
  status: string;
};

export type DemoDomain = {
  id: number;
  code: string;
  name: string;
  description: string;
  accent: string;
  supportLine: string;
};

export type DemoProfile = {
  customerId: number;
  nickname: string;
  phone: string;
  selectedDomainId: number;
};

export type AdminProfile = {
  username: string;
  selectedDomainId: number;
};

export type TicketMeta = {
  businessDomainId: number;
  customerId: number;
  ticketTypeId: number;
  priority: TicketPriority;
  description: string;
};

export type DemoTicket = TicketRecord & TicketMeta & {
  source: "web";
};

export type DashboardStats = {
  totalTickets: number;
  openTickets: number;
  processingTickets: number;
  waitingCustomerTickets: number;
  resolvedTickets: number;
  closedTickets: number;
  urgentOpenTickets: number;
  openConsultationSessions: number;
};

export type ConsultationSessionSummary = {
  sessionNo: string;
  businessDomainId: number;
  customerId: number;
  sessionStatus: "open" | "processing" | "closed" | string;
  assignedTo?: number | null;
  lastMessageAt?: string | null;
  lastMessagePreview?: string | null;
};

export type ConsultationMessage = {
  sessionNo: string;
  seqNo: number;
  senderRole: "customer" | "agent" | "system" | string;
  senderUserId?: number | null;
  content: string;
  createdAt: string;
};

export type SendConsultationMessagePayload = {
  businessDomainId: number;
  customerId: number;
  senderUserId?: number;
  sessionNo?: string;
  senderRole: "customer" | "agent";
  content: string;
};

/** P0 分页响应（doc/P0接口契约表.md） */
export type P0PageResult<T> = {
  total: number;
  list: T[];
};

export type P0VisibilityPolicyCode = "public" | "domain_customer_only" | "channel_only";

export type P0AccessPolicy = "allowed" | "disallowed";

export type P0RegistrationPolicy = "open" | "invitation_only" | "admin_only";

/** 平台 / 域管理侧业务域行（对齐 Domain DTO） */
export type AdminDomain = {
  id: string;
  code: string;
  name: string;
  description?: string | null;
  logo?: string | null;
  visibility_policy_codes: P0VisibilityPolicyCode[];
  registration_enabled: P0AccessPolicy;
  invitation_enabled: P0AccessPolicy;
  status?: string | null;
  created_at?: string | null;
  updated_at?: string | null;
  created_by?: string | null;
  updated_by?: string | null;
  creator_name?: string | null;
  updater_name?: string | null;
};

export type CreateAdminDomainPayload = {
  name: string;
  code: string;
  description?: string;
  logo?: string;
  visibility_policy_codes: P0VisibilityPolicyCode[];
  registration_enabled: P0AccessPolicy;
  invitation_enabled: P0AccessPolicy;
  /** 可选：建域时套用的团队模板 id */
  team_template_id?: number | string | null;
};

export type UpdateAdminDomainPayload = {
  name?: string;
  description?: string;
  logo?: string;
  visibility_policy_codes?: P0VisibilityPolicyCode[];
  registration_enabled?: P0AccessPolicy;
  invitation_enabled?: P0AccessPolicy;
  status?: string | number;
};

export type P0StepUpRequest = {
  password: string;
  operation_code: string;
};

export type P0StepUpResponse = {
  step_up_token: string;
  expires_in: number;
  reuse_policy: "session_15m" | "one_time";
  operation_code: string;
};

/** 与 P0 文档示例一致的敏感操作编码占位，后端接入后需与清单保持一致 */
export const P0_STEP_UP_OPERATION = {
  DELETE_BUSINESS_DOMAIN: "business_domain.delete",
  PLATFORM_ROLES_ASSIGN: "staff.platform_roles",
  DOMAIN_SUPER_ADMIN_GRANT: "domain.super_admin.grant"
} as const;

export type P0InboxMessage = {
  id: string;
  title: string;
  content?: string | null;
  jump_url?: string | null;
  is_read: boolean;
  domain_name?: string | null;
  created_at: string;
};

export type P0InboxPageResponse = {
  total: number;
  unread_count: number;
  list: P0InboxMessage[];
};

export type CustomerSatisfactionView = {
  id: number;
  rating: number;
  comment: string | null;
  status: string;
  createdAt: string;
};

export type P0AdminTicketListItem = {
  id: string;
  ticket_no: string;
  title: string;
  type_name?: string | null;
  status: string;
  priority?: string | null;
  assignee_name?: string | null;
  sla_status?: string | null;
  created_at: string;
  updated_at?: string | null;
};

export type P0InvitationCode = {
  id: string;
  domain_id: string;
  code: string;
  channel?: string | null;
  expires_at?: string | null;
  max_uses?: number | null;
  used_count?: number | null;
  status?: string | null;
  created_at?: string | null;
};

export type CreateP0InvitationCodePayload = {
  channel?: string | null;
  expires_at?: string | null;
  max_uses?: number | null;
};

export type DomainRole = {
  id: string;
  business_domain_id: string;
  code: string;
  name: string;
  preset: boolean;
};

export type DomainPermissionItem = {
  id: string;
  code: string;
  name: string;
  module?: string | null;
  type?: string | null;
};

export type DomainRolePermissions = {
  role_id: string;
  code: string;
  name: string;
  permission_items: DomainPermissionItem[];
};

export type DomainMember = {
  id: string;
  staff_account_id: string;
  business_domain_id: string;
  username?: string | null;
  real_name?: string | null;
  nickname?: string | null;
  /** @deprecated 使用 username */
  login_name?: string | null;
  phone?: string | null;
  email?: string | null;
  status?: string | null;
  source?: string | null;
  activated_at?: string | null;
  disabled_at?: string | null;
  deleted_at?: string | null;
  created_at?: string | null;
  roles?: DomainRole[];
};

export type DomainStaffCandidate = {
  id: string;
  username?: string | null;
  real_name?: string | null;
  nickname?: string | null;
  phone?: string | null;
  email?: string | null;
  status?: string | null;
};

export type BlockedWord = {
  id: string;
  word: string;
  created_at?: string | null;
};

export type BlockedWordBatchResult = {
  created_count: number;
  skipped: { word: string; reason: string }[];
};

export type TicketStatusFlowState = {
  code: string;
  name: string;
  state_type: "in_progress" | "paused" | "terminal";
  allow_customer_withdraw?: boolean;
  is_resolved?: boolean;
  position?: { x: number; y: number };
};

/** Assembled workflow graph. `transitions[].from` may be `*` (any state); `to` must be a concrete state code. */
export type TicketStatusFlow = {
  states: TicketStatusFlowState[];
  transitions: { from: string; to: string; label?: string }[];
  /** Unique workflow entry state; null when states is empty. House icon in UI keys off this field. */
  initial_state_code?: string | null;
};

export type DomainTicketType = {
	id: string;
	domain_id: string;
	code: string;
	name: string;
	description?: string | null;
	description_template_md?: string | null;
	icon?: string | null;
	category?: string;
	status: "active" | "disabled" | string;
	status_flow: TicketStatusFlow | Record<string, unknown> | null;
	form_schema: Record<string, unknown> | null;
	form_schema_draft?: Record<string, unknown> | null;
	form_schema_current_version_no?: number | null;
	form_schema_has_unpublished?: boolean | null;
	/** 溯源平台事项类型 ID；直接创建时为 null */
	source_global_type_id?: string | null;
	transition_rules?: TransitionRule[];
};

export type TransitionRule = {
	id?: string;
	from_state_code: string;
	to_state_code: string;
	step_name: string;
	permission_mode: "none" | "members" | "roles";
	member_ids: number[];
	role_ids: number[];
	required_slot_ids: string[];
	attribute_updates: AttributeUpdateItem[];
	/** 转换前附加属性（补录）；与 required_slot_ids 兼容同步 */
	additional_attributes?: AdditionalAttributeItem[];
	sort_order?: number;
};

export type AttributeUpdateItem = {
	slot_id: string;
	value: unknown;
	value_type: "string" | "number" | "boolean" | "date";
};

export type AdditionalAttributeItem = {
	slot_id: string;
	required: boolean;
	default_mode: "keep" | "set";
	default_value?: unknown;
};

export type SaveTransitionRuleBody = {
	from_state_code: string;
	to_state_code: string;
	step_name: string;
	permission_mode: "none" | "members" | "roles";
	member_ids?: number[];
	role_ids?: number[];
	required_slot_ids?: string[];
	attribute_updates?: AttributeUpdateItem[];
	additional_attributes?: AdditionalAttributeItem[];
};

export type DomainTicketFormSchemaVersionSummary = {
  version_no: number;
  is_current: boolean;
  published_at?: string | null;
  published_by?: string | null;
};

export type DomainTicketFormSchemaVersions = {
  current_version_no?: number | null;
  items: DomainTicketFormSchemaVersionSummary[];
};

export type DomainTicketFormSchemaVersionDetail = {
  version_no: number;
  form_schema: Record<string, unknown> | null;
  published_at?: string | null;
  published_by?: string | null;
};

export type TicketAttributeFieldType = "input" | "select" | "switch" | "date" | "member";

export type TicketAttributeTypeConfig = {
  format?: "text" | "email" | "phone" | "integer" | "decimal";
  multiline?: boolean;
  multiple?: boolean;
  withTime?: boolean;
  unit?: string;
  options?: { label: string; value: string; color?: string; icon?: string }[];
  options_source?: "priority_levels";
  scope_mode?: "auto" | "domain" | "platform";
};

export type TicketAttribute = {
  id: string;
  scope: "platform" | "domain";
  business_domain_id?: string | null;
  name: string;
  description: string;
  field_type: TicketAttributeFieldType;
  type_config: TicketAttributeTypeConfig;
  status: "active" | "disabled" | string;
  sort_order: number;
  is_system: boolean;
  system_key?: string | null;
  source_attribute_id?: string | null;
  created_at?: string | null;
  updated_at?: string | null;
};

export type TicketAttributeList = {
  total: number;
  items: TicketAttribute[];
};

export type TicketStatusDefinitionCategory = "not_started" | "in_progress" | "completed";
export type TicketStatusDefinitionStateType = "in_progress" | "paused" | "terminal";

export type TicketStatusDefinition = {
  id: string;
  scope: "platform" | "domain" | string;
  code: string;
  name: string;
  description: string;
  category: TicketStatusDefinitionCategory;
  state_type: TicketStatusDefinitionStateType;
  status: "active" | "disabled" | string;
  sort_order: number;
  is_system: boolean;
  /** 溯源平台状态 ID；直接创建时为 null */
  source_status_id?: string | null;
  created_at?: string | null;
  updated_at?: string | null;
};

export type TicketStatusDefinitionList = {
  total: number;
  items: TicketStatusDefinition[];
};

export type CreateTicketStatusDefinitionBody = {
  name: string;
  description?: string;
  category: TicketStatusDefinitionCategory;
  code?: string;
};

export type UpdateTicketStatusDefinitionBody = {
  name?: string;
  description?: string;
  category?: TicketStatusDefinitionCategory;
};

export type TeamTemplateItem = {
  id: string;
  ticket_type_id: string;
  ticket_type_code?: string | null;
  ticket_type_name?: string | null;
  sort_order: number;
  include_form_schema: boolean;
  include_workflow: boolean;
  include_description_template: boolean;
};

export type TeamTemplate = {
  id: string;
  code: string;
  name: string;
  description: string;
  icon?: string | null;
  status: "active" | "disabled" | string;
  is_system: boolean;
  sort_order: number;
  version: number;
  items: TeamTemplateItem[];
  created_at?: string | null;
  updated_at?: string | null;
};

export type TeamTemplateList = {
  total: number;
  items: TeamTemplate[];
};

export type TeamTemplateOption = {
  id: string;
  code: string;
  name: string;
  description: string;
  icon?: string | null;
  version: number;
  item_count: number;
};

export type TeamTemplateItemBody = {
  ticket_type_id: number | string;
  sort_order?: number;
  include_form_schema?: boolean;
  include_workflow?: boolean;
  include_description_template?: boolean;
};

export type CreateTeamTemplateBody = {
  /** 不传则由服务端根据名称自动生成 */
  code?: string;
  name: string;
  description?: string;
  icon?: string | null;
  status?: "active" | "disabled";
  /** 不传或空数组均允许（空壳模板） */
  items?: TeamTemplateItemBody[];
};

export type UpdateTeamTemplateBody = {
  name?: string;
  description?: string;
  icon?: string | null;
  status?: "active" | "disabled";
  /** 传入时整表替换；不传则不改关联事项类型 */
  items?: TeamTemplateItemBody[];
};

export type CreateTicketAttributeBody = {
  name: string;
  description?: string;
  field_type: TicketAttributeFieldType;
  type_config?: TicketAttributeTypeConfig;
};

export type UpdateTicketAttributeBody = {
  name?: string;
  description?: string;
  field_type?: TicketAttributeFieldType;
  type_config?: TicketAttributeTypeConfig;
  status?: string;
};

export type TicketAttributeSortOrderItem = {
  id: number;
  sort_order: number;
};

export type TicketAttributeSlotConfig = {
  required?: boolean;
  placeholder?: string;
  visible_to_customer?: boolean;
  default_value?: string;
  display_name?: string;
};

export type TicketAttributeSlot = {
  id: string;
  ticket_type_id: string;
  attribute_id: string;
  attribute: TicketAttribute;
  sort_order: number;
  slot_config: TicketAttributeSlotConfig;
  status: string;
  is_system: boolean;
  system_field_key?: string | null;
};

export type PlatformTicketType = {
  id: string;
  scope: "platform" | "domain" | string;
  code: string;
  name: string;
  description: string;
  icon: string;
  category: string;
  status: string;
  sort_order: number;
  is_system: boolean;
  linked_domain_count: number;
  created_at?: string | null;
  updated_at?: string | null;
};

export type PlatformTicketTypeDetail = PlatformTicketType & {
  description_template_md?: string | null;
  status_flow: TicketStatusFlow | Record<string, unknown> | null;
  form_schema: Record<string, unknown> | null;
  form_schema_draft?: Record<string, unknown> | null;
  form_schema_current_version_no?: number | null;
  form_schema_has_unpublished?: boolean | null;
  transition_rules?: TransitionRule[];
};

export type PlatformTicketTypeList = {
  total: number;
  items: PlatformTicketType[];
};

export type CreatePlatformTicketTypeBody = {
  code?: string;
  name: string;
  description?: string;
  icon: string;
  category?: string;
  template_key?: string;
};

export type UpdatePlatformTicketTypeBody = {
  name?: string;
  description?: string;
  description_template_md?: string | null;
  icon?: string;
  status?: string;
  status_flow?: TicketStatusFlow | Record<string, unknown> | null;
  transition_rules?: SaveTransitionRuleBody[];
};

export type PlatformTicketTypeSortOrderItem = {
  id: number;
  sort_order: number;
};

export type DomainTicketTemplate = {
  id: string;
  domain_id: string;
  name: string;
  type: string;
  type_id: string;
  fields_snapshot?: unknown;
  content?: string | null;
  sort_order?: number | null;
};

export type CreateDomainTicketTypeBody = {
  code?: string;
  name: string;
  description?: string | null;
  icon?: string | null;
  template_key?: string | null;
  status_flow?: TicketStatusFlow | Record<string, unknown> | null;
};

export type UpdateDomainTicketTypeBody = {
	name?: string;
	description?: string | null;
	description_template_md?: string | null;
	icon?: string | null;
	status?: string;
	status_flow?: TicketStatusFlow | Record<string, unknown> | null;
	transition_rules?: SaveTransitionRuleBody[];
};

export type CreateDomainTicketTemplateBody = {
  name: string;
  type: string;
  type_id?: string;
  fields_snapshot?: unknown;
  content?: string;
  sort_order?: number;
};

export type UpdateDomainTicketTemplateBody = {
  name?: string;
  type?: string;
  type_id?: string;
  fields_snapshot?: unknown;
  content?: string;
  sort_order?: number;
};

export type P0DomainCustomer = {
  id: string;
  customer_account_id?: string | null;
  display_name: string;
  login_name?: string | null;
  phone?: string | null;
  email?: string | null;
  real_name?: string | null;
  id_card_no?: string | null;
  status: string;
  source?: string | null;
  activated_at?: string | null;
  tags?: string[] | null;
  created_at?: string | null;
};

/** 更新域客户资料请求：登录名不可修改；id_card_no 提交脱敏回显值（含 *）时应剔除不发送 */
export type UpdateDomainCustomerRequest = {
  display_name: string;
  real_name?: string;
  phone: string;
  email?: string;
  id_card_no?: string;
};

export type P0BatchCreateDomainCustomersResult = {
  added: number;
  skipped: number;
  items: P0DomainCustomer[];
};

/** 管理员重置客户密码响应：一次性展示的随机密码 */
export type ResetDomainCustomerPasswordResponse = {
  password: string;
  must_change_password?: boolean;
};

export type P0AttachmentTargetType = "ticket" | "consultation" | "knowledge";

export type P0AttachmentPresignRequest = {
  file_name: string;
  mime_type: string;
  file_size: number;
  target_type: P0AttachmentTargetType;
  domain_id: string;
};

export type P0AttachmentPresignResponse = {
  attachment_id: string;
  upload_url: string;
  expires_in: number;
};

export type P0AttachmentLocalUploadResponse = {
  attachment_id: string;
  download_url: string;
  storage_type: "object_storage";
};

export const DEMO_DOMAINS: DemoDomain[] = [
  {
    id: 1,
    code: "default",
    name: "默认业务域",
    description: "后端当前默认接入的演示业务域，适合展示工单创建和流转。",
    accent: "#6d5efc",
    supportLine: "07x-1000-1000"
  },
  {
    id: 2,
    code: "online-service",
    name: "在线客服域",
    description: "适合咨询接待、消息回访和快速工单创建。",
    accent: "#0f766e",
    supportLine: "07x-2000-2000"
  },
  {
    id: 3,
    code: "after-sales",
    name: "售后支持域",
    description: "适合演示售后处理、状态更新和复杂工单流转。",
    accent: "#1d4ed8",
    supportLine: "07x-3000-3000"
  }
];
