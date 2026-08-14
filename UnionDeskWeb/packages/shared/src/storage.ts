import {
  type AuthSessionState,
  type AuthPersistMode,
  DEMO_DOMAINS,
  type AdminProfile,
  type ConsultationMessage,
  type ConsultationSessionSummary,
  type DemoProfile,
  type PermissionSnapshot,
  type DemoTicket,
  type TicketMeta
} from "./types";

const TICKET_META_KEY = "uniondesk.demo.ticket-meta";
const CONSULTATION_STATE_KEY = "uniondesk.demo.consultation-state";
const CUSTOMER_PROFILE_KEY = "uniondesk.demo.customer-profile";
const ADMIN_PROFILE_KEY = "uniondesk.demo.admin-profile";
const AUTH_SESSION_KEY = "uniondesk.auth.session";
const AUTH_TOKEN_KEY = "uniondesk.auth.token";
const PERMISSION_SNAPSHOT_KEY = "uniondesk.auth.permission-snapshot";

const seedMeta: Record<string, TicketMeta> = {
  T202604190001: {
    businessDomainId: "1",
    customerId: "1",
    ticketTypeId: "1",
    priority: "normal",
    description: "Default seed ticket for the demo backend."
  }
};

type DemoConsultationState = {
  sessions: ConsultationSessionSummary[];
  messages: Record<string, ConsultationMessage[]>;
};

type TicketMetaStore = Record<string, TicketMeta>;

type StorageLike = Pick<Storage, "getItem" | "setItem" | "removeItem">;
type PersistedAuthSessionState = Omit<AuthSessionState, "accessToken" | "refreshToken">;
type LegacyPersistedAuthSessionState = PersistedAuthSessionState &
  Partial<Pick<AuthSessionState, "accessToken" | "refreshToken">>;
type PersistedAuthTokenState = {
  accessToken: string;
  refreshToken: string;
};
type SaveAuthSessionOptions = {
  persistMode?: AuthPersistMode;
};
type SavePermissionSnapshotOptions = {
  persistMode?: AuthPersistMode;
};

const memoryStore = new Map<string, string>();
let inMemoryAccessToken = "";
let inMemoryRefreshToken = "";
const memoryStorage: StorageLike = {
  getItem(key: string) {
    return memoryStore.get(key) ?? null;
  },
  setItem(key: string, value: string) {
    memoryStore.set(key, value);
  },
  removeItem(key: string) {
    memoryStore.delete(key);
  }
};

function normalizePersistMode(mode?: AuthPersistMode): AuthPersistMode {
  return mode === "session" ? "session" : "local";
}

function getStorage(mode: AuthPersistMode = "local"): StorageLike {
  if (typeof window !== "undefined") {
    if (mode === "session" && window.sessionStorage) {
      return window.sessionStorage;
    }
    if (mode === "local" && window.localStorage) {
      return window.localStorage;
    }
  }
  return memoryStorage;
}

function readJsonFromStorage<T>(storage: StorageLike, key: string, fallback: T): T {
  const raw = storage.getItem(key);
  if (!raw) {
    return fallback;
  }
  try {
    return JSON.parse(raw) as T;
  } catch {
    return fallback;
  }
}

function writeJsonToStorage<T>(storage: StorageLike, key: string, value: T): void {
  storage.setItem(key, JSON.stringify(value));
}

function removeJsonFromStorage(storage: StorageLike, key: string): void {
  storage.removeItem(key);
}

function readJson<T>(key: string, fallback: T, mode: AuthPersistMode = "local"): T {
  return readJsonFromStorage(getStorage(mode), key, fallback);
}

function writeJson<T>(key: string, value: T, mode: AuthPersistMode = "local"): void {
  writeJsonToStorage(getStorage(mode), key, value);
}

function clearAuthStorageByMode(mode: AuthPersistMode, includePermissionSnapshot: boolean): void {
  const storage = getStorage(mode);
  removeJsonFromStorage(storage, AUTH_SESSION_KEY);
  removeJsonFromStorage(storage, AUTH_TOKEN_KEY);
  if (includePermissionSnapshot) {
    removeJsonFromStorage(storage, PERMISSION_SNAPSHOT_KEY);
  }
}

function clearAllPersistedAuth(includePermissionSnapshot: boolean): void {
  clearAuthStorageByMode("local", includePermissionSnapshot);
  clearAuthStorageByMode("session", includePermissionSnapshot);
}

function persistAuthTokens(mode: AuthPersistMode, accessToken: string, refreshToken: string): void {
  writeJsonToStorage<PersistedAuthTokenState>(getStorage(mode), AUTH_TOKEN_KEY, {
    accessToken,
    refreshToken
  });
}

function readPersistedAuthFromMode(mode: AuthPersistMode): AuthSessionState | null {
  const storage = getStorage(mode);
  const persistedSession = readJsonFromStorage<LegacyPersistedAuthSessionState | null>(storage, AUTH_SESSION_KEY, null);
  if (!persistedSession) {
    return null;
  }
  const persistedTokens = readJsonFromStorage<PersistedAuthTokenState | null>(storage, AUTH_TOKEN_KEY, null);
  const accessToken = (persistedTokens?.accessToken ?? persistedSession.accessToken ?? "").trim();
  const refreshToken = (persistedTokens?.refreshToken ?? persistedSession.refreshToken ?? "").trim();
  if (!persistedSession.clientCode || !accessToken || !refreshToken) {
    clearAuthStorageByMode(mode, true);
    return null;
  }
  return {
    ...persistedSession,
    accessToken,
    refreshToken,
    persistMode: normalizePersistMode(persistedSession.persistMode ?? mode)
  };
}

function nowIso(): string {
  return new Date().toISOString();
}

function seedConsultationState(): DemoConsultationState {
  const timestamp = nowIso();
  const sessions: ConsultationSessionSummary[] = DEMO_DOMAINS.map((domain, index) => {
    const sessionNo = `CS-${String(domain.id).padStart(2, "0")}-0001`;
    const createdAt = new Date(Date.now() - index * 15 * 60_000).toISOString();
    return {
      sessionNo,
      businessDomainId: domain.id,
      customerId: "1",
      sessionStatus: "open",
      assignedTo: 2,
      lastMessageAt: createdAt,
      lastMessagePreview: `${domain.name} support is online.`
    };
  });

  const messages: Record<string, ConsultationMessage[]> = Object.fromEntries(
    sessions.map((session, index) => [
      session.sessionNo,
      [
        {
          sessionNo: session.sessionNo,
          seqNo: 1,
          senderRole: "agent",
          senderUserId: "2",
          content: `${DEMO_DOMAINS[index].name} support is online.`,
          createdAt: timestamp
        }
      ]
    ])
  );

  return { sessions, messages };
}

function loadConsultationState(): DemoConsultationState {
  const state = readJson<DemoConsultationState | null>(CONSULTATION_STATE_KEY, null);
  if (state && Array.isArray(state.sessions) && state.messages) {
    return state;
  }
  const seeded = seedConsultationState();
  writeJson(CONSULTATION_STATE_KEY, seeded);
  return seeded;
}

function persistConsultationState(state: DemoConsultationState): void {
  writeJson(CONSULTATION_STATE_KEY, state);
}

function loadTicketMetaStore(): TicketMetaStore {
  const meta = readJson<TicketMetaStore | null>(TICKET_META_KEY, null);
  if (meta) {
    return { ...seedMeta, ...meta };
  }
  writeJson(TICKET_META_KEY, seedMeta);
  return { ...seedMeta };
}

function persistTicketMetaStore(store: TicketMetaStore): void {
  writeJson(TICKET_META_KEY, store);
}

function fallbackTicketMeta(ticketNo: string): TicketMeta {
  return loadTicketMetaStore()[ticketNo] ?? {
    businessDomainId: DEMO_DOMAINS[0].id,
    customerId: "1",
    ticketTypeId: "1",
    priority: "normal",
    description: ""
  };
}

function sortByLatest<T extends { lastMessageAt?: string | null; createdAt?: string }>(items: T[]): T[] {
  return [...items].sort((left, right) => {
    const leftTime = Date.parse(left.lastMessageAt ?? left.createdAt ?? "");
    const rightTime = Date.parse(right.lastMessageAt ?? right.createdAt ?? "");
    return rightTime - leftTime;
  });
}

export function getDemoDomains() {
  return DEMO_DOMAINS;
}

export function loadCustomerProfile(): DemoProfile {
  const profile = readJson<DemoProfile | null>(CUSTOMER_PROFILE_KEY, null);
  if (profile) {
    return profile;
  }
  const fallback: DemoProfile = {
    customerId: "1",
    nickname: "Demo Customer",
    phone: "13800000000",
    selectedDomainId: DEMO_DOMAINS[0].id
  };
  writeJson(CUSTOMER_PROFILE_KEY, fallback);
  return fallback;
}

export function saveCustomerProfile(profile: DemoProfile): DemoProfile {
  writeJson(CUSTOMER_PROFILE_KEY, profile);
  return profile;
}

export function loadAdminProfile(): AdminProfile {
  const profile = readJson<AdminProfile | null>(ADMIN_PROFILE_KEY, null);
  if (profile) {
    return profile;
  }
  const fallback: AdminProfile = {
    username: "admin",
    selectedDomainId: DEMO_DOMAINS[0].id
  };
  writeJson(ADMIN_PROFILE_KEY, fallback);
  return fallback;
}

export function saveAdminProfile(profile: AdminProfile): AdminProfile {
  writeJson(ADMIN_PROFILE_KEY, profile);
  return profile;
}

export function loadAccessToken(): string {
  if (!inMemoryAccessToken) {
    const session = loadAuthSession();
    if (session?.accessToken) {
      inMemoryAccessToken = session.accessToken;
    }
  }
  return inMemoryAccessToken;
}

export function saveAccessToken(token: string): string {
  const normalized = token.trim();
  inMemoryAccessToken = normalized;
  return normalized;
}

export function loadRefreshToken(): string {
  if (!inMemoryRefreshToken) {
    const session = loadAuthSession();
    if (session?.refreshToken) {
      inMemoryRefreshToken = session.refreshToken;
    }
  }
  return inMemoryRefreshToken;
}

export function saveRefreshToken(token: string): string {
  const normalized = token.trim();
  inMemoryRefreshToken = normalized;
  return normalized;
}

export function loadAuthSession(): AuthSessionState | null {
  const persistedSession = readPersistedAuthFromMode("session") ?? readPersistedAuthFromMode("local");
  if (!persistedSession) {
    return null;
  }
  inMemoryAccessToken = persistedSession.accessToken;
  inMemoryRefreshToken = persistedSession.refreshToken;
  return persistedSession;
}

export function saveAuthSession(session: AuthSessionState, options?: SaveAuthSessionOptions): AuthSessionState {
  const persistMode = normalizePersistMode(options?.persistMode ?? session.persistMode);
  const accessToken = saveAccessToken(session.accessToken);
  const refreshToken = saveRefreshToken(session.refreshToken);
  const { accessToken: _, refreshToken: __, persistMode: ___, ...persistedSessionBase } = session;
  const persistedSession: PersistedAuthSessionState = {
    ...persistedSessionBase,
    persistMode
  };
  clearAllPersistedAuth(true);
  writeJsonToStorage<PersistedAuthSessionState>(getStorage(persistMode), AUTH_SESSION_KEY, persistedSession);
  persistAuthTokens(persistMode, accessToken, refreshToken);
  return {
    ...persistedSession,
    accessToken,
    refreshToken,
    persistMode
  };
}

export function clearAuthSession(): void {
  saveAccessToken("");
  saveRefreshToken("");
  clearAllPersistedAuth(true);
}

/** 更新已存会话的强制改密标志（改密成功后置 false） */
export function updateStoredMustChangePassword(mustChangePassword: boolean): void {
  const session = loadAuthSession();
  if (!session) {
    return;
  }
  saveAuthSession({
    ...session,
    mustChangePassword,
  });
}

export function loadPermissionSnapshot(): PermissionSnapshot | null {
  const session = loadAuthSession();
  if (!session) {
    return null;
  }
  const preferredMode = normalizePersistMode(session.persistMode);
  const preferredSnapshot = readJson<PermissionSnapshot | null>(PERMISSION_SNAPSHOT_KEY, null, preferredMode);
  if (preferredSnapshot) {
    return preferredSnapshot;
  }
  const fallbackMode = preferredMode === "local" ? "session" : "local";
  return readJson<PermissionSnapshot | null>(PERMISSION_SNAPSHOT_KEY, null, fallbackMode);
}

export function savePermissionSnapshot(snapshot: PermissionSnapshot, options?: SavePermissionSnapshotOptions): PermissionSnapshot {
  const mode = normalizePersistMode(options?.persistMode ?? loadAuthSession()?.persistMode);
  // Ensure snapshot only exists in the active persist mode.
  removeJsonFromStorage(getStorage(mode === "local" ? "session" : "local"), PERMISSION_SNAPSHOT_KEY);
  writeJson<PermissionSnapshot>(PERMISSION_SNAPSHOT_KEY, snapshot, mode);
  return snapshot;
}

export function loadTicketMeta(ticketNo: string): TicketMeta {
  return fallbackTicketMeta(ticketNo);
}

export function saveTicketMeta(ticketNo: string, meta: TicketMeta): TicketMeta {
  const store = loadTicketMetaStore();
  store[ticketNo] = meta;
  persistTicketMetaStore(store);
  return meta;
}

export function saveTicketMetas(entries: Record<string, TicketMeta>): Record<string, TicketMeta> {
  const store = { ...loadTicketMetaStore(), ...entries };
  persistTicketMetaStore(store);
  return store;
}

export function mergeTicket(record: { id: string; ticketNo: string; title: string; status: string; createdAt: string }): DemoTicket {
  const meta = loadTicketMeta(record.ticketNo);
  return {
    ...record,
    ...meta,
    source: "web"
  };
}

export function mergeTickets(
  records: Array<{ id: string; ticketNo: string; title: string; status: string; createdAt: string }>
): DemoTicket[] {
  return records.map((record) => mergeTicket(record));
}

export function listSessions(domainId: string, customerId?: string): ConsultationSessionSummary[] {
  const state = loadConsultationState();
  const sessions = state.sessions.filter((session) => {
    const sameDomain = session.businessDomainId === domainId;
    const sameCustomer = customerId === undefined || session.customerId === customerId;
    return sameDomain && sameCustomer;
  });
  return sortByLatest(sessions);
}

export function listMessages(sessionNo: string): ConsultationMessage[] {
  const state = loadConsultationState();
  return [...(state.messages[sessionNo] ?? [])].sort((left, right) => left.seqNo - right.seqNo);
}

export function saveMessage(payload: {
  sessionNo?: string;
  businessDomainId: string;
  customerId: string;
  senderUserId?: string;
  senderRole: "customer" | "agent";
  content: string;
}): ConsultationSessionSummary {
  const state = loadConsultationState();
  const timestamp = nowIso();
  const existingSessionIndex = payload.sessionNo
    ? state.sessions.findIndex((session) => session.sessionNo === payload.sessionNo)
    : -1;
  let session: ConsultationSessionSummary;

  if (existingSessionIndex >= 0) {
    session = state.sessions[existingSessionIndex];
  } else {
    session = {
      sessionNo: `CS-${String(payload.businessDomainId).padStart(2, "0")}-${String(payload.customerId).padStart(4, "0")}-${Date.now().toString().slice(-4)}`,
      businessDomainId: payload.businessDomainId,
      customerId: payload.customerId,
      sessionStatus: "open",
      assignedTo: payload.senderRole === "agent" ? Number(payload.senderUserId ?? 2) : 2,
      lastMessageAt: timestamp,
      lastMessagePreview: payload.content.slice(0, 80)
    };
    state.sessions.push(session);
  }

  const currentMessages = state.messages[session.sessionNo] ?? [];
  const nextSeq = (currentMessages.at(-1)?.seqNo ?? 0) + 1;
  currentMessages.push({
    sessionNo: session.sessionNo,
    seqNo: nextSeq,
    senderRole: payload.senderRole,
    senderUserId: payload.senderUserId ?? null,
    content: payload.content,
    createdAt: timestamp
  });

  if (payload.senderRole === "customer" && nextSeq === 1) {
    currentMessages.push({
      sessionNo: session.sessionNo,
      seqNo: nextSeq + 1,
      senderRole: "agent",
      senderUserId: "2",
      content: "We have received your message and will follow up soon.",
      createdAt: new Date(Date.now() + 60_000).toISOString()
    });
    session.sessionStatus = "processing";
    session.lastMessagePreview = "We have received your message and will follow up soon.";
    session.lastMessageAt = currentMessages.at(-1)?.createdAt ?? timestamp;
  } else {
    session.sessionStatus = payload.senderRole === "agent" ? "processing" : session.sessionStatus;
    session.lastMessagePreview = payload.content.slice(0, 80);
    session.lastMessageAt = timestamp;
  }

  state.messages[session.sessionNo] = currentMessages;
  const updatedSessionIndex = state.sessions.findIndex((item) => item.sessionNo === session.sessionNo);
  if (updatedSessionIndex >= 0) {
    state.sessions[updatedSessionIndex] = session;
  }
  persistConsultationState(state);
  return session;
}

export function seedTicketMetaIfNeeded(records: Array<{ ticketNo: string }>): void {
  const store = loadTicketMetaStore();
  let changed = false;
  for (const record of records) {
    if (!store[record.ticketNo]) {
      store[record.ticketNo] = fallbackTicketMeta(record.ticketNo);
      changed = true;
    }
  }
  if (changed) {
    persistTicketMetaStore(store);
  }
}
