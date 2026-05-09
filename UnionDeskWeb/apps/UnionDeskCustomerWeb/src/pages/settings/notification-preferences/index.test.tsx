import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, it, expect, beforeEach } from "vitest";
import NotificationPreferencesPage from "./index";

const STORAGE_KEY = "uniondesk.customer.notification-preferences";

describe("NotificationPreferencesPage - 通知偏好设置页", () => {
  beforeEach(() => {
    window.localStorage.clear();
  });

  it("应该渲染通知偏好设置页面", () => {
    render(<NotificationPreferencesPage />);

    expect(screen.getByText("通知偏好设置")).toBeInTheDocument();
    expect(screen.getByText("通知渠道")).toBeInTheDocument();
    expect(screen.getByText("通知类型")).toBeInTheDocument();
    expect(screen.getByText("免打扰时段")).toBeInTheDocument();
  });

  it("应该可以保存偏好到本机存储", async () => {
    render(<NotificationPreferencesPage />);

    const inAppSwitch = screen.getByRole("switch", { name: "站内信通知" });
    fireEvent.click(inAppSwitch);

    fireEvent.click(screen.getByRole("button", { name: "保存偏好" }));

    await waitFor(() => {
      const raw = window.localStorage.getItem(STORAGE_KEY);
      expect(raw).not.toBeNull();
      expect(JSON.parse(raw ?? "{}")).toMatchObject({
        inAppEnabled: false,
        emailEnabled: true,
        quietStart: "22:00",
        quietEnd: "08:00"
      });
    });
  });

  it("应该可以恢复默认设置", async () => {
    window.localStorage.setItem(
      STORAGE_KEY,
      JSON.stringify({
        inAppEnabled: false,
        emailEnabled: false,
        categories: ["security_alert"],
        quietStart: "00:00",
        quietEnd: "06:00"
      })
    );

    render(<NotificationPreferencesPage />);

    fireEvent.click(screen.getByRole("button", { name: "恢复默认" }));

    await waitFor(() => {
      expect(window.localStorage.getItem(STORAGE_KEY)).toBeNull();
    });
  });
});
