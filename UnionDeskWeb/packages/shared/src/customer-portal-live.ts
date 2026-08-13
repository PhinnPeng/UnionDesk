import {
  createCustomerMyTicket,
  fetchDomains,
  fetchP0InboxPage,
  fetchP0InboxUnreadCount,
  fetchSatisfactionStatus,
  getCustomerMyTicketDetail,
  listCustomerDomainTicketTypes,
  listCustomerMyTickets,
  login,
  logout,
  markP0InboxMessageRead,
  register,
  replyCustomerMyTicket,
  submitSatisfaction,
  switchDomain,
  withdrawCustomerMyTicket,
  type CustomerTicketRow,
} from "./api";
import {
  clearCustomerPortalLiveSession,
  getCustomerPortalSnapshot,
  hydrateCustomerPortalFromLogin,
  replaceCustomerPortalTicketsForActiveDomain,
  setCustomerPortalLiveTicketTypes,
  syncCustomerPortalActiveDomain,
  type CustomerLoginPayload,
  type CustomerPortalInboxMessage,
  type CustomerPortalSnapshot,
  type CustomerPortalTicket,
  type CustomerPortalTicketStatus,
  type CustomerPortalTypeOption,
  type CustomerRegisterPayload,
  type CustomerTicketCreatePayload,
} from "./customer-portal";
import { loadAuthSession } from "./storage";
import type { CustomerSatisfactionView, LoginResponse, P0InboxMessage } from "./types";

function mapStatus(status: string): CustomerPortalTicketStatus {
  const normalized = status.trim().toLowerCase();
  const allowed: CustomerPortalTicketStatus[] = [
    "open",
    "processing",
    "waiting_customer",
    "resolved",
    "closed",
    "withdrawn",
  ];
  if ((allowed as string[]).includes(normalized)) {
    return normalized as CustomerPortalTicketStatus;
  }
  return "processing";
}

function toIso(value: string | undefined | null): string {
  if (!value) {
    return new Date().toISOString();
  }
  const parsed = Date.parse(value);
  return Number.isFinite(parsed) ? new Date(parsed).toISOString() : String(value);
}

function mapTicketRow(row: CustomerTicketRow, accountId: number): CustomerPortalTicket {
  return {
    id: row.id,
    ticketNo: row.ticketNo,
    accountId,
    domainId: row.businessDomainId,
    typeId: String(row.ticketTypeId),
    typeName: row.ticketTypeName,
    title: row.title,
    description: row.description ?? "",
    status: mapStatus(row.status),
    attachments: [],
    replies: [],
    createdAt: toIso(row.createdAt),
    updatedAt: toIso(row.updatedAt),
    withdrawnAt: mapStatus(row.status) === "withdrawn" ? toIso(row.updatedAt) : null,
  };
}

/** Restore portal snapshot after page refresh when a real JWT session exists. */
export async function restoreCustomerPortalLive(): Promise<CustomerPortalSnapshot | null> {
  const session = loadAuthSession();
  if (!session?.accessToken || session.clientCode !== "ud-customer-web") {
    return null;
  }
  if (session.accessToken.startsWith("cust-at")) {
    return null;
  }
  const domains = await fetchDomains();
  const response: LoginResponse = {
    accessToken: session.accessToken,
    refreshToken: session.refreshToken,
    sid: session.sid ?? "",
    role: session.role ?? "customer",
    clientCode: "ud-customer-web",
    tokenType: "Bearer",
    expiresInSeconds: 3600,
    user: {
      id: session.userId ?? 0,
      username: session.username ?? "customer",
      mobile: null,
      email: null,
      roles: ["customer"],
    },
    accessibleDomains: domains,
    defaultBusinessDomainId: session.businessDomainId ?? domains[0]?.id ?? 0,
    mustChangePassword: Boolean(session.mustChangePassword),
  };
  hydrateCustomerPortalFromLogin(response);
  const domainId = session.businessDomainId;
  if (domainId && domainId > 0) {
    await refreshCustomerTicketsLive(domainId).catch(() => undefined);
    await refreshCustomerTicketTypesLive(domainId).catch(() => undefined);
  }
  return getCustomerPortalSnapshot();
}

export async function loginCustomerLive(payload: CustomerLoginPayload): Promise<{
  snapshot: CustomerPortalSnapshot;
  riskLoginNotified: boolean;
  mustChangePassword: boolean;
}> {
  const response: LoginResponse = await login(
    {
      username: payload.loginName.trim(),
      password: payload.password,
      ...(payload.captchaToken ? { captchaToken: payload.captchaToken } : {}),
    },
    { skipPermissionSnapshot: true, persistMode: "local" },
  );
  hydrateCustomerPortalFromLogin(response);
  const domainId = response.defaultBusinessDomainId;
  if (domainId && domainId > 0) {
    await refreshCustomerTicketsLive(domainId).catch(() => undefined);
    await refreshCustomerTicketTypesLive(domainId).catch(() => undefined);
  }
  return {
    snapshot: getCustomerPortalSnapshot(),
    riskLoginNotified: Boolean(response.riskLoginNotified),
    mustChangePassword: Boolean(response.mustChangePassword),
  };
}

/** 真实注册（注册即登录即入域）：调 register → 按返回会话恢复门户快照 → 刷新工单/类型 */
export async function registerCustomerLive(payload: CustomerRegisterPayload): Promise<CustomerPortalSnapshot> {
  const response = await register(
    {
      loginName: payload.loginName.trim(),
      password: payload.password,
      displayName: payload.displayName.trim(),
      phone: payload.phone.trim(),
      ...(payload.email?.trim() ? { email: payload.email.trim() } : {}),
      ...(payload.domainId != null ? { domainId: payload.domainId } : {}),
      ...(payload.invitationCode?.trim() ? { invitationCode: payload.invitationCode.trim() } : {}),
    },
    { skipPermissionSnapshot: true, persistMode: "local" },
  );
  const domains = await fetchDomains();
  hydrateCustomerPortalFromLogin({
    accessToken: response.accessToken,
    refreshToken: response.refreshToken,
    sid: "",
    role: "customer",
    clientCode: "ud-customer-web",
    tokenType: "Bearer",
    expiresInSeconds: 3600,
    user: {
      id: response.accountId,
      username: payload.loginName.trim(),
      mobile: payload.phone.trim(),
      email: payload.email?.trim() || null,
      roles: ["customer"],
    },
    accessibleDomains: domains,
    defaultBusinessDomainId: payload.domainId ?? 0,
    mustChangePassword: false,
  });
  if (payload.domainId) {
    await refreshCustomerTicketsLive(payload.domainId).catch(() => undefined);
    await refreshCustomerTicketTypesLive(payload.domainId).catch(() => undefined);
  }
  return getCustomerPortalSnapshot();
}

export async function logoutCustomerLive(): Promise<void> {
  try {
    await logout();
  } finally {
    clearCustomerPortalLiveSession();
  }
}

export async function selectCustomerDomainLive(domainId: number): Promise<CustomerPortalSnapshot> {
  await switchDomain({ domainId });
  syncCustomerPortalActiveDomain(domainId);
  await refreshCustomerTicketsLive(domainId);
  await refreshCustomerTicketTypesLive(domainId).catch(() => undefined);
  return getCustomerPortalSnapshot();
}

export async function refreshCustomerTicketsLive(domainId?: number): Promise<CustomerPortalTicket[]> {
  const session = loadAuthSession();
  const resolvedDomainId = domainId ?? session?.businessDomainId ?? undefined;
  const accountId = session?.userId;
  if (resolvedDomainId == null || accountId == null) {
    replaceCustomerPortalTicketsForActiveDomain([]);
    return [];
  }
  const rows = await listCustomerMyTickets(resolvedDomainId);
  const tickets = rows.map((row) => mapTicketRow(row, accountId));
  replaceCustomerPortalTicketsForActiveDomain(tickets);
  return tickets;
}

export async function refreshCustomerTicketTypesLive(domainId?: number): Promise<CustomerPortalTypeOption[]> {
  const session = loadAuthSession();
  const resolvedDomainId = domainId ?? session?.businessDomainId ?? undefined;
  if (resolvedDomainId == null) {
    setCustomerPortalLiveTicketTypes([]);
    return [];
  }
  const rows = await listCustomerDomainTicketTypes(resolvedDomainId);
  const types: CustomerPortalTypeOption[] = rows.map((row) => ({
    id: String(row.id),
    name: row.name,
    description_template_md: row.description ?? null,
    has_description_slot: true,
  }));
  setCustomerPortalLiveTicketTypes(types);
  return types;
}

export async function createCustomerTicketLive(
  payload: CustomerTicketCreatePayload,
): Promise<{ snapshot: CustomerPortalSnapshot; ticketId: number; ticketNo: string }> {
  const session = loadAuthSession();
  const domainId = session?.businessDomainId;
  if (domainId == null) {
    throw new Error("请先选择业务域");
  }
  const typeId = Number(payload.typeId);
  if (!Number.isFinite(typeId) || typeId <= 0) {
    throw new Error("请选择有效的工单类型");
  }
  const created = await createCustomerMyTicket(domainId, {
    ticketTypeId: typeId,
    title: payload.title,
    description: payload.description,
    attachmentIds: payload.attachmentIds,
  });
  await refreshCustomerTicketsLive(domainId);
  return {
    snapshot: getCustomerPortalSnapshot(),
    ticketId: created.id,
    ticketNo: created.ticketNo,
  };
}

export async function getCustomerTicketLive(ticketId: number): Promise<{
  ticket: CustomerPortalTicket;
  version: number;
} | null> {
  const session = loadAuthSession();
  const domainId = session?.businessDomainId;
  const accountId = session?.userId;
  if (domainId == null || accountId == null) {
    return null;
  }
  const detail = await getCustomerMyTicketDetail(domainId, ticketId);
  const ticket = mapTicketRow(detail.ticket, accountId);
  ticket.replies = (detail.replies ?? []).map((reply) => ({
    id: reply.id,
    authorType: reply.senderType === "customer" ? "customer" : reply.senderType === "system" ? "system" : "agent",
    authorName: reply.senderType === "customer" ? "我" : "客服",
    content: reply.content,
    attachmentIds: [],
    createdAt: toIso(reply.createdAt),
  }));
  return { ticket, version: detail.ticket.version };
}

export async function replyCustomerTicketLive(ticketId: number, content: string, version: number): Promise<void> {
  const session = loadAuthSession();
  const domainId = session?.businessDomainId;
  if (domainId == null) {
    throw new Error("请先选择业务域");
  }
  await replyCustomerMyTicket(domainId, ticketId, { version, content });
}

export async function withdrawCustomerTicketLive(
  ticketId: number,
  version: number,
  reason = "客户撤回",
): Promise<void> {
  const session = loadAuthSession();
  const domainId = session?.businessDomainId;
  if (domainId == null) {
    throw new Error("请先选择业务域");
  }
  await withdrawCustomerMyTicket(domainId, ticketId, { version, reason });
  await refreshCustomerTicketsLive(domainId);
}

function mapInboxMessage(item: P0InboxMessage, accountId: number): CustomerPortalInboxMessage {
  const title = item.title ?? "";
  const content = item.content ?? "";
  const isRisk = title.includes("登录环境") || content.includes("新环境登录") || content.includes("security.risk_login");
  return {
    id: Number(item.id) || 0,
    accountId,
    domainId: 0,
    ticketId: null,
    title,
    content,
    jumpUrl: item.jump_url || "/inbox",
    kind: isRisk ? "system" : "ticket",
    isRead: Boolean(item.is_read),
    createdAt: toIso(item.created_at),
  };
}

export async function fetchCustomerInboxLive(): Promise<{
  messages: CustomerPortalInboxMessage[];
  unreadCount: number;
}> {
  const session = loadAuthSession();
  const accountId = session?.userId ?? 0;
  const [page, unreadCount] = await Promise.all([
    fetchP0InboxPage({ page: 1, page_size: 100 }),
    fetchP0InboxUnreadCount(),
  ]);
  const messages = (page.list ?? []).map((item) => mapInboxMessage(item, accountId));
  return {
    messages,
    unreadCount: typeof page.unread_count === "number" ? page.unread_count : unreadCount,
  };
}

export async function markCustomerInboxReadLive(messageId: number): Promise<void> {
  await markP0InboxMessageRead(String(messageId));
}

/** 查询当前域工单的满意度评价状态（未评价返回 null） */
export async function fetchSatisfactionLive(ticketId: number): Promise<CustomerSatisfactionView | null> {
  const session = loadAuthSession();
  const domainId = session?.businessDomainId;
  if (domainId == null) {
    throw new Error("请先选择业务域");
  }
  return fetchSatisfactionStatus(domainId, ticketId);
}

/** 提交当前域工单的满意度评价（后端保证仅一次） */
export async function submitSatisfactionLive(
  ticketId: number,
  payload: { rating: number; comment?: string },
): Promise<CustomerSatisfactionView> {
  const session = loadAuthSession();
  const domainId = session?.businessDomainId;
  if (domainId == null) {
    throw new Error("请先选择业务域");
  }
  return submitSatisfaction(domainId, ticketId, payload);
}
