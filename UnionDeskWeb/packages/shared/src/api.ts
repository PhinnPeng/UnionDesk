import axios from "axios";
import type {
  AdminPermissionCode,
  AuthPublicKeyResponse,
  BackendHealthResponse,
  CaptchaChallengeResponse,
  CaptchaVerifyRequest,
  CaptchaVerifyResponse,
  AuthSessionStatus,
  AuthPersistMode,
  ClientCode,
  ConsultationMessage,
  ConsultationSessionSummary,
  BusinessDomainView,
  CreateIamUserPayload,
  CreateMenuPayload,
  CreateRolePayload,
  CreateTicketRequest,
  DemoTicket,
  IamResource,
  IamRole,
  IamUser,
  MenuTreeNode,
  PermissionSnapshot,
  PlatformOrganizationView,
  LoginConfig,
  LoginRequest,
  LoginResponse,
  LoginLogView,
  OnlineSessionView,
  SessionView,
  UpdateLoginConfigRequest,
  SendConsultationMessagePayload,
  TicketActionResponse,
  TicketRecord,
  RolePermissions,
  UpdateIamUserPayload,
  UpdateMenuPayload,
  UpdateRolePayload,
  UpdateRolePermissionsPayload,
  P0PageResult,
  AdminDomain,
  CreateAdminDomainPayload,
  UpdateAdminDomainPayload,
  P0StepUpRequest,
  P0StepUpResponse,
  P0InboxPageResponse,
  P0AdminTicketListItem,
  P0InvitationCode,
  P0DomainCustomer,
  P0BatchCreateDomainCustomersResult,
  DomainRole,
  DomainPermissionItem,
  DomainRolePermissions,
  DomainMember,
  DomainStaffCandidate,
  BlockedWord,
  BlockedWordBatchResult,
  CreateDomainTicketTemplateBody,
  CreateDomainTicketTypeBody,
  DomainTicketTemplate,
  DomainTicketType,
  DomainTicketFormSchemaVersionDetail,
  DomainTicketFormSchemaVersions,
  CreateTicketAttributeBody,
  CreateTicketStatusDefinitionBody,
  CreatePlatformTicketTypeBody,
  PlatformTicketType,
  PlatformTicketTypeDetail,
  PlatformTicketTypeList,
  PlatformTicketTypeSortOrderItem,
  TicketAttribute,
  TicketAttributeList,
  TicketAttributeSlot,
  TicketAttributeSlotConfig,
  TicketAttributeSortOrderItem,
  TicketStatusDefinition,
  TicketStatusDefinitionList,
  UpdateTicketAttributeBody,
  UpdateTicketStatusDefinitionBody,
  UpdatePlatformTicketTypeBody,
  UpdateDomainTicketTemplateBody,
  UpdateDomainTicketTypeBody,
  P0AttachmentPresignRequest,
  P0AttachmentPresignResponse,
  P0AttachmentLocalUploadResponse,
  P0VisibilityPolicyCode
} from "./types";
import {
  normalizeAdminDomain,
  normalizeAdminDomainsPageResult,
} from "./domain/normalize-admin-domain";
import {
  clearAuthSession,
  loadAuthSession,
  loadAccessToken,
  listMessages,
  listSessions,
  loadPermissionSnapshot,
  mergeTickets,
  saveAuthSession,
  savePermissionSnapshot,
  saveMessage,
  saveTicketMeta,
  seedTicketMetaIfNeeded
} from "./storage";

const api = axios.create({
  baseURL: getApiBaseUrl(),
  timeout: 10_000
});

type ApiEnvelope<T> = {
  code?: number | string;
  success?: boolean;
  message?: unknown;
  data?: T;
};

/** 区分统一响应包装与业务 DTO（如 DomainView 的 code 短码字段） */
function isApiEnvelopePayload(payload: unknown): payload is ApiEnvelope<unknown> {
	if (!payload || typeof payload !== "object" || Array.isArray(payload)) {
		return false;
	}
	const record = payload as Record<string, unknown>;
	if ("success" in record) {
		return true;
	}
	if ("data" in record && ("message" in record || "success" in record)) {
		return true;
	}
	return typeof record.code === "number" && ("message" in record || "data" in record);
}
type RetriableRequestConfig = {
  __retried?: boolean;
  method?: string;
};
type LoginOptions = {
  persistMode?: AuthPersistMode;
};
const CLIENT_CODE_HEADER = "X-UD-Client-Code";
const API_ERROR_CODE_MESSAGES: Record<string, string> = {
  "40102": "客户端标识缺失，请重新登录后再试",
};
let runtimeClientCode: ClientCode | null = null;

export function setClientCode(clientCode: ClientCode): void {
  runtimeClientCode = clientCode;
}

export function getClientCode(): ClientCode | null {
  return runtimeClientCode;
}

type UnauthorizedHandler = () => void;
let unauthorizedHandler: UnauthorizedHandler | null = null;
let unauthorizedNotifyInFlight = false;

/** Host apps register this to navigate to login when shared axios receives HTTP 401. */
export function setUnauthorizedHandler(handler: UnauthorizedHandler | null): void {
  unauthorizedHandler = handler;
}

function notifyUnauthorized(): void {
  if (unauthorizedNotifyInFlight) {
    return;
  }
  unauthorizedNotifyInFlight = true;
  try {
    clearAuthSession();
    unauthorizedHandler?.();
  }
  finally {
    queueMicrotask(() => {
      unauthorizedNotifyInFlight = false;
    });
  }
}

function resolveClientCode(): ClientCode | null {
  const resolved = runtimeClientCode ?? loadAuthSession()?.clientCode ?? null;
  if (resolved) {
    return resolved;
  }
  if (typeof window !== "undefined") {
    const { hostname, port } = window.location;
    if ((hostname === "localhost" || hostname === "127.0.0.1") && (port === "3333" || port === "")) {
      return "ud-admin-web";
    }
  }
  return null;
}

api.interceptors.request.use((config) => {
  const token = loadAccessToken();
  const clientCode = resolveClientCode();
  config.headers = config.headers ?? {};
  if (clientCode) {
    config.headers[CLIENT_CODE_HEADER] = clientCode;
  }
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => {
    const payload = response.data as ApiEnvelope<unknown> | unknown;
    if (isApiEnvelopePayload(payload)) {
      const code = payload.code;
      const isErrorCode =
        (typeof code === "number" && code !== 0) ||
        (typeof code === "string" &&
          code.trim() !== "" &&
          code.trim() !== "0" &&
          code.trim().toUpperCase() !== "SUCCESS" &&
          code.trim().toUpperCase() !== "OK");
      if (isErrorCode) {
        const codeText = String(payload.code ?? "").trim();
        const message = payload.message;
        const resolved = API_ERROR_CODE_MESSAGES[codeText]
          ?? (typeof message === "string" && !message.includes("?") ? message : undefined)
          ?? "Request failed";
        return Promise.reject(new Error(resolved));
      }
    }
    if (isApiEnvelopePayload(payload) && payload.success === false) {
      const code = String((payload as ApiEnvelope<unknown>).code ?? "").trim();
      const message = (payload as ApiEnvelope<unknown>).message;
      const resolved = API_ERROR_CODE_MESSAGES[code]
        ?? (typeof message === "string" && !message.includes("?") ? message : undefined)
        ?? "Request failed";
      return Promise.reject(new Error(resolved));
    }
    return response;
  },
  async (error) => {
    if (axios.isAxiosError(error)) {
      const status = error.response?.status;
      const requestConfig = error.config as (RetriableRequestConfig & object) | undefined;
      const method = requestConfig?.method?.toLowerCase();
      const shouldRetry = method === "get" && !requestConfig?.__retried && (status === undefined || status >= 500);
      if (shouldRetry && requestConfig) {
        requestConfig.__retried = true;
        return api.request(requestConfig);
      }
      if (status === 401) {
        notifyUnauthorized();
      }
    }
    return Promise.reject(error);
  }
);

function getApiBaseUrl(): string {
  if (typeof window !== "undefined") {
    const { hostname } = window.location;
    if (hostname === "localhost" || hostname === "127.0.0.1") {
      return "http://localhost:8080/api/v1";
    }
  }
  return "/api/v1";
}

type AuthPublicKeyState = {
  publicKeyPem: string;
  fetchedAt: number;
};

let cachedAuthPublicKey: AuthPublicKeyState | null = null;
const AUTH_PUBLIC_KEY_TTL_MS = 10 * 60_000;

export async function fetchAuthPublicKey(forceRefresh = false): Promise<string> {
  const now = Date.now();
  if (!forceRefresh && cachedAuthPublicKey && now - cachedAuthPublicKey.fetchedAt < AUTH_PUBLIC_KEY_TTL_MS) {
    return cachedAuthPublicKey.publicKeyPem;
  }
  const response = await api.get<AuthPublicKeyResponse>("/auth/public-key");
  const payload = unwrapApiResponse(response.data);
  const publicKey = (payload as AuthPublicKeyResponse | null | undefined)?.publicKey;
  if (!publicKey || typeof publicKey !== "string" || !publicKey.trim()) {
    throw new Error("Missing auth public key");
  }
  cachedAuthPublicKey = { publicKeyPem: publicKey, fetchedAt: now };
  return publicKey;
}

function toError(error: unknown): Error {
  if (axios.isAxiosError(error)) {
    const responseData = error.response?.data as { message?: unknown } | undefined;
    const message = typeof responseData?.message === "string" ? responseData.message : error.message;
    return new Error(message);
  }
  if (error instanceof Error) {
    return error;
  }
  return new Error(String(error));
}

export function toErrorMessage(error: unknown): string {
  const message = toError(error).message;
  if (/^40102$/.test(message.trim())) {
    return API_ERROR_CODE_MESSAGES["40102"]!;
  }
  if (message.includes("????")) {
    return API_ERROR_CODE_MESSAGES["40102"] ?? "请求失败，请重新登录后再试";
  }
  return message;
}

function unwrapApiResponse<T>(payload: T | { success?: boolean; message?: string; data?: T }): T {
  if (payload && typeof payload === "object" && "data" in payload) {
    return (payload as { data?: T }).data as T;
  }
  return payload as T;
}

export const defaultLoginConfig: LoginConfig = {
  passwordLoginEnabled: true,
  usernameLoginEnabled: true,
  emailLoginEnabled: true,
  mobileLoginEnabled: true,
  captchaEnabled: false,
  wechatLoginEnabled: false,
  wechatLoginUrl: null,
  wechatHint: null,
  captchaHint: null,
  sessionTtlSeconds: 7 * 24 * 60 * 60,
  maxActiveSessionsPerUser: 10,
  updatedAt: null
};

const defaultSessionStatus: AuthSessionStatus = {
  authenticated: false,
  username: null,
  role: null,
  clientCode: null,
  sid: null,
  userId: null,
  businessDomainId: null,
  expiresAt: null
};

export async function fetchHealth(): Promise<BackendHealthResponse> {
  const response = await api.get<BackendHealthResponse>("/health");
  return unwrapApiResponse(response.data);
}

export async function fetchDomains(): Promise<BusinessDomainView[]> {
  try {
    const response = await api.get<{ total: number; items: BusinessDomainView[] }>("/domains");
    const result = unwrapApiResponse(response.data);
    return result?.items ?? [];
  } catch (error) {
    throw toError(error);
  }
}

export async function fetchOrganizations(): Promise<PlatformOrganizationView[]> {
  try {
    const response = await api.get<{ total: number; items: PlatformOrganizationView[] }>("/iam/organizations");
    const result = unwrapApiResponse(response.data);
    return result?.items ?? [];
  } catch (error) {
    throw toError(error);
  }
}

export async function fetchLoginConfig(): Promise<LoginConfig> {
  try {
    const response = await api.get<LoginConfig>("/auth/login-config");
    return {
      ...defaultLoginConfig,
      ...unwrapApiResponse(response.data)
    };
  } catch {
    return defaultLoginConfig;
  }
}

export async function createCaptchaChallenge(): Promise<CaptchaChallengeResponse> {
  try {
    const response = await api.post<CaptchaChallengeResponse>("/auth/captcha/challenge");
    return unwrapApiResponse(response.data);
  } catch (error) {
    throw toError(error);
  }
}

export async function verifyCaptcha(payload: CaptchaVerifyRequest): Promise<CaptchaVerifyResponse> {
  try {
    const response = await api.post<CaptchaVerifyResponse>("/auth/captcha/verify", payload);
    return unwrapApiResponse(response.data);
  } catch (error) {
    throw toError(error);
  }
}

export async function fetchSessionStatus(): Promise<AuthSessionStatus> {
  try {
    const response = await api.get<SessionView>("/auth/session");
    const session = unwrapApiResponse(response.data);
    const authSession = loadAuthSession();
    return {
      ...defaultSessionStatus,
      authenticated: true,
      username: authSession?.username ?? null,
      role: session.role,
      clientCode: session.clientCode,
      sid: session.sid,
      userId: session.userId,
      businessDomainId: session.businessDomainId
    };
  } catch {
    const authSession = loadAuthSession();
    return {
      ...defaultSessionStatus,
      authenticated: false,
      username: authSession?.username ?? null,
      role: authSession?.role ?? null,
      clientCode: authSession?.clientCode ?? null,
      sid: authSession?.sid ?? null,
      userId: authSession?.userId ?? null,
      businessDomainId: authSession?.businessDomainId ?? null,
      expiresAt: authSession?.expiresAt ?? null
    };
  }
}

export async function login(payload: LoginRequest, options?: LoginOptions): Promise<LoginResponse> {
  try {
    const response = await api.post<LoginResponse>("/auth/login", {
      ...payload,
      password: payload.password
    });
    const loginResponse = unwrapApiResponse(response.data);
    const session = saveAuthSession({
      username: loginResponse.user?.username ?? payload.username,
      accessToken: loginResponse.accessToken,
      refreshToken: loginResponse.refreshToken,
      role: loginResponse.role,
      clientCode: loginResponse.clientCode,
      sid: loginResponse.sid,
      userId: loginResponse.user?.id ?? null,
      businessDomainId: loginResponse.defaultBusinessDomainId ?? null,
      expiresAt: new Date(Date.now() + loginResponse.expiresInSeconds * 1000).toISOString(),
      authenticatedAt: new Date().toISOString()
    }, options);
    const snapshot = await fetchPermissionSnapshot();
    savePermissionSnapshot(snapshot, { persistMode: session.persistMode });
    return loginResponse;
  } catch (error) {
    throw toError(error);
  }
}

export async function logout(): Promise<void> {
  try {
    await api.post("/auth/logout");
  } catch (error) {
    if (!(axios.isAxiosError(error) && error.response?.status === 401)) {
      throw toError(error);
    }
  } finally {
    clearAuthSession();
  }
}

export async function updateLoginConfig(payload: UpdateLoginConfigRequest): Promise<LoginConfig> {
  try {
    const response = await api.put<LoginConfig>("/auth/login-config", payload);
    return {
      ...defaultLoginConfig,
      ...unwrapApiResponse(response.data)
    };
  } catch (error) {
    throw toError(error);
  }
}

export async function fetchOnlineSessions(limit = 100): Promise<OnlineSessionView[]> {
  try {
    const response = await api.get<{ total: number; items: OnlineSessionView[] }>("/auth/online-sessions", {
      params: { limit }
    });
    const result = unwrapApiResponse(response.data);
    return result?.items ?? [];
  } catch (error) {
    throw toError(error);
  }
}

export async function revokeOnlineSession(sid: string): Promise<void> {
  try {
    await api.post(`/auth/online-sessions/${encodeURIComponent(sid)}/revoke`);
  } catch (error) {
    throw toError(error);
  }
}

export async function revokeUserSessions(userId: number): Promise<void> {
  try {
    await api.post(`/auth/users/${userId}/revoke-sessions`);
  } catch (error) {
    throw toError(error);
  }
}

export async function fetchLoginLogs(limit = 100): Promise<LoginLogView[]> {
  try {
    const response = await api.get<{ total: number; list: LoginLogView[] }>("/admin/login-logs", {
      params: { page: 1, page_size: limit, event_type: "LOGIN" }
    });
    const page = unwrapApiResponse(response.data);
    return page.list ?? [];
  } catch (error) {
    throw toError(error);
  }
}

export async function fetchTickets(): Promise<DemoTicket[]> {
  try {
    const response = await api.get<{ total: number; items: TicketRecord[] }>("/tickets");
    const result = unwrapApiResponse(response.data);
    const records = result?.items ?? [];
    seedTicketMetaIfNeeded(records.map((ticket: TicketRecord) => ({ ticketNo: ticket.ticketNo })));
    return mergeTickets(records);
  } catch (error) {
    throw toError(error);
  }
}

export async function createTicket(payload: CreateTicketRequest): Promise<DemoTicket> {
  try {
    const response = await api.post<TicketRecord>("/tickets", payload);
    const ticket = unwrapApiResponse(response.data);
    seedTicketMetaIfNeeded([{ ticketNo: ticket.ticketNo }]);
    return mergeTickets([ticket])[0];
  } catch (error) {
    throw toError(error);
  }
}

export async function markTicketProcessing(ticketId: number): Promise<TicketActionResponse> {
  try {
    const response = await api.post<TicketActionResponse>(`/tickets/${ticketId}/processing`);
    return unwrapApiResponse(response.data);
  } catch (error) {
    throw toError(error);
  }
}

export async function markTicketResolved(ticketId: number): Promise<TicketActionResponse> {
  try {
    const response = await api.post<TicketActionResponse>(`/tickets/${ticketId}/resolved`);
    return unwrapApiResponse(response.data);
  } catch (error) {
    throw toError(error);
  }
}

export type UpsertIamResourcePayload = {
  resourceType?: string;
  resourceCode?: string;
  resourceName?: string;
  clientScope?: string;
  httpMethod?: string | null;
  pathPattern?: string | null;
  status?: number;
};

export async function fetchIamResources(params?: {
  resourceType?: string;
  clientScope?: string;
}): Promise<IamResource[]> {
  try {
    const response = await api.get<{ total: number; items: IamResource[] }>("/iam/resources", { params });
    const result = unwrapApiResponse(response.data);
    return result?.items ?? [];
  } catch (error) {
    throw toError(error);
  }
}

export async function createIamResource(payload: UpsertIamResourcePayload): Promise<IamResource> {
  try {
    const response = await api.post<IamResource>("/iam/resources", payload);
    return unwrapApiResponse(response.data);
  } catch (error) {
    throw toError(error);
  }
}

export async function updateIamResource(id: number, payload: UpsertIamResourcePayload): Promise<IamResource> {
  try {
    const response = await api.put<IamResource>(`/iam/resources/${id}`, payload);
    return unwrapApiResponse(response.data);
  } catch (error) {
    throw toError(error);
  }
}

export async function fetchRoleResources(roleId: number): Promise<IamResource[]> {
  try {
    const response = await api.get<{ total: number; items: IamResource[] }>(`/iam/roles/${roleId}/resources`);
    const result = unwrapApiResponse(response.data);
    return result?.items ?? [];
  } catch (error) {
    throw toError(error);
  }
}

export async function replaceRoleResources(roleId: number, resourceIds: number[]): Promise<IamResource[]> {
  try {
    const response = await api.put<{ total: number; items: IamResource[] }>(`/iam/roles/${roleId}/resources`, { resourceIds });
    const result = unwrapApiResponse(response.data);
    return result?.items ?? [];
  } catch (error) {
    throw toError(error);
  }
}

export async function fetchMyMenuResources(): Promise<IamResource[]> {
  try {
    const response = await api.get<{ total: number; items: IamResource[] }>("/iam/me/menu-resources");
    const result = unwrapApiResponse(response.data);
    return result?.items ?? [];
  } catch (error) {
    throw toError(error);
  }
}

export async function fetchPermissionSnapshot(): Promise<PermissionSnapshot> {
  try {
    const response = await api.get<PermissionSnapshot>("/iam/me/permission-snapshot");
    return unwrapApiResponse(response.data);
  } catch (error) {
    throw toError(error);
  }
}

export async function fetchMenusTree(scope?: string): Promise<MenuTreeNode[] | Record<string, MenuTreeNode[]>> {
  try {
    const response = await api.get<MenuTreeNode[] | Record<string, MenuTreeNode[]>>("/iam/menus/tree", {
      params: scope ? { scope } : undefined
    });
    return unwrapApiResponse(response.data);
  } catch (error) {
    throw toError(error);
  }
}

export async function fetchAdminPermissionCodes(): Promise<AdminPermissionCode[]> {
  try {
    const response = await api.get<{ total: number; items: AdminPermissionCode[] }>("/iam/admin-permission-codes");
    const result = unwrapApiResponse(response.data);
    return result?.items ?? [];
  } catch (error) {
    throw toError(error);
  }
}

export async function createMenu(payload: CreateMenuPayload): Promise<IamResource> {
  try {
    const response = await api.post<IamResource>("/iam/menus", payload);
    return unwrapApiResponse(response.data);
  } catch (error) {
    throw toError(error);
  }
}

export async function updateMenu(id: number, payload: UpdateMenuPayload): Promise<IamResource> {
  try {
    const response = await api.put<IamResource>(`/iam/menus/${id}`, payload);
    return unwrapApiResponse(response.data);
  } catch (error) {
    throw toError(error);
  }
}

export async function deleteMenu(id: number): Promise<void> {
  try {
    await api.delete(`/iam/menus/${id}`);
  } catch (error) {
    throw toError(error);
  }
}

export async function fetchRoles(): Promise<IamRole[]> {
  try {
    const response = await api.get<{ total: number; items: IamRole[] }>("/iam/roles");
    const result = unwrapApiResponse(response.data);
    return result?.items ?? [];
  } catch (error) {
    throw toError(error);
  }
}

export async function createRole(payload: CreateRolePayload): Promise<IamRole> {
  try {
    const response = await api.post<IamRole>("/iam/roles", payload);
    return unwrapApiResponse(response.data);
  } catch (error) {
    throw toError(error);
  }
}

export async function updateRole(roleId: number, payload: UpdateRolePayload): Promise<IamRole> {
  try {
    const response = await api.put<IamRole>(`/iam/roles/${roleId}`, payload);
    return unwrapApiResponse(response.data);
  } catch (error) {
    throw toError(error);
  }
}

export async function deleteRole(roleId: number): Promise<void> {
  try {
    await api.delete(`/iam/roles/${roleId}`);
  } catch (error) {
    throw toError(error);
  }
}

export async function fetchRolePermissions(roleId: number): Promise<RolePermissions> {
  try {
    const response = await api.get<RolePermissions>(`/iam/roles/${roleId}/permissions`);
    return unwrapApiResponse(response.data);
  } catch (error) {
    throw toError(error);
  }
}

export async function updateRolePermissions(roleId: number, payload: UpdateRolePermissionsPayload): Promise<RolePermissions> {
  try {
    const response = await api.put<RolePermissions>(`/iam/roles/${roleId}/permissions`, payload);
    return unwrapApiResponse(response.data);
  } catch (error) {
    throw toError(error);
  }
}

export async function fetchUsers(): Promise<IamUser[]> {
  try {
    const response = await api.get<{ total: number; list: IamUser[] }>("/admin/staff", {
      params: { page: 1, page_size: 1000 },
    });
    const result = unwrapApiResponse(response.data);
    return result?.list ?? [];
  } catch (error) {
    throw toError(error);
  }
}

export async function createUser(payload: CreateIamUserPayload): Promise<IamUser> {
  try {
    const response = await api.post<IamUser>("/admin/staff", {
      username: payload.username,
      phone: payload.mobile,
      email: payload.email,
      password: payload.password,
      accountType: payload.accountType,
      roleCodes: payload.roleCodes,
      businessDomainIds: payload.businessDomainIds,
      organizationIds: payload.organizationIds ?? [],
      real_name: payload.remark ?? undefined,
    });
    return unwrapApiResponse(response.data);
  } catch (error) {
    throw toError(error);
  }
}

export async function updateUser(userId: number, payload: UpdateIamUserPayload): Promise<IamUser> {
  try {
    const response = await api.put<IamUser>(`/admin/staff/${userId}`, {
      username: payload.username,
      phone: payload.mobile,
      email: payload.email,
      password: payload.password,
      accountType: payload.accountType,
      roleCodes: payload.roleCodes,
      businessDomainIds: payload.businessDomainIds,
      organizationIds: payload.organizationIds,
      status: payload.status,
      real_name: payload.remark ?? undefined,
    });
    return unwrapApiResponse(response.data);
  } catch (error) {
    throw toError(error);
  }
}

export async function offboardUser(userId: number, reason?: string): Promise<IamUser> {
  try {
    const response = await api.post<IamUser>(`/admin/staff/${userId}/offboard`, { reason });
    return unwrapApiResponse(response.data);
  } catch (error) {
    throw toError(error);
  }
}

export async function restoreUser(userId: number): Promise<IamUser> {
  try {
    const response = await api.post<IamUser>(`/admin/staff/${userId}/restore`);
    return unwrapApiResponse(response.data);
  } catch (error) {
    throw toError(error);
  }
}

export async function fetchOffboardPoolUsers(): Promise<IamUser[]> {
  try {
    const response = await api.get<{ total: number; list: IamUser[] }>("/admin/staff", {
      params: { status: "offboarded", page: 1, page_size: 1000 },
    });
    const result = unwrapApiResponse(response.data);
    return result?.list ?? [];
  } catch (error) {
    throw toError(error);
  }
}

export async function deleteUser(userId: number): Promise<void> {
  try {
    await api.delete(`/admin/staff/${userId}`);
  } catch (error) {
    throw toError(error);
  }
}

function toP0VisibilityList(legacy?: string | null): P0VisibilityPolicyCode[] {
  if (!legacy || legacy.trim() === "") {
    return ["public"];
  }
  const v = legacy.trim();
  if (v === "public" || v === "domain_customer_only" || v === "channel_only") {
    return [v];
  }
  return ["public"];
}

function legacyDomainToAdmin(row: BusinessDomainView): AdminDomain {
  return {
    id: String(row.id),
    code: row.code,
    name: row.name,
    visibility_policy_codes: toP0VisibilityList(row.visibilityPolicy),
    registration_enabled: "allowed",
    invitation_enabled: "allowed",
    status: row.status != null ? String(row.status) : undefined,
    created_at: undefined
  };
}

/** P0：`POST /api/v1/auth/step-up` */
export async function postAuthStepUp(payload: P0StepUpRequest): Promise<P0StepUpResponse> {
  try {
    const response = await api.post<P0StepUpResponse>("/auth/step-up", payload);
    return unwrapApiResponse(response.data);
  } catch (error) {
    throw toError(error);
  }
}

type RequestOptions = {
  stepUpToken?: string;
};

function withStepUpHeaders(options?: RequestOptions): { headers?: Record<string, string> } {
  if (!options?.stepUpToken) {
    return {};
  }
  return { headers: { "X-UD-Step-Up-Token": options.stepUpToken } };
}

/** 业务域管理列表：`GET /api/v1/admin/domains`；404 时回退到演示 `GET /domains` 并映射为分页结构 */
export async function fetchAdminDomainsPage(params: {
  page: number;
  page_size: number;
  status?: string;
  keyword?: string;
  created_from?: string;
  created_to?: string;
}): Promise<P0PageResult<AdminDomain>> {
  try {
    const response = await api.get<P0PageResult<AdminDomain>>("/admin/domains", { params });
    return normalizeAdminDomainsPageResult(unwrapApiResponse(response.data));
  } catch (error) {
    if (axios.isAxiosError(error) && error.response?.status === 404) {
      const legacy = await fetchDomains();
      const list = legacy.map(legacyDomainToAdmin);
      return { total: list.length, list };
    }
    throw toError(error);
  }
}

/** 业务域控制台：`GET /api/v1/admin/domains/{domain_id}` */
export async function fetchAdminDomain(domainId: string): Promise<AdminDomain> {
  try {
    const response = await api.get<AdminDomain>(`/admin/domains/${encodeURIComponent(domainId)}`);
    const row = normalizeAdminDomain(unwrapApiResponse(response.data));
    if (!row) {
      throw new Error("业务域数据格式无效");
    }
    return row;
  } catch (error) {
    if (axios.isAxiosError(error) && error.response?.status === 404) {
      const legacy = await fetchDomains();
      const matched = legacy.find(item => String(item.id) === domainId);
      if (matched) {
        return legacyDomainToAdmin(matched);
      }
    }
    throw toError(error);
  }
}

/** 创建业务域：`POST /api/v1/admin/domains` */
export async function createAdminDomain(payload: CreateAdminDomainPayload): Promise<{ id: string; code: string }> {
  try {
    const response = await api.post<{ id: string; code: string }>("/admin/domains", payload);
    return unwrapApiResponse(response.data);
  } catch (error) {
    throw toError(error);
  }
}

/** 更新业务域：`PUT /api/v1/admin/domains/{domain_id}` */
export async function updateAdminDomain(domainId: string, payload: UpdateAdminDomainPayload): Promise<AdminDomain> {
  try {
    const response = await api.put<AdminDomain>(`/admin/domains/${encodeURIComponent(domainId)}`, payload);
    const row = normalizeAdminDomain(unwrapApiResponse(response.data));
    if (!row) {
      throw new Error("更新业务域失败：响应无效");
    }
    return row;
  } catch (error) {
    throw toError(error);
  }
}

/** 删除业务域：`DELETE /api/v1/admin/domains/{domain_id}`（需 one_time step-up） */
export async function deleteAdminDomain(domainId: string, options?: RequestOptions): Promise<void> {
  try {
    await api.delete(`/admin/domains/${encodeURIComponent(domainId)}`, {
      ...withStepUpHeaders(options)
    });
  } catch (error) {
    throw toError(error);
  }
}

/** P0：`GET /api/v1/admin/domains/{domain_id}/tickets`；404 时回退到演示工单列表 */
export async function fetchP0AdminDomainTicketsPage(params: {
  domainId: string;
  page: number;
  page_size: number;
  status?: string;
  keyword?: string;
}): Promise<P0PageResult<P0AdminTicketListItem>> {
  const { domainId, page, page_size, status, keyword } = params;
  try {
    const response = await api.get<P0PageResult<P0AdminTicketListItem>>(
      `/admin/domains/${encodeURIComponent(domainId)}/tickets`,
      { params: { page, page_size, status, keyword } }
    );
    return unwrapApiResponse(response.data);
  } catch (error) {
    if (axios.isAxiosError(error) && error.response?.status === 404) {
      const rows = await fetchTickets();
      const list: P0AdminTicketListItem[] = rows.map((t) => ({
        id: String(t.id),
        ticket_no: t.ticketNo,
        title: t.title,
        type_name: null,
        status: String(t.status),
        priority: t.priority,
        assignee_name: null,
        sla_status: null,
        created_at: t.createdAt,
        updated_at: t.createdAt
      }));
      return { total: list.length, list };
    }
    throw toError(error);
  }
}

/** P0：`POST /api/v1/admin/domains/{domain_id}/tickets/{ticket_id}/claim` */
export async function claimP0AdminTicket(domainId: string, ticketId: string): Promise<void> {
  try {
    await api.post(`/admin/domains/${encodeURIComponent(domainId)}/tickets/${encodeURIComponent(ticketId)}/claim`);
  } catch (error) {
    throw toError(error);
  }
}

/** P0：`GET /api/v1/inbox`；404 时返回空列表 */
export async function fetchP0InboxPage(params: {
  page: number;
  page_size: number;
  is_read?: boolean;
  domain_id?: string;
}): Promise<P0InboxPageResponse> {
  try {
    const response = await api.get<P0InboxPageResponse>("/inbox", { params });
    return unwrapApiResponse(response.data);
  } catch (error) {
    if (axios.isAxiosError(error) && error.response?.status === 404) {
      return { total: 0, unread_count: 0, list: [] };
    }
    throw toError(error);
  }
}

/** P0：`GET /api/v1/inbox/unread-count` */
export async function fetchP0InboxUnreadCount(): Promise<number> {
  try {
    const response = await api.get<{ count: number }>("/inbox/unread-count");
    const data = unwrapApiResponse(response.data);
    return typeof data.count === "number" ? data.count : 0;
  } catch (error) {
    if (axios.isAxiosError(error) && error.response?.status === 404) {
      return 0;
    }
    throw toError(error);
  }
}

/** P0：`PUT /api/v1/inbox/{message_id}/read` */
export async function markP0InboxMessageRead(messageId: string): Promise<void> {
  try {
    await api.put(`/inbox/${encodeURIComponent(messageId)}/read`);
  } catch (error) {
    throw toError(error);
  }
}

/** P0：`GET .../invitation-codes`；404 返回空 */
export async function fetchP0InvitationCodes(domainId: string): Promise<P0PageResult<P0InvitationCode>> {
  try {
    const response = await api.get<P0PageResult<P0InvitationCode>>(
      `/admin/domains/${encodeURIComponent(domainId)}/invitation-codes`
    );
    return unwrapApiResponse(response.data);
  } catch (error) {
    if (axios.isAxiosError(error) && error.response?.status === 404) {
      return { total: 0, list: [] };
    }
    throw toError(error);
  }
}

/** P0：`GET .../customers`；404 返回空 */
export async function fetchP0DomainCustomersPage(params: {
  domainId: string;
  page: number;
  page_size: number;
  status?: string;
  keyword?: string;
}): Promise<P0PageResult<P0DomainCustomer>> {
  const { domainId, page, page_size, status, keyword } = params;
  try {
    const response = await api.get<P0PageResult<Record<string, unknown>>>(
      `/admin/domains/${encodeURIComponent(domainId)}/customers`,
      { params: { page, page_size, status, keyword } }
    );
    const data = unwrapApiResponse(response.data);
    return {
      total: data.total ?? 0,
      list: Array.isArray(data.list)
        ? data.list.map(item => normalizeP0DomainCustomer(item))
        : [],
    };
  } catch (error) {
    if (axios.isAxiosError(error) && error.response?.status === 404) {
      return { total: 0, list: [] };
    }
    throw toError(error);
  }
}

function normalizeP0DomainCustomer(raw: Record<string, unknown>): P0DomainCustomer {
  return {
    id: String(raw.id ?? ""),
    customer_account_id: raw.customer_account_id != null
      ? String(raw.customer_account_id)
      : raw.customerAccountId != null
        ? String(raw.customerAccountId)
        : null,
    display_name: String(raw.display_name ?? raw.displayName ?? raw.nickname ?? "—"),
    login_name: raw.login_name != null
      ? String(raw.login_name)
      : raw.loginName != null
        ? String(raw.loginName)
        : raw.username != null
          ? String(raw.username)
          : null,
    phone: raw.phone != null ? String(raw.phone) : null,
    email: raw.email != null ? String(raw.email) : null,
    status: raw.status != null ? String(raw.status) : "active",
    source: raw.source != null ? String(raw.source) : null,
    activated_at: raw.activated_at != null
      ? String(raw.activated_at)
      : raw.activatedAt != null
        ? String(raw.activatedAt)
        : null,
    created_at: raw.created_at != null
      ? String(raw.created_at)
      : raw.createdAt != null
        ? String(raw.createdAt)
        : null,
  };
}

/** `GET .../customers/{customerId}` */
export async function fetchDomainCustomer(
  domainId: string,
  customerId: string,
): Promise<P0DomainCustomer> {
  const response = await api.get<Record<string, unknown>>(
    `/admin/domains/${encodeURIComponent(domainId)}/customers/${encodeURIComponent(customerId)}`,
  );
  return normalizeP0DomainCustomer(unwrapApiResponse(response.data) as Record<string, unknown>);
}

/** `POST .../customers/manual` */
export async function createDomainCustomerManual(
  domainId: string,
  body: { display_name: string; login_name: string; phone: string; email: string },
): Promise<P0DomainCustomer> {
  const response = await api.post<Record<string, unknown>>(
    `/admin/domains/${encodeURIComponent(domainId)}/customers/manual`,
    body,
  );
  return normalizeP0DomainCustomer(unwrapApiResponse(response.data) as Record<string, unknown>);
}

/** `POST .../customers/from-staff` */
export async function createDomainCustomersFromStaff(
  domainId: string,
  body: { staff_account_ids: string[] },
): Promise<P0BatchCreateDomainCustomersResult> {
  const response = await api.post<Record<string, unknown>>(
    `/admin/domains/${encodeURIComponent(domainId)}/customers/from-staff`,
    {
      staff_account_ids: body.staff_account_ids.map(id => Number(id)),
    },
  );
  const data = unwrapApiResponse(response.data) as Record<string, unknown>;
  const itemsRaw = data.items;
  return {
    added: Number(data.added ?? 0),
    skipped: Number(data.skipped ?? 0),
    items: Array.isArray(itemsRaw)
      ? itemsRaw.map(item => normalizeP0DomainCustomer(item as Record<string, unknown>))
      : [],
  };
}

/** `PATCH .../customers/{customerId}/status` */
export async function updateDomainCustomerStatus(
  domainId: string,
  customerId: string,
  status: "active" | "disabled",
): Promise<P0DomainCustomer> {
  const response = await api.patch<Record<string, unknown>>(
    `/admin/domains/${encodeURIComponent(domainId)}/customers/${encodeURIComponent(customerId)}/status`,
    { status },
  );
  return normalizeP0DomainCustomer(unwrapApiResponse(response.data) as Record<string, unknown>);
}

function normalizeDomainRole(raw: Record<string, unknown>): DomainRole {
  return {
    id: String(raw.id ?? ""),
    business_domain_id: String(raw.business_domain_id ?? raw.businessDomainId ?? ""),
    code: String(raw.code ?? ""),
    name: String(raw.name ?? ""),
    preset: Boolean(raw.preset),
  };
}

function normalizeDomainMember(raw: Record<string, unknown>): DomainMember {
  const rolesRaw = raw.roles;
  const roles = Array.isArray(rolesRaw)
    ? rolesRaw.map(item => normalizeDomainRole(item as Record<string, unknown>))
    : undefined;
  return {
    id: String(raw.id ?? ""),
    staff_account_id: String(raw.staff_account_id ?? raw.staffAccountId ?? ""),
    business_domain_id: String(raw.business_domain_id ?? raw.businessDomainId ?? ""),
    username: raw.username != null
      ? String(raw.username)
      : raw.login_name != null
        ? String(raw.login_name)
        : raw.loginName != null
          ? String(raw.loginName)
          : null,
    real_name: raw.real_name != null
      ? String(raw.real_name)
      : raw.realName != null
        ? String(raw.realName)
        : null,
    nickname: raw.nickname != null ? String(raw.nickname) : null,
    login_name: raw.login_name != null ? String(raw.login_name) : raw.loginName != null ? String(raw.loginName) : null,
    phone: raw.phone != null ? String(raw.phone) : null,
    email: raw.email != null ? String(raw.email) : null,
    status: raw.status != null ? String(raw.status) : null,
    source: raw.source != null ? String(raw.source) : null,
    activated_at: raw.activated_at != null ? String(raw.activated_at) : raw.activatedAt != null ? String(raw.activatedAt) : null,
    disabled_at: raw.disabled_at != null ? String(raw.disabled_at) : raw.disabledAt != null ? String(raw.disabledAt) : null,
    deleted_at: raw.deleted_at != null ? String(raw.deleted_at) : raw.deletedAt != null ? String(raw.deletedAt) : null,
    created_at: raw.created_at != null ? String(raw.created_at) : raw.createdAt != null ? String(raw.createdAt) : null,
    roles,
  };
}

function normalizeDomainStaffCandidate(raw: Record<string, unknown>): DomainStaffCandidate {
  return {
    id: String(raw.id ?? ""),
    username: raw.username != null ? String(raw.username) : null,
    real_name: raw.real_name != null ? String(raw.real_name) : raw.realName != null ? String(raw.realName) : null,
    nickname: raw.nickname != null ? String(raw.nickname) : null,
    phone: raw.phone != null ? String(raw.phone) : null,
    email: raw.email != null ? String(raw.email) : null,
    status: raw.status != null ? String(raw.status) : null,
  };
}

function normalizeBlockedWord(raw: Record<string, unknown>): BlockedWord {
  return {
    id: String(raw.id ?? ""),
    word: String(raw.word ?? ""),
    created_at: raw.created_at != null ? String(raw.created_at) : raw.createdAt != null ? String(raw.createdAt) : null,
  };
}

function normalizeDomainPermissionItem(raw: Record<string, unknown>): DomainPermissionItem {
  return {
    id: String(raw.id ?? ""),
    code: String(raw.code ?? ""),
    name: String(raw.name ?? ""),
    module: raw.module != null ? String(raw.module) : null,
    type: raw.type != null ? String(raw.type) : null,
  };
}

function normalizeDomainRolePermissions(raw: Record<string, unknown>): DomainRolePermissions {
  const itemsRaw = raw.permission_items ?? raw.permissionItems;
  const permission_items = Array.isArray(itemsRaw)
    ? itemsRaw.map(item => normalizeDomainPermissionItem(item as Record<string, unknown>))
    : [];
  return {
    role_id: String(raw.role_id ?? raw.roleId ?? ""),
    code: String(raw.code ?? ""),
    name: String(raw.name ?? ""),
    permission_items,
  };
}

/** `GET /api/v1/admin/domains/{domainId}/roles`（域内访问，保留） */
export async function fetchDomainRoles(domainId: string): Promise<DomainRole[]> {
  try {
    const response = await api.get<{ total: number; items: DomainRole[] }>(`/admin/domains/${encodeURIComponent(domainId)}/roles`);
    const result = unwrapApiResponse(response.data);
    const data = result?.items ?? [];
    return data.map(item => normalizeDomainRole(item as unknown as Record<string, unknown>));
  } catch (error) {
    if (axios.isAxiosError(error) && error.response?.status === 404) {
      return [];
    }
    throw toError(error);
  }
}

/** `GET /api/v1/admin/domains/{domainId}/platform-roles`（平台控制台） */
export async function fetchPlatformDomainRoles(domainId: string): Promise<DomainRole[]> {
  try {
    const response = await api.get<{ total: number; items: DomainRole[] }>(`/admin/domains/${encodeURIComponent(domainId)}/platform-roles`);
    const result = unwrapApiResponse(response.data);
    const data = result?.items ?? [];
    return data.map(item => normalizeDomainRole(item as unknown as Record<string, unknown>));
  } catch (error) {
    if (axios.isAxiosError(error) && error.response?.status === 404) {
      return [];
    }
    throw toError(error);
  }
}

/** `GET /api/v1/admin/domains/{domainId}/roles/{roleId}/permissions`（域内访问，保留） */
export async function fetchDomainRolePermissions(domainId: string, roleId: string): Promise<DomainRolePermissions> {
  const response = await api.get<DomainRolePermissions>(
    `/admin/domains/${encodeURIComponent(domainId)}/roles/${encodeURIComponent(roleId)}/permissions`,
  );
  return normalizeDomainRolePermissions(unwrapApiResponse(response.data) as Record<string, unknown>);
}

/** `GET /api/v1/admin/domains/{domainId}/platform-roles/{roleId}/permissions`（平台控制台） */
export async function fetchPlatformDomainRolePermissions(domainId: string, roleId: string): Promise<DomainRolePermissions> {
  const response = await api.get<DomainRolePermissions>(
    `/admin/domains/${encodeURIComponent(domainId)}/platform-roles/${encodeURIComponent(roleId)}/permissions`,
  );
  return normalizeDomainRolePermissions(unwrapApiResponse(response.data) as Record<string, unknown>);
}

/** `GET /api/v1/admin/domains/{domainId}/members` */
export async function fetchDomainMembersPage(params: {
  domainId: string;
  page?: number;
  page_size?: number;
  status?: string;
  keyword?: string;
  created_from?: string;
  created_to?: string;
}): Promise<P0PageResult<DomainMember>> {
  const { domainId, page = 1, page_size = 20, status, keyword, created_from, created_to } = params;
  try {
    const response = await api.get<P0PageResult<DomainMember>>(
      `/admin/domains/${encodeURIComponent(domainId)}/members`,
      { params: { page, page_size, status, keyword, created_from, created_to } },
    );
    const data = unwrapApiResponse(response.data);
    return {
      total: data.total ?? 0,
      list: Array.isArray(data.list)
        ? data.list.map(item => normalizeDomainMember(item as unknown as Record<string, unknown>))
        : [],
    };
  } catch (error) {
    if (axios.isAxiosError(error) && error.response?.status === 404) {
      return { total: 0, list: [] };
    }
    throw toError(error);
  }
}

/** `GET /api/v1/admin/domains/{domainId}/members/staff-candidates` */
export async function fetchDomainStaffCandidates(params: {
  domainId: string;
  page?: number;
  page_size?: number;
  keyword?: string;
}): Promise<P0PageResult<DomainStaffCandidate>> {
  const { domainId, page = 1, page_size = 20, keyword } = params;
  const response = await api.get<P0PageResult<DomainStaffCandidate>>(
    `/admin/domains/${encodeURIComponent(domainId)}/members/staff-candidates`,
    { params: { page, page_size, keyword } },
  );
  const data = unwrapApiResponse(response.data);
  return {
    total: data.total ?? 0,
    list: Array.isArray(data.list)
      ? data.list.map(item => normalizeDomainStaffCandidate(item as unknown as Record<string, unknown>))
      : [],
  };
}

/** `POST /api/v1/admin/domains/{domainId}/members` */
export async function createDomainMember(
  domainId: string,
  body: { staff_account_id: string; role_ids: string[] },
): Promise<DomainMember> {
  const response = await api.post<DomainMember>(
    `/admin/domains/${encodeURIComponent(domainId)}/members`,
    {
      staff_account_id: Number(body.staff_account_id),
      role_ids: body.role_ids.map(id => Number(id)),
    },
  );
  return normalizeDomainMember(unwrapApiResponse(response.data) as Record<string, unknown>);
}

/** `POST /api/v1/admin/domains/{domainId}/members/with-staff` */
export async function createDomainMemberWithStaff(
  domainId: string,
  body: {
    username: string;
    real_name?: string;
    nickname?: string;
    phone: string;
    email?: string;
    password: string;
    role_ids: string[];
  },
): Promise<DomainMember> {
  const response = await api.post<DomainMember>(
    `/admin/domains/${encodeURIComponent(domainId)}/members/with-staff`,
    {
      ...body,
      role_ids: body.role_ids.map(id => Number(id)),
    },
  );
  return normalizeDomainMember(unwrapApiResponse(response.data) as Record<string, unknown>);
}

/** `PUT /api/v1/admin/domains/{domainId}/members/{memberId}/roles` */
export async function updateDomainMemberRoles(
  domainId: string,
  memberId: string,
  roleIds: string[],
): Promise<DomainMember> {
  const response = await api.put<DomainMember>(
    `/admin/domains/${encodeURIComponent(domainId)}/members/${encodeURIComponent(memberId)}/roles`,
    { role_ids: roleIds.map(id => Number(id)) },
  );
  return normalizeDomainMember(unwrapApiResponse(response.data) as Record<string, unknown>);
}

/** `PUT /api/v1/admin/domains/{domainId}/members/{memberId}/status` */
export async function updateDomainMemberStatus(
  domainId: string,
  memberId: string,
  status: "active" | "disabled",
): Promise<DomainMember> {
  const response = await api.put<DomainMember>(
    `/admin/domains/${encodeURIComponent(domainId)}/members/${encodeURIComponent(memberId)}/status`,
    { status },
  );
  return normalizeDomainMember(unwrapApiResponse(response.data) as Record<string, unknown>);
}

/** `DELETE /api/v1/admin/domains/{domainId}/members/{memberId}` */
export async function deleteDomainMember(domainId: string, memberId: string): Promise<void> {
  await api.delete(`/admin/domains/${encodeURIComponent(domainId)}/members/${encodeURIComponent(memberId)}`);
}

/** `GET /api/v1/admin/blocked-words` */
export async function fetchPlatformBlockedWordsPage(params: {
  page: number;
  page_size: number;
  keyword?: string;
}): Promise<P0PageResult<BlockedWord>> {
  const { page, page_size, keyword } = params;
  try {
    const response = await api.get<P0PageResult<Record<string, unknown>>>("/admin/blocked-words", {
      params: { page, page_size, keyword },
    });
    const data = unwrapApiResponse(response.data);
    return {
      total: data.total ?? 0,
      list: Array.isArray(data.list)
        ? data.list.map(item => normalizeBlockedWord(item))
        : [],
    };
  } catch (error) {
    if (axios.isAxiosError(error) && error.response?.status === 404) {
      return { total: 0, list: [] };
    }
    throw toError(error);
  }
}

/** `POST /api/v1/admin/blocked-words` */
export async function createPlatformBlockedWord(word: string): Promise<BlockedWord> {
  const response = await api.post<BlockedWord>("/admin/blocked-words", { word });
  return normalizeBlockedWord(unwrapApiResponse(response.data) as unknown as Record<string, unknown>);
}

/** `POST /api/v1/admin/blocked-words/batch` */
export async function createPlatformBlockedWordsBatch(words: string[]): Promise<BlockedWordBatchResult> {
  const response = await api.post<BlockedWordBatchResult>("/admin/blocked-words/batch", { words });
  const data = unwrapApiResponse(response.data) as BlockedWordBatchResult;
  return {
    created_count: data.created_count ?? 0,
    skipped: Array.isArray(data.skipped) ? data.skipped : [],
  };
}

/** `DELETE /api/v1/admin/blocked-words/{wordId}` */
export async function deletePlatformBlockedWord(wordId: string): Promise<void> {
  await api.delete(`/admin/blocked-words/${encodeURIComponent(wordId)}`);
}

/** `GET /api/v1/admin/domains/{domainId}/blocked-words` */
export async function fetchBlockedWordsPage(
  domainId: string,
  params: {
    page: number;
    page_size: number;
    keyword?: string;
  },
): Promise<P0PageResult<BlockedWord>> {
  const { page, page_size, keyword } = params;
  try {
    const response = await api.get<P0PageResult<Record<string, unknown>>>(
      `/admin/domains/${encodeURIComponent(domainId)}/blocked-words`,
      { params: { page, page_size, keyword } },
    );
    const data = unwrapApiResponse(response.data);
    return {
      total: data.total ?? 0,
      list: Array.isArray(data.list)
        ? data.list.map(item => normalizeBlockedWord(item))
        : [],
    };
  } catch (error) {
    if (axios.isAxiosError(error) && error.response?.status === 404) {
      return { total: 0, list: [] };
    }
    throw toError(error);
  }
}

/** @deprecated 使用 fetchBlockedWordsPage */
export async function fetchBlockedWords(domainId: string): Promise<BlockedWord[]> {
  const result = await fetchBlockedWordsPage(domainId, { page: 1, page_size: 200 });
  return result.list;
}

/** `POST /api/v1/admin/domains/{domainId}/blocked-words` */
export async function createBlockedWord(domainId: string, word: string): Promise<BlockedWord> {
  const response = await api.post<BlockedWord>(
    `/admin/domains/${encodeURIComponent(domainId)}/blocked-words`,
    { word },
  );
  return normalizeBlockedWord(unwrapApiResponse(response.data) as unknown as Record<string, unknown>);
}

/** `POST /api/v1/admin/domains/{domainId}/blocked-words/batch` */
export async function createBlockedWordsBatch(domainId: string, words: string[]): Promise<BlockedWordBatchResult> {
  const response = await api.post<BlockedWordBatchResult>(
    `/admin/domains/${encodeURIComponent(domainId)}/blocked-words/batch`,
    { words },
  );
  const data = unwrapApiResponse(response.data) as BlockedWordBatchResult;
  return {
    created_count: data.created_count ?? 0,
    skipped: Array.isArray(data.skipped) ? data.skipped : [],
  };
}

/** `DELETE /api/v1/admin/domains/{domainId}/blocked-words/{wordId}` */
export async function deleteBlockedWord(domainId: string, wordId: string): Promise<void> {
  await api.delete(`/admin/domains/${encodeURIComponent(domainId)}/blocked-words/${encodeURIComponent(wordId)}`);
}

/** `GET /api/v1/admin/domains/{domainId}/ticket-types` */
export async function fetchDomainTicketTypes(domainId: string): Promise<DomainTicketType[]> {
  const response = await api.get<{ total: number; items: DomainTicketType[] }>(
    `/admin/domains/${encodeURIComponent(domainId)}/ticket-types`,
  );
  const result = unwrapApiResponse(response.data);
  return result?.items ?? [];
}

/** `POST /api/v1/admin/domains/{domainId}/ticket-types` */
export async function createDomainTicketType(
  domainId: string,
  body: CreateDomainTicketTypeBody,
): Promise<DomainTicketType> {
  const response = await api.post<DomainTicketType>(
    `/admin/domains/${encodeURIComponent(domainId)}/ticket-types`,
    body,
  );
  return unwrapApiResponse(response.data);
}

/** `PUT /api/v1/admin/domains/{domainId}/ticket-types/{typeId}` */
export async function updateDomainTicketType(
  domainId: string,
  typeId: string,
  body: UpdateDomainTicketTypeBody,
): Promise<DomainTicketType> {
  const response = await api.put<DomainTicketType>(
    `/admin/domains/${encodeURIComponent(domainId)}/ticket-types/${encodeURIComponent(typeId)}`,
    body,
  );
  return unwrapApiResponse(response.data);
}

/** `PUT /api/v1/admin/domains/{domainId}/ticket-types/{typeId}/form-schema/draft` */
export async function saveDomainTicketTypeFormSchemaDraft(
  domainId: string,
  typeId: string,
  form_schema: Record<string, unknown>,
): Promise<DomainTicketType> {
  const response = await api.put<DomainTicketType>(
    `/admin/domains/${encodeURIComponent(domainId)}/ticket-types/${encodeURIComponent(typeId)}/form-schema/draft`,
    { form_schema },
  );
  return unwrapApiResponse(response.data);
}

/** `POST /api/v1/admin/domains/{domainId}/ticket-types/{typeId}/form-schema/publish` */
export async function publishDomainTicketTypeFormSchema(
  domainId: string,
  typeId: string,
  form_schema: Record<string, unknown>,
): Promise<DomainTicketType> {
  const response = await api.post<DomainTicketType>(
    `/admin/domains/${encodeURIComponent(domainId)}/ticket-types/${encodeURIComponent(typeId)}/form-schema/publish`,
    { form_schema },
  );
  return unwrapApiResponse(response.data);
}

/** `GET /api/v1/admin/domains/{domainId}/ticket-types/{typeId}/form-schema/versions` */
export async function fetchDomainTicketTypeFormSchemaVersions(
  domainId: string,
  typeId: string,
): Promise<DomainTicketFormSchemaVersions> {
  const response = await api.get<DomainTicketFormSchemaVersions>(
    `/admin/domains/${encodeURIComponent(domainId)}/ticket-types/${encodeURIComponent(typeId)}/form-schema/versions`,
  );
  return unwrapApiResponse(response.data) ?? { items: [] };
}

/** `GET /api/v1/admin/domains/{domainId}/ticket-types/{typeId}/form-schema/versions/{versionNo}` */
export async function fetchDomainTicketTypeFormSchemaVersion(
  domainId: string,
  typeId: string,
  versionNo: number,
): Promise<DomainTicketFormSchemaVersionDetail> {
  const response = await api.get<DomainTicketFormSchemaVersionDetail>(
    `/admin/domains/${encodeURIComponent(domainId)}/ticket-types/${encodeURIComponent(typeId)}/form-schema/versions/${versionNo}`,
  );
  return unwrapApiResponse(response.data);
}

/** `POST /api/v1/admin/domains/{domainId}/ticket-types/{typeId}/form-schema/versions/{versionNo}/rollback` */
export async function rollbackDomainTicketTypeFormSchemaVersion(
  domainId: string,
  typeId: string,
  versionNo: number,
): Promise<DomainTicketType> {
  const response = await api.post<DomainTicketType>(
    `/admin/domains/${encodeURIComponent(domainId)}/ticket-types/${encodeURIComponent(typeId)}/form-schema/versions/${versionNo}/rollback`,
  );
  return unwrapApiResponse(response.data);
}

/** `DELETE /api/v1/admin/domains/{domainId}/ticket-types/{typeId}` */
export async function deleteDomainTicketType(domainId: string, typeId: string): Promise<void> {
  await api.delete(
    `/admin/domains/${encodeURIComponent(domainId)}/ticket-types/${encodeURIComponent(typeId)}`,
  );
}

/** `GET /api/v1/admin/platform/ticket-attributes` */
export async function fetchPlatformTicketAttributes(params?: {
  keyword?: string;
  page?: number;
  page_size?: number;
}): Promise<TicketAttributeList> {
  const response = await api.get<TicketAttributeList>("/admin/platform/ticket-attributes", { params });
  return unwrapApiResponse(response.data) ?? { total: 0, items: [] };
}

/** `POST /api/v1/admin/platform/ticket-attributes` */
export async function createPlatformTicketAttribute(body: CreateTicketAttributeBody): Promise<TicketAttribute> {
  const response = await api.post<TicketAttribute>("/admin/platform/ticket-attributes", body);
  return unwrapApiResponse(response.data);
}

/** `PUT /api/v1/admin/platform/ticket-attributes/{attributeId}` */
export async function updatePlatformTicketAttribute(
  attributeId: string,
  body: UpdateTicketAttributeBody,
): Promise<TicketAttribute> {
  const response = await api.put<TicketAttribute>(
    `/admin/platform/ticket-attributes/${encodeURIComponent(attributeId)}`,
    body,
  );
  return unwrapApiResponse(response.data);
}

/** `DELETE /api/v1/admin/platform/ticket-attributes/{attributeId}` */
export async function deletePlatformTicketAttribute(attributeId: string): Promise<void> {
  await api.delete(`/admin/platform/ticket-attributes/${encodeURIComponent(attributeId)}`);
}

/** `PUT /api/v1/admin/platform/ticket-attributes/reorder` */
export async function reorderPlatformTicketAttributes(orders: TicketAttributeSortOrderItem[]): Promise<void> {
  await api.put("/admin/platform/ticket-attributes/reorder", { orders });
}

/** `GET /api/v1/platform/ticket-statuses` */
export async function fetchPlatformTicketStatuses(params?: {
  keyword?: string;
  page?: number;
  page_size?: number;
}): Promise<TicketStatusDefinitionList> {
  const response = await api.get<TicketStatusDefinitionList>("/platform/ticket-statuses", { params });
  return unwrapApiResponse(response.data) ?? { total: 0, items: [] };
}

/** `POST /api/v1/platform/ticket-statuses` */
export async function createPlatformTicketStatus(body: CreateTicketStatusDefinitionBody): Promise<TicketStatusDefinition> {
  const response = await api.post<TicketStatusDefinition>("/platform/ticket-statuses", body);
  return unwrapApiResponse(response.data);
}

/** `PUT /api/v1/platform/ticket-statuses/{statusId}` */
export async function updatePlatformTicketStatus(
  statusId: string,
  body: UpdateTicketStatusDefinitionBody,
): Promise<TicketStatusDefinition> {
  const response = await api.put<TicketStatusDefinition>(
    `/platform/ticket-statuses/${encodeURIComponent(statusId)}`,
    body,
  );
  return unwrapApiResponse(response.data);
}

/** `DELETE /api/v1/platform/ticket-statuses/{statusId}` */
export async function deletePlatformTicketStatus(statusId: string): Promise<void> {
  await api.delete(`/platform/ticket-statuses/${encodeURIComponent(statusId)}`);
}

/** `GET /api/v1/admin/platform/ticket-types/{typeId}` */
export async function fetchPlatformTicketType(typeId: string): Promise<PlatformTicketTypeDetail> {
  const response = await api.get<PlatformTicketTypeDetail>(
    `/admin/platform/ticket-types/${encodeURIComponent(typeId)}`,
  );
  return unwrapApiResponse(response.data);
}

/** `GET /api/v1/admin/platform/ticket-types` */
export async function fetchPlatformTicketTypes(params?: {
  keyword?: string;
  page?: number;
  page_size?: number;
}): Promise<PlatformTicketTypeList> {
  const response = await api.get<PlatformTicketTypeList>("/admin/platform/ticket-types", { params });
  return unwrapApiResponse(response.data) ?? { total: 0, items: [] };
}

/** `POST /api/v1/admin/platform/ticket-types` */
export async function createPlatformTicketType(body: CreatePlatformTicketTypeBody): Promise<PlatformTicketType> {
  const response = await api.post<PlatformTicketType>("/admin/platform/ticket-types", body);
  return unwrapApiResponse(response.data);
}

/** `PUT /api/v1/admin/platform/ticket-types/{typeId}` */
export async function updatePlatformTicketType(
  typeId: string,
  body: UpdatePlatformTicketTypeBody,
): Promise<PlatformTicketType> {
  const response = await api.put<PlatformTicketType>(
    `/admin/platform/ticket-types/${encodeURIComponent(typeId)}`,
    body,
  );
  return unwrapApiResponse(response.data);
}

/** `DELETE /api/v1/admin/platform/ticket-types/{typeId}` */
export async function deletePlatformTicketType(typeId: string): Promise<void> {
  await api.delete(`/admin/platform/ticket-types/${encodeURIComponent(typeId)}`);
}

/** `PUT /api/v1/admin/platform/ticket-types/reorder` */
export async function reorderPlatformTicketTypes(orders: PlatformTicketTypeSortOrderItem[]): Promise<void> {
  await api.put("/admin/platform/ticket-types/reorder", { orders });
}

/** `GET /api/v1/admin/platform/ticket-types/{typeId}/attribute-slots` */
export async function fetchPlatformTicketAttributeSlots(typeId: string): Promise<TicketAttributeSlot[]> {
  const response = await api.get<{ total: number; items: TicketAttributeSlot[] }>(
    `/admin/platform/ticket-types/${encodeURIComponent(typeId)}/attribute-slots`,
  );
  const result = unwrapApiResponse(response.data);
  return result?.items ?? [];
}

/** `POST /api/v1/admin/platform/ticket-types/{typeId}/attribute-slots` */
export async function insertPlatformTicketAttributeSlot(
  typeId: string,
  attributeId: string,
  slotConfig?: Partial<TicketAttributeSlotConfig>,
): Promise<TicketAttributeSlot> {
  const response = await api.post<TicketAttributeSlot>(
    `/admin/platform/ticket-types/${encodeURIComponent(typeId)}/attribute-slots`,
    {
      attribute_id: Number(attributeId),
      ...(slotConfig ? { slot_config: slotConfig } : {}),
    },
  );
  return unwrapApiResponse(response.data);
}

/** `PUT /api/v1/admin/platform/ticket-types/{typeId}/attribute-slots/{slotId}` */
export async function updatePlatformTicketAttributeSlot(
  typeId: string,
  slotId: string,
  slotConfig: TicketAttributeSlotConfig,
): Promise<TicketAttributeSlot> {
  const response = await api.put<TicketAttributeSlot>(
    `/admin/platform/ticket-types/${encodeURIComponent(typeId)}/attribute-slots/${encodeURIComponent(slotId)}`,
    { slot_config: slotConfig },
  );
  return unwrapApiResponse(response.data);
}

/** `DELETE /api/v1/admin/platform/ticket-types/{typeId}/attribute-slots/{slotId}` */
export async function removePlatformTicketAttributeSlot(typeId: string, slotId: string): Promise<void> {
  await api.delete(
    `/admin/platform/ticket-types/${encodeURIComponent(typeId)}/attribute-slots/${encodeURIComponent(slotId)}`,
  );
}

/** `PUT /api/v1/admin/platform/ticket-types/{typeId}/attribute-slots/reorder` */
export async function reorderPlatformTicketAttributeSlots(
  typeId: string,
  orders: TicketAttributeSortOrderItem[],
): Promise<void> {
  await api.put(
    `/admin/platform/ticket-types/${encodeURIComponent(typeId)}/attribute-slots/reorder`,
    { orders },
  );
}

/** `POST /api/v1/admin/platform/ticket-types/{typeId}/form-release/draft` */
export async function savePlatformTicketFormReleaseDraft(typeId: string): Promise<PlatformTicketTypeDetail> {
  const response = await api.post<PlatformTicketTypeDetail>(
    `/admin/platform/ticket-types/${encodeURIComponent(typeId)}/form-release/draft`,
  );
  return unwrapApiResponse(response.data);
}

/** `POST /api/v1/admin/platform/ticket-types/{typeId}/form-release/publish` */
export async function publishPlatformTicketFormRelease(typeId: string): Promise<PlatformTicketTypeDetail> {
  const response = await api.post<PlatformTicketTypeDetail>(
    `/admin/platform/ticket-types/${encodeURIComponent(typeId)}/form-release/publish`,
  );
  return unwrapApiResponse(response.data);
}

/** `GET /api/v1/admin/domains/{domainId}/ticket-attributes` */
export async function fetchDomainTicketAttributes(
  domainId: string,
  params?: { keyword?: string; page?: number; page_size?: number },
): Promise<TicketAttributeList> {
  const response = await api.get<TicketAttributeList>(
    `/admin/domains/${encodeURIComponent(domainId)}/ticket-attributes`,
    { params },
  );
  return unwrapApiResponse(response.data) ?? { total: 0, items: [] };
}

/** `POST /api/v1/admin/domains/{domainId}/ticket-attributes` */
export async function createDomainTicketAttribute(
  domainId: string,
  body: CreateTicketAttributeBody,
): Promise<TicketAttribute> {
  const response = await api.post<TicketAttribute>(
    `/admin/domains/${encodeURIComponent(domainId)}/ticket-attributes`,
    body,
  );
  return unwrapApiResponse(response.data);
}

/** `PUT /api/v1/admin/domains/{domainId}/ticket-attributes/{attributeId}` */
export async function updateDomainTicketAttribute(
  domainId: string,
  attributeId: string,
  body: UpdateTicketAttributeBody,
): Promise<TicketAttribute> {
  const response = await api.put<TicketAttribute>(
    `/admin/domains/${encodeURIComponent(domainId)}/ticket-attributes/${encodeURIComponent(attributeId)}`,
    body,
  );
  return unwrapApiResponse(response.data);
}

/** `DELETE /api/v1/admin/domains/{domainId}/ticket-attributes/{attributeId}` */
export async function deleteDomainTicketAttribute(domainId: string, attributeId: string): Promise<void> {
  await api.delete(
    `/admin/domains/${encodeURIComponent(domainId)}/ticket-attributes/${encodeURIComponent(attributeId)}`,
  );
}

/** `PUT /api/v1/admin/domains/{domainId}/ticket-attributes/reorder` */
export async function reorderDomainTicketAttributes(
  domainId: string,
  orders: TicketAttributeSortOrderItem[],
): Promise<void> {
  await api.put(`/admin/domains/${encodeURIComponent(domainId)}/ticket-attributes/reorder`, { orders });
}

/** `GET /api/v1/admin/domains/{domainId}/ticket-types/{typeId}/attribute-slots` */
export async function fetchTicketAttributeSlots(domainId: string, typeId: string): Promise<TicketAttributeSlot[]> {
  const response = await api.get<{ total: number; items: TicketAttributeSlot[] }>(
    `/admin/domains/${encodeURIComponent(domainId)}/ticket-types/${encodeURIComponent(typeId)}/attribute-slots`,
  );
  const result = unwrapApiResponse(response.data);
  return result?.items ?? [];
}

/** `POST /api/v1/admin/domains/{domainId}/ticket-types/{typeId}/attribute-slots` */
export async function insertTicketAttributeSlot(
  domainId: string,
  typeId: string,
  attributeId: string,
  slotConfig?: Partial<TicketAttributeSlotConfig>,
): Promise<TicketAttributeSlot> {
  const response = await api.post<TicketAttributeSlot>(
    `/admin/domains/${encodeURIComponent(domainId)}/ticket-types/${encodeURIComponent(typeId)}/attribute-slots`,
    {
      attribute_id: Number(attributeId),
      ...(slotConfig ? { slot_config: slotConfig } : {}),
    },
  );
  return unwrapApiResponse(response.data);
}

/** `PUT /api/v1/admin/domains/{domainId}/ticket-types/{typeId}/attribute-slots/{slotId}` */
export async function updateTicketAttributeSlot(
  domainId: string,
  typeId: string,
  slotId: string,
  slotConfig: TicketAttributeSlotConfig,
): Promise<TicketAttributeSlot> {
  const response = await api.put<TicketAttributeSlot>(
    `/admin/domains/${encodeURIComponent(domainId)}/ticket-types/${encodeURIComponent(typeId)}/attribute-slots/${encodeURIComponent(slotId)}`,
    { slot_config: slotConfig },
  );
  return unwrapApiResponse(response.data);
}

/** `DELETE /api/v1/admin/domains/{domainId}/ticket-types/{typeId}/attribute-slots/{slotId}` */
export async function removeTicketAttributeSlot(domainId: string, typeId: string, slotId: string): Promise<void> {
  await api.delete(
    `/admin/domains/${encodeURIComponent(domainId)}/ticket-types/${encodeURIComponent(typeId)}/attribute-slots/${encodeURIComponent(slotId)}`,
  );
}

/** `PUT /api/v1/admin/domains/{domainId}/ticket-types/{typeId}/attribute-slots/reorder` */
export async function reorderTicketAttributeSlots(
  domainId: string,
  typeId: string,
  orders: TicketAttributeSortOrderItem[],
): Promise<void> {
  await api.put(
    `/admin/domains/${encodeURIComponent(domainId)}/ticket-types/${encodeURIComponent(typeId)}/attribute-slots/reorder`,
    { orders },
  );
}

/** `POST /api/v1/admin/domains/{domainId}/ticket-types/{typeId}/form-release/draft` */
export async function saveTicketFormReleaseDraft(domainId: string, typeId: string): Promise<DomainTicketType> {
  const response = await api.post<DomainTicketType>(
    `/admin/domains/${encodeURIComponent(domainId)}/ticket-types/${encodeURIComponent(typeId)}/form-release/draft`,
  );
  return unwrapApiResponse(response.data);
}

/** `POST /api/v1/admin/domains/{domainId}/ticket-types/{typeId}/form-release/publish` */
export async function publishTicketFormRelease(domainId: string, typeId: string): Promise<DomainTicketType> {
  const response = await api.post<DomainTicketType>(
    `/admin/domains/${encodeURIComponent(domainId)}/ticket-types/${encodeURIComponent(typeId)}/form-release/publish`,
  );
  return unwrapApiResponse(response.data);
}

/** `GET /api/v1/admin/domains/{domainId}/ticket-templates` */
export async function fetchDomainTicketTemplates(domainId: string): Promise<DomainTicketTemplate[]> {
  const response = await api.get<{ total: number; items: DomainTicketTemplate[] }>(
    `/admin/domains/${encodeURIComponent(domainId)}/ticket-templates`,
  );
  const result = unwrapApiResponse(response.data);
  return result?.items ?? [];
}

/** `POST /api/v1/admin/domains/{domainId}/ticket-templates` */
export async function createDomainTicketTemplate(
  domainId: string,
  body: CreateDomainTicketTemplateBody,
): Promise<DomainTicketTemplate> {
  const response = await api.post<DomainTicketTemplate>(
    `/admin/domains/${encodeURIComponent(domainId)}/ticket-templates`,
    body,
  );
  return unwrapApiResponse(response.data);
}

/** `PUT /api/v1/admin/domains/{domainId}/ticket-templates/{templateId}` */
export async function updateDomainTicketTemplate(
  domainId: string,
  templateId: string,
  body: UpdateDomainTicketTemplateBody,
): Promise<DomainTicketTemplate> {
  const response = await api.put<DomainTicketTemplate>(
    `/admin/domains/${encodeURIComponent(domainId)}/ticket-templates/${encodeURIComponent(templateId)}`,
    body,
  );
  return unwrapApiResponse(response.data);
}

/** `DELETE /api/v1/admin/domains/{domainId}/ticket-templates/{templateId}` */
export async function deleteDomainTicketTemplate(domainId: string, templateId: string): Promise<void> {
  await api.delete(
    `/admin/domains/${encodeURIComponent(domainId)}/ticket-templates/${encodeURIComponent(templateId)}`,
  );
}

/** P0：`POST /api/v1/attachments/presign` */
export async function presignP0Attachment(payload: P0AttachmentPresignRequest): Promise<P0AttachmentPresignResponse> {
  try {
    const response = await api.post<P0AttachmentPresignResponse>("/attachments/presign", payload);
    return unwrapApiResponse(response.data);
  } catch (error) {
    throw toError(error);
  }
}

/** P0：`POST /api/v1/attachments/upload`（multipart，服务端代理写入 MinIO） */
export async function uploadP0AttachmentLocal(
  form: FormData
): Promise<P0AttachmentLocalUploadResponse> {
  try {
    const response = await api.post<P0AttachmentLocalUploadResponse>("/attachments/upload", form);
    return unwrapApiResponse(response.data);
  } catch (error) {
    throw toError(error);
  }
}

/** P0：`PUT /api/v1/attachments/{attachment_id}/confirm` */
export async function confirmP0Attachment(attachmentId: string): Promise<void> {
  try {
    await api.put(`/attachments/${encodeURIComponent(attachmentId)}/confirm`);
  } catch (error) {
    throw toError(error);
  }
}

export function getCachedPermissionSnapshot(): PermissionSnapshot | null {
  return loadPermissionSnapshot();
}

export function loadConsultationSessions(domainId: number, customerId?: number): ConsultationSessionSummary[] {
  return listSessions(domainId, customerId);
}

export function loadConsultationMessages(sessionNo: string): ConsultationMessage[] {
  return listMessages(sessionNo);
}

export function sendConsultationMessage(payload: SendConsultationMessagePayload): string {
  const session = saveMessage(payload);
  return session.sessionNo;
}

export { clearAuthSession, saveTicketMeta };
