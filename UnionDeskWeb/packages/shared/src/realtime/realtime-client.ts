/**
 * 实时通道客户端（WebSocket 单例）：握手鉴权（?token=）、业务心跳、
 * 断线指数退避重连、事件订阅（on/off）、重连就绪回调（hello 触发外部增量刷新）。
 * 与后端 uniondesk-support/realtime 的 RealtimeWebSocketHandler 协议对齐。
 */

/** 下行事件类型（与后端 RealtimeConstants 对齐） */
export const REALTIME_EVENT = {
  HELLO: "hello",
  CHAT_MESSAGE: "chat.message",
  CHAT_SESSION: "chat.session",
  CHAT_QUEUE: "chat.queue",
  TICKET_CREATED: "ticket.created",
  TICKET_REPLIED: "ticket.replied",
  TICKET_UPDATED: "ticket.updated",
  INBOX_NEW: "inbox.new",
} as const;

export type RealtimeEventType = (typeof REALTIME_EVENT)[keyof typeof REALTIME_EVENT];

export interface RealtimeEnvelope {
  v: number;
  id: string;
  type: string;
  ts: number;
  payload: Record<string, unknown>;
}

type EventHandler = (payload: Record<string, unknown>, envelope: RealtimeEnvelope) => void;

const PING_INTERVAL_MS = 30_000;
const RECONNECT_BASE_MS = 1_000;
const RECONNECT_MAX_MS = 30_000;

function resolveWsUrl(): string {
  if (typeof window === "undefined") {
    return "";
  }
  const override = (import.meta as { env?: Record<string, string> }).env?.VITE_WS_URL;
  if (override) {
    return override;
  }
  const protocol = window.location.protocol === "https:" ? "wss:" : "ws:";
  return `${protocol}//${window.location.host}/api/v1/ws`;
}

class RealtimeClient {
  private socket: WebSocket | null = null;
  private token = "";
  private url = "";
  private handlers = new Map<string, Set<EventHandler>>();
  private pingTimer: ReturnType<typeof setInterval> | null = null;
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null;
  private reconnectAttempt = 0;
  private closedByUser = false;
  /** 连接建立（含重连）后触发：外部据此做增量刷新 */
  private readyHandlers = new Set<() => void>();
  private lastPongAt = 0;

  /** 建立连接；token 变化（换用户/重登录）时强制重连 */
  connect(token: string): void {
    if (this.token === token && this.socket) {
      return;
    }
    this.token = token;
    this.closedByUser = false;
    if (this.socket) {
      this.socket.close();
      this.socket = null;
    }
    this.open();
  }

  /** 主动断开（登出时调用） */
  disconnect(): void {
    this.closedByUser = true;
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
    if (this.pingTimer) {
      clearInterval(this.pingTimer);
      this.pingTimer = null;
    }
    this.socket?.close();
    this.socket = null;
  }

  on(type: RealtimeEventType, handler: EventHandler): void {
    const set = this.handlers.get(type) ?? new Set<EventHandler>();
    set.add(handler);
    this.handlers.set(type, set);
  }

  off(type: RealtimeEventType, handler: EventHandler): void {
    this.handlers.get(type)?.delete(handler);
  }

  /** 连接就绪/重连成功回调（收到 hello 后触发） */
  onReady(handler: () => void): void {
    this.readyHandlers.add(handler);
  }

  offReady(handler: () => void): void {
    this.readyHandlers.delete(handler);
  }

  private open(): void {
    if (!this.token || this.socket?.readyState === WebSocket.OPEN || this.socket?.readyState === WebSocket.CONNECTING) {
      return;
    }
    if (!this.url) {
      this.url = resolveWsUrl();
    }
    const separator = this.url.includes("?") ? "&" : "?";
    const socket = new WebSocket(`${this.url}${separator}token=${encodeURIComponent(this.token)}`);
    this.socket = socket;

    socket.onopen = () => {
      this.reconnectAttempt = 0;
      this.startPing();
    };

    socket.onmessage = event => {
      let envelope: RealtimeEnvelope;
      try {
        envelope = JSON.parse(String(event.data)) as RealtimeEnvelope;
      }
      catch {
        return;
      }
      if (envelope.type === REALTIME_EVENT.HELLO) {
        this.lastPongAt = Date.now();
        for (const handler of this.readyHandlers) {
          handler();
        }
        return;
      }
      if (envelope.type === "pong") {
        this.lastPongAt = Date.now();
        return;
      }
      const handlers = this.handlers.get(envelope.type);
      if (handlers) {
        for (const handler of handlers) {
          handler(envelope.payload ?? {}, envelope);
        }
      }
    };

    socket.onclose = () => {
      this.stopPing();
      this.scheduleReconnect();
    };

    socket.onerror = () => {
      // onclose 随后触发，统一走重连
    };
  }

  private startPing(): void {
    this.stopPing();
    this.pingTimer = setInterval(() => {
      if (this.socket?.readyState === WebSocket.OPEN) {
        this.socket.send(JSON.stringify({ type: "ping", ts: Date.now() }));
        // 90s 无 pong 判定失联，主动关闭触发重连
        if (this.lastPongAt > 0 && Date.now() - this.lastPongAt > 90_000) {
          this.socket.close();
        }
      }
    }, PING_INTERVAL_MS);
  }

  private stopPing(): void {
    if (this.pingTimer) {
      clearInterval(this.pingTimer);
      this.pingTimer = null;
    }
  }

  private scheduleReconnect(): void {
    if (this.closedByUser) {
      return;
    }
    if (this.reconnectTimer) {
      return;
    }
    const delay = Math.min(RECONNECT_BASE_MS * 2 ** this.reconnectAttempt, RECONNECT_MAX_MS)
      + Math.random() * 500;
    this.reconnectAttempt += 1;
    this.reconnectTimer = setTimeout(() => {
      this.reconnectTimer = null;
      this.open();
    }, delay);
  }
}

/** 全局单例（两端共用） */
export const realtimeClient = new RealtimeClient();
