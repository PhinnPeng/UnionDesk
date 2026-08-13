export type TrackEventCode = "satisfaction.submit" | "satisfaction.view";

/**
 * 埋点最小基建：事件码统一在此登记，当前为占位实现（console.debug 本地日志）。
 * 后续接入分析平台时仅需扩展本函数上报实现，调用点不变。
 */
export function trackEvent(code: TrackEventCode, payload?: Record<string, unknown>): void {
  if (typeof console !== "undefined" && console.debug) {
    console.debug(`[track] ${code}`, payload ?? {});
  }
}
