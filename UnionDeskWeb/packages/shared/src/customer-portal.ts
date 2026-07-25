import { useSyncExternalStore } from "react";

import { getDemoDomains, loadAuthSession, saveAuthSession, clearAuthSession } from "./storage";
import type { AuthSessionState, DemoDomain, P0RegistrationPolicy } from "./types";

type CustomerPortalDomain = DemoDomain & {
  registrationPolicy: P0RegistrationPolicy;
  invitationCode: string;
};

export type CustomerPortalAccount = {
  id: number;
  loginName: string;
  password: string;
  displayName: string;
  phone: string;
  email?: string | null;
  domainIds: number[];
  createdAt: string;
  updatedAt: string;
};

export type CustomerPortalAttachment = {
  id: number;
  ownerAccountId: number;
  domainId: number;
  fileName: string;
  mimeType: string;
  size: number;
  content: string;
  createdAt: string;
};

export type CustomerPortalReply = {
  id: number;
  authorType: "customer" | "agent" | "system";
  authorName: string;
  content: string;
  attachmentIds: number[];
  createdAt: string;
};

export type CustomerPortalTicketStatus = "open" | "processing" | "waiting_customer" | "resolved" | "closed" | "withdrawn";

export type CustomerPortalTicket = {
  id: number;
  ticketNo: string;
  accountId: number;
  domainId: number;
  typeId: string;
  typeName: string;
  title: string;
  description: string;
  status: CustomerPortalTicketStatus;
  attachments: number[];
  replies: CustomerPortalReply[];
  createdAt: string;
  updatedAt: string;
  withdrawnAt?: string | null;
};

export type CustomerPortalInboxMessage = {
  id: number;
  accountId: number;
  domainId: number;
  ticketId?: number | null;
  title: string;
  content: string;
  jumpUrl: string;
  kind: "ticket" | "domain" | "system";
  isRead: boolean;
  createdAt: string;
};

export type CustomerPortalTypeOption = {
  id: string;
  name: string;
  description_template_md?: string | null;
  has_description_slot?: boolean;
};

type CustomerPortalState = {
  nextIds: {
    account: number;
    ticket: number;
    reply: number;
    attachment: number;
    inbox: number;
  };
  accounts: CustomerPortalAccount[];
  domains: CustomerPortalDomain[];
  tickets: CustomerPortalTicket[];
  inbox: CustomerPortalInboxMessage[];
  attachments: CustomerPortalAttachment[];
  activeAccountId: number | null;
  activeDomainId: number | null;
};

export type CustomerPortalDomainView = CustomerPortalDomain & {
  joined: boolean;
  selected: boolean;
  canJoin: boolean;
  joinHint: string;
};

export type CustomerPortalSnapshot = {
  account: CustomerPortalAccount | null;
  activeDomain: CustomerPortalDomainView | null;
  domains: CustomerPortalDomainView[];
  joinedDomains: CustomerPortalDomainView[];
  currentDomainTickets: CustomerPortalTicket[];
  inboxMessages: CustomerPortalInboxMessage[];
  unreadCount: number;
  attachments: CustomerPortalAttachment[];
  ticketTypes: CustomerPortalTypeOption[];
};

export type CustomerLoginPayload = {
  loginName: string;
  password: string;
};

export type CustomerRegisterPayload = {
  loginName: string;
  password: string;
  displayName: string;
  phone: string;
  email?: string;
  domainId?: number | null;
  invitationCode?: string;
};

export type CustomerJoinDomainPayload = {
  invitationCode: string;
};

export type CustomerTicketCreatePayload = {
  typeId: string;
  title: string;
  description: string;
  attachmentIds?: number[];
};

type CustomerPortalAttachmentUploadInput = {
  fileName: string;
  mimeType: string;
  size: number;
  content: string;
};

const STORAGE_KEY = "uniondesk.customer.portal-state.v1";
const ACCOUNT_SESSION_ROLE = "customer";

const ticketTypes: CustomerPortalTypeOption[] = [
  {
    id: "technical",
    name: "技术支持",
    has_description_slot: true,
    description_template_md: "## 问题现象\n\n## 复现步骤\n\n## 期望结果\n",
  },
  {
    id: "account",
    name: "账号问题",
    has_description_slot: true,
    description_template_md: null,
  },
  {
    id: "billing",
    name: "费用与发票",
    has_description_slot: false,
    description_template_md: "## 账单信息\n\n（无 description 槽位时不应预填）\n",
  },
];

let cachedState: CustomerPortalState | null = null;
const listeners = new Set<() => void>();

function nowIso(): string {
  return new Date().toISOString();
}

function createIdPrefix(prefix: string): string {
  return `${prefix}-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 8)}`;
}

function getStorage(): Storage | null {
  if (typeof window === "undefined") {
    return null;
  }
  return window.localStorage;
}

function readState(): CustomerPortalState {
  if (cachedState) {
    return cachedState;
  }
  const storage = getStorage();
  if (storage) {
    const raw = storage.getItem(STORAGE_KEY);
    if (raw) {
      try {
        cachedState = JSON.parse(raw) as CustomerPortalState;
        return cachedState;
      } catch {
        storage.removeItem(STORAGE_KEY);
      }
    }
  }
  cachedState = seedState();
  persistState(cachedState);
  return cachedState;
}

function persistState(state: CustomerPortalState): void {
  cachedState = state;
  const storage = getStorage();
  if (storage) {
    storage.setItem(STORAGE_KEY, JSON.stringify(state));
  }
}

function emitChange(): void {
  for (const listener of listeners) {
    listener();
  }
}

function updateState(updater: (state: CustomerPortalState) => CustomerPortalState): CustomerPortalState {
  const nextState = updater(readState());
  persistState(nextState);
  emitChange();
  return nextState;
}

function seedDomains(): CustomerPortalDomain[] {
  return getDemoDomains().map((domain, index) => ({
    ...domain,
    registrationPolicy: index === 2 ? "admin_only" : index === 1 ? "invitation_only" : "open",
    invitationCode: `INV-${String(domain.id).padStart(4, "0")}`
  }));
}

function seedState(): CustomerPortalState {
  const domains = seedDomains();
  const accountId = 1;
  const createdAt = nowIso();
  const ticketId = 1;
  const attachmentId = 1;
  const replyId = 1;
  const inboxId = 1;
  return {
    nextIds: {
      account: 2,
      ticket: ticketId + 1,
      reply: replyId + 1,
      attachment: attachmentId + 1,
      inbox: inboxId + 1
    },
    accounts: [
      {
        id: accountId,
        loginName: "customer",
        password: "customer123",
        displayName: "演示客户",
        phone: "13800000000",
        email: "customer@uniondesk.com",
        domainIds: [domains[0]?.id ?? 1],
        createdAt,
        updatedAt: createdAt
      }
    ],
    domains,
    tickets: [
      {
        id: ticketId,
        ticketNo: "UDC-20260503-0001",
        accountId,
        domainId: domains[0]?.id ?? 1,
        typeId: "technical",
        typeName: "技术支持",
        title: "演示工单：登录后无法切换业务域",
        description: "这是用于展示客户闭环的演示工单，页面会展示状态、回复、附件与通知。",
        status: "processing",
        attachments: [],
        replies: [
          {
            id: replyId,
            authorType: "system",
            authorName: "系统",
            content: "工单已受理，请耐心等待客服跟进。",
            attachmentIds: [],
            createdAt
          }
        ],
        createdAt,
        updatedAt: createdAt
      }
    ],
    inbox: [
      {
        id: inboxId,
        accountId,
        domainId: domains[0]?.id ?? 1,
        ticketId,
        title: "工单已创建",
        content: "你的示例工单已创建成功，当前状态为处理中。",
        jumpUrl: `/tickets/${ticketId}`,
        kind: "ticket",
        isRead: false,
        createdAt
      }
    ],
    attachments: [],
    activeAccountId: accountId,
    activeDomainId: domains[0]?.id ?? 1
  };
}

function findAccountByLoginName(state: CustomerPortalState, loginName: string): CustomerPortalAccount | null {
  const normalized = loginName.trim().toLowerCase();
  return state.accounts.find((account) => account.loginName.toLowerCase() === normalized) ?? null;
}

function findDomainByInvitationCode(state: CustomerPortalState, invitationCode: string): CustomerPortalDomain | null {
  const normalized = invitationCode.trim().toLowerCase();
  return state.domains.find((domain) => domain.invitationCode.toLowerCase() === normalized) ?? null;
}

function resolveActiveAccount(state: CustomerPortalState): CustomerPortalAccount | null {
  const session = loadAuthSession();
  const loginName = session?.username?.trim().toLowerCase();
  if (loginName) {
    const account = state.accounts.find((item) => item.loginName.toLowerCase() === loginName);
    if (account) {
      return account;
    }
  }
  if (state.activeAccountId != null) {
    return state.accounts.find((item) => item.id === state.activeAccountId) ?? null;
  }
  return null;
}

function resolveActiveDomain(state: CustomerPortalState, account: CustomerPortalAccount | null): CustomerPortalDomain | null {
  if (!account) {
    return null;
  }
  const preferredId = state.activeDomainId ?? account.domainIds[0] ?? null;
  if (preferredId == null) {
    return null;
  }
  return state.domains.find((domain) => domain.id === preferredId) ?? null;
}

function selectDomainForAccount(state: CustomerPortalState, account: CustomerPortalAccount | null, domainId?: number | null): CustomerPortalState {
  if (!account) {
    return state;
  }
  const selectedDomainId = domainId ?? account.domainIds[0] ?? null;
  return {
    ...state,
    activeAccountId: account.id,
    activeDomainId: selectedDomainId
  };
}

function createSession(account: CustomerPortalAccount, domainId?: number | null): AuthSessionState {
  return {
    username: account.loginName,
    accessToken: createIdPrefix("cust-at"),
    refreshToken: createIdPrefix("cust-rt"),
    role: ACCOUNT_SESSION_ROLE,
    clientCode: "ud-customer-web",
    authenticatedAt: nowIso(),
    sid: createIdPrefix("cust-sid"),
    userId: account.id,
    businessDomainId: domainId ?? account.domainIds[0] ?? null,
    expiresAt: new Date(Date.now() + 7 * 24 * 60 * 60_000).toISOString(),
    persistMode: "local"
  };
}

function ensureActiveAccount(state: CustomerPortalState): CustomerPortalAccount {
  const account = resolveActiveAccount(state);
  if (!account) {
    throw new Error("当前没有可用的客户会话，请重新登录");
  }
  return account;
}

function ensureActiveDomain(state: CustomerPortalState, account: CustomerPortalAccount): CustomerPortalDomain {
  const domain = resolveActiveDomain(state, account);
  if (!domain) {
    throw new Error("请先选择业务域");
  }
  return domain;
}

function normalizeCode(code: string): string {
  return code.trim().toLowerCase();
}

function buildPortalSnapshot(state: CustomerPortalState): CustomerPortalSnapshot {
  const account = resolveActiveAccount(state);
  const activeDomain = resolveActiveDomain(state, account);
  const joinedDomainIds = new Set(account?.domainIds ?? []);
  const domains = state.domains.map((domain) => ({
    ...domain,
    joined: joinedDomainIds.has(domain.id),
    selected: domain.id === activeDomain?.id,
    canJoin: domain.registrationPolicy !== "admin_only",
    joinHint:
      domain.registrationPolicy === "open"
        ? "开放注册"
        : domain.registrationPolicy === "invitation_only"
          ? "仅邀请码可入域"
          : "仅管理员可分配"
  }));

  const activeDomainView: CustomerPortalDomainView | null = activeDomain
    ? (domains.find((d) => d.id === activeDomain.id) ?? null)
    : null;

  const currentDomainTickets = activeDomain
    ? [...state.tickets].filter((ticket) => ticket.accountId === account?.id && ticket.domainId === activeDomain.id)
    : [];
  currentDomainTickets.sort((left, right) => Date.parse(right.updatedAt) - Date.parse(left.updatedAt));

  const inboxMessages = [...state.inbox]
    .filter((message) => message.accountId === account?.id)
    .sort((left, right) => Date.parse(right.createdAt) - Date.parse(left.createdAt));

  return {
    account,
    activeDomain: activeDomainView,
    domains,
    joinedDomains: domains.filter((domain) => domain.joined),
    currentDomainTickets,
    inboxMessages,
    unreadCount: inboxMessages.filter((message) => !message.isRead).length,
    attachments: state.attachments.filter((attachment) => attachment.ownerAccountId === account?.id),
    ticketTypes
  };
}

export function useCustomerPortal(): CustomerPortalSnapshot & {
  login: (payload: CustomerLoginPayload) => CustomerPortalSnapshot;
  register: (payload: CustomerRegisterPayload) => CustomerPortalSnapshot;
  logout: () => void;
  selectDomain: (domainId: number) => CustomerPortalSnapshot;
  joinDomainByInvitation: (payload: CustomerJoinDomainPayload) => CustomerPortalSnapshot;
  createTicket: (payload: CustomerTicketCreatePayload) => CustomerPortalSnapshot;
  withdrawTicket: (ticketId: number) => CustomerPortalSnapshot;
  markInboxRead: (messageId: number) => CustomerPortalSnapshot;
  uploadAttachment: (file: File, domainId?: number) => Promise<CustomerPortalSnapshot>;
  getTicketById: (ticketId: number) => CustomerPortalTicket | null;
  getAttachmentById: (attachmentId: number) => CustomerPortalAttachment | null;
} {
  const state = useSyncExternalStore(subscribeCustomerPortal, getCustomerPortalState, getCustomerPortalState);
  const snapshot = buildPortalSnapshot(state);
  return {
    ...snapshot,
    login: (payload) => loginCustomer(payload),
    register: (payload) => registerCustomer(payload),
    logout: logoutCustomer,
    selectDomain: (domainId) => selectCustomerDomain(domainId),
    joinDomainByInvitation: (payload) => joinCustomerDomainByInvitation(payload),
    createTicket: (payload) => createCustomerTicket(payload),
    withdrawTicket: (ticketId) => withdrawCustomerTicket(ticketId),
    markInboxRead: (messageId) => markCustomerInboxRead(messageId),
    uploadAttachment: (file, domainId) => uploadCustomerAttachment(file, domainId),
    getTicketById: (ticketId) => getCustomerTicket(ticketId),
    getAttachmentById: (attachmentId) => getCustomerAttachment(attachmentId)
  };
}

export function subscribeCustomerPortal(listener: () => void): () => void {
  listeners.add(listener);
  return () => {
    listeners.delete(listener);
  };
}

export function getCustomerPortalState(): CustomerPortalState {
  return readState();
}

export function loginCustomer(payload: CustomerLoginPayload): CustomerPortalSnapshot {
  const nextState = updateState((state) => {
    const account = findAccountByLoginName(state, payload.loginName);
    if (!account || account.password !== payload.password) {
      throw new Error("账号或密码错误");
    }
    const updatedState = selectDomainForAccount(state, account, account.domainIds[0] ?? null);
    saveAuthSession(createSession(account, updatedState.activeDomainId ?? null));
    return updatedState;
  });
  return buildPortalSnapshot(nextState);
}

export function registerCustomer(payload: CustomerRegisterPayload): CustomerPortalSnapshot {
  const nextState = updateState((state) => {
    if (findAccountByLoginName(state, payload.loginName)) {
      throw new Error("登录名已存在");
    }
    const existingPhone = state.accounts.find((account) => account.phone === payload.phone.trim());
    if (existingPhone) {
      throw new Error("手机号已被注册");
    }
    const normalizedInvitationCode = payload.invitationCode?.trim();
    const domainFromInvitation = normalizedInvitationCode ? findDomainByInvitationCode(state, normalizedInvitationCode) : null;
    const selectedDomain = payload.domainId ? state.domains.find((domain) => domain.id === payload.domainId) ?? null : null;
    if (selectedDomain?.registrationPolicy === "invitation_only" && !domainFromInvitation) {
      throw new Error("该业务域需要邀请码");
    }
    const domain = domainFromInvitation ?? selectedDomain ?? state.domains.find((item) => item.registrationPolicy === "open") ?? null;
    if (domain?.registrationPolicy === "admin_only" && !domainFromInvitation) {
      throw new Error("该业务域不允许自助注册");
    }
    const accountId = state.nextIds.account;
    const domainIds = domain ? [domain.id] : [];
    const createdAt = nowIso();
    const account: CustomerPortalAccount = {
      id: accountId,
      loginName: payload.loginName.trim(),
      password: payload.password,
      displayName: payload.displayName.trim(),
      phone: payload.phone.trim(),
      email: payload.email?.trim() || null,
      domainIds,
      createdAt,
      updatedAt: createdAt
    };
    const updatedState: CustomerPortalState = {
      ...state,
      nextIds: { ...state.nextIds, account: accountId + 1, inbox: state.nextIds.inbox + 1 },
      accounts: [...state.accounts, account]
    };
    if (domain) {
      updatedState.inbox = [
        {
          id: state.nextIds.inbox,
          accountId,
          domainId: domain.id,
          ticketId: null,
          title: "欢迎加入业务域",
          content: `你已经成功加入「${domain.name}」。`,
          jumpUrl: "/domains",
          kind: "domain",
          isRead: false,
          createdAt
        },
        ...state.inbox
      ];
    }
    const selectedDomainId = domain?.id ?? null;
    updatedState.activeAccountId = accountId;
    updatedState.activeDomainId = selectedDomainId;
    saveAuthSession(createSession(account, selectedDomainId));
    return updatedState;
  });
  return buildPortalSnapshot(nextState);
}

export function logoutCustomer(): void {
  clearAuthSession();
  updateState((state) => ({
    ...state,
    activeAccountId: null,
    activeDomainId: null
  }));
}

export function selectCustomerDomain(domainId: number): CustomerPortalSnapshot {
  const nextState = updateState((state) => {
    const account = ensureActiveAccount(state);
    if (!account.domainIds.includes(domainId)) {
      throw new Error("当前账号尚未加入该业务域");
    }
    const domain = state.domains.find((item) => item.id === domainId);
    if (!domain) {
      throw new Error("业务域不存在");
    }
    saveAuthSession(createSession(account, domainId));
    return {
      ...state,
      activeAccountId: account.id,
      activeDomainId: domainId
    };
  });
  return buildPortalSnapshot(nextState);
}

export function joinCustomerDomainByInvitation(payload: CustomerJoinDomainPayload): CustomerPortalSnapshot {
  const nextState = updateState((state) => {
    const account = ensureActiveAccount(state);
    const domain = findDomainByInvitationCode(state, payload.invitationCode);
    if (!domain) {
      throw new Error("邀请码无效");
    }
    if (domain.registrationPolicy === "admin_only") {
      throw new Error("该业务域不支持邀请码入域");
    }
    const domainIds = account.domainIds.includes(domain.id) ? account.domainIds : [...account.domainIds, domain.id];
    const updatedAccount: CustomerPortalAccount = {
      ...account,
      domainIds,
      updatedAt: nowIso()
    };
    const inboxId = state.nextIds.inbox;
    const createdAt = nowIso();
    const updatedState: CustomerPortalState = {
      ...state,
      nextIds: { ...state.nextIds, inbox: inboxId + 1 },
      accounts: state.accounts.map((item) => (item.id === account.id ? updatedAccount : item)),
      inbox: [
        {
          id: inboxId,
          accountId: account.id,
          domainId: domain.id,
          ticketId: null,
          title: "邀请码入域成功",
          content: `你已经加入「${domain.name}」。`,
          jumpUrl: "/domains",
          kind: "domain",
          isRead: false,
          createdAt
        },
        ...state.inbox
      ],
      activeAccountId: account.id,
      activeDomainId: domain.id
    };
    saveAuthSession(createSession(updatedAccount, domain.id));
    return updatedState;
  });
  return buildPortalSnapshot(nextState);
}

export function createCustomerTicket(payload: CustomerTicketCreatePayload): CustomerPortalSnapshot {
  const nextState = updateState((state) => {
    const account = ensureActiveAccount(state);
    const domain = ensureActiveDomain(state, account);
    const type = ticketTypes.find((item) => item.id === payload.typeId) ?? ticketTypes[0];
    const ticketId = state.nextIds.ticket;
    const replyId = state.nextIds.reply;
    const inboxId = state.nextIds.inbox;
    const now = nowIso();
    const ticketNo = `UDC-${new Date().toISOString().slice(0, 10).replaceAll("-", "")}-${String(ticketId).padStart(4, "0")}`;
    const ticket: CustomerPortalTicket = {
      id: ticketId,
      ticketNo,
      accountId: account.id,
      domainId: domain.id,
      typeId: type.id,
      typeName: type.name,
      title: payload.title.trim(),
      description: payload.description.trim(),
      status: "open",
      attachments: payload.attachmentIds ?? [],
      replies: [
        {
          id: replyId,
          authorType: "system",
          authorName: "系统",
          content: "工单已提交，客服稍后会跟进处理。",
          attachmentIds: [],
          createdAt: now
        },
        {
          id: replyId + 1,
          authorType: "agent",
          authorName: "在线客服",
          content: "我们已收到你的工单，会在稍后回复你。",
          attachmentIds: [],
          createdAt: new Date(Date.now() + 60_000).toISOString()
        }
      ],
      createdAt: now,
      updatedAt: now
    };
    const updatedState: CustomerPortalState = {
      ...state,
      nextIds: {
        ...state.nextIds,
        ticket: ticketId + 1,
        reply: replyId + 2,
        inbox: inboxId + 1
      },
      tickets: [ticket, ...state.tickets],
      inbox: [
        {
          id: inboxId,
          accountId: account.id,
          domainId: domain.id,
          ticketId,
          title: "工单已创建",
          content: `${ticket.ticketNo} 已创建，当前状态为「${ticket.status}」。`,
          jumpUrl: `/tickets/${ticket.id}`,
          kind: "ticket",
          isRead: false,
          createdAt: now
        },
        ...state.inbox
      ],
      activeAccountId: account.id,
      activeDomainId: domain.id
    };
    return updatedState;
  });
  return buildPortalSnapshot(nextState);
}

export function withdrawCustomerTicket(ticketId: number): CustomerPortalSnapshot {
  const nextState = updateState((state) => {
    const account = ensureActiveAccount(state);
    const ticket = state.tickets.find((item) => item.id === ticketId && item.accountId === account.id);
    if (!ticket) {
      throw new Error("工单不存在");
    }
    if (ticket.status !== "open") {
      throw new Error("仅待受理状态的工单可撤回");
    }
    const now = nowIso();
    const updatedTicket: CustomerPortalTicket = {
      ...ticket,
      status: "withdrawn",
      withdrawnAt: now,
      updatedAt: now,
      replies: [
        ...ticket.replies,
        {
          id: state.nextIds.reply,
          authorType: "system",
          authorName: "系统",
          content: "工单已撤回。",
          attachmentIds: [],
          createdAt: now
        }
      ]
    };
    const inboxId = state.nextIds.inbox;
    return {
      ...state,
      nextIds: { ...state.nextIds, reply: state.nextIds.reply + 1, inbox: inboxId + 1 },
      tickets: state.tickets.map((item) => (item.id === ticketId ? updatedTicket : item)),
      inbox: [
        {
          id: inboxId,
          accountId: account.id,
          domainId: ticket.domainId,
          ticketId,
          title: "工单已撤回",
          content: `${ticket.ticketNo} 已撤回。`,
          jumpUrl: `/tickets/${ticket.id}`,
          kind: "ticket",
          isRead: false,
          createdAt: now
        },
        ...state.inbox
      ]
    };
  });
  return buildPortalSnapshot(nextState);
}

export function markCustomerInboxRead(messageId: number): CustomerPortalSnapshot {
  const nextState = updateState((state) => {
    const account = ensureActiveAccount(state);
    return {
      ...state,
      inbox: state.inbox.map((item) => (item.id === messageId && item.accountId === account.id ? { ...item, isRead: true } : item))
    };
  });
  return buildPortalSnapshot(nextState);
}

export function getCustomerTicket(ticketId: number): CustomerPortalTicket | null {
  const state = readState();
  const account = resolveActiveAccount(state);
  if (!account) {
    return null;
  }
  return state.tickets.find((ticket) => ticket.id === ticketId && ticket.accountId === account.id) ?? null;
}

export function getCustomerAttachment(attachmentId: number): CustomerPortalAttachment | null {
  const state = readState();
  const account = resolveActiveAccount(state);
  if (!account) {
    return null;
  }
  return state.attachments.find((attachment) => attachment.id === attachmentId && attachment.ownerAccountId === account.id) ?? null;
}

export function uploadCustomerAttachment(file: File, domainId?: number): Promise<CustomerPortalSnapshot> {
  return new Promise<CustomerPortalSnapshot>((resolve, reject) => {
    const reader = new FileReader();
    reader.onerror = () => reject(new Error("附件读取失败"));
    reader.onload = () => {
      try {
        const nextState = updateState((state) => {
          const account = ensureActiveAccount(state);
          const domain = domainId
            ? state.domains.find((item) => item.id === domainId) ?? null
            : ensureActiveDomain(state, account);
          if (!domain) {
            throw new Error("请先选择业务域");
          }
          const attachmentId = state.nextIds.attachment;
          const attachment: CustomerPortalAttachment = {
            id: attachmentId,
            ownerAccountId: account.id,
            domainId: domain.id,
            fileName: file.name,
            mimeType: file.type || "application/octet-stream",
            size: file.size,
            content: typeof reader.result === "string" ? reader.result : "",
            createdAt: nowIso()
          };
          return {
            ...state,
            nextIds: { ...state.nextIds, attachment: attachmentId + 1 },
            attachments: [attachment, ...state.attachments]
          };
        });
        resolve(buildPortalSnapshot(nextState));
      } catch (error) {
        reject(error);
      }
    };
    reader.readAsDataURL(file);
  });
}

export function listCustomerTicketTypes(): CustomerPortalTypeOption[] {
  return ticketTypes;
}

export function getCustomerPortalSnapshot(): CustomerPortalSnapshot {
  return buildPortalSnapshot(readState());
}
