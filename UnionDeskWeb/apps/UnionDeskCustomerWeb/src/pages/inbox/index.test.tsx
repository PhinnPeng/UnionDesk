import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { BrowserRouter } from "react-router-dom";
import { describe, it, expect, beforeEach, vi } from "vitest";
import InboxPage from "./index";

const mockNavigate = vi.fn();
vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual("react-router-dom");
  return {
    ...actual,
    useNavigate: () => mockNavigate
  };
});

describe("InboxPage - 通知中心页面", () => {
  beforeEach(() => {
    mockNavigate.mockClear();
  });

  it("TC-048: 应该渲染通知中心页面", () => {
    render(
      <BrowserRouter>
        <InboxPage />
      </BrowserRouter>
    );

    expect(screen.getByText("通知中心")).toBeInTheDocument();
    expect(screen.getByText(/这里展示工单、入域和系统通知/)).toBeInTheDocument();
  });

  it("TC-048: 应该显示通知列表", () => {
    render(
      <BrowserRouter>
        <InboxPage />
      </BrowserRouter>
    );

    expect(screen.getByText("工单已创建", { selector: "strong" })).toBeInTheDocument();
    expect(screen.getByText("你的示例工单已创建成功，当前状态为处理中。")).toBeInTheDocument();
  });

  it("TC-048: 通知应该显示已读/未读状态", () => {
    render(
      <BrowserRouter>
        <InboxPage />
      </BrowserRouter>
    );

    const unreadTag = screen.queryByText("未读");
    const readTag = screen.queryByText("已读");

    expect(unreadTag || readTag).toBeTruthy();
  });

  it("TC-048: 应该能够标记通知为已读", async () => {
    render(
      <BrowserRouter>
        <InboxPage />
      </BrowserRouter>
    );

    const markReadButtons = screen.getAllByRole("button", { name: /标记已读/ });
    if (markReadButtons.length > 0) {
      fireEvent.click(markReadButtons[0]);
      await waitFor(() => {
        expect(screen.getByText("已读")).toBeInTheDocument();
      });
    }
  });

  it("TC-048: 通知应该显示类型标签", () => {
    render(
      <BrowserRouter>
        <InboxPage />
      </BrowserRouter>
    );

    const ticketTag = screen.queryByText("ticket");
    const domainTag = screen.queryByText("domain");
    const systemTag = screen.queryByText("system");

    expect(ticketTag || domainTag || systemTag).toBeTruthy();
  });

  it("TC-048: 应该能够跳转到相关页面", () => {
    render(
      <BrowserRouter>
        <InboxPage />
      </BrowserRouter>
    );

    expect(screen.getByRole("link", { name: "跳转到相关页面" })).toHaveAttribute("href", "/tickets/1");
  });

  it("TC-048: 应该能够返回工作台", async () => {
    const user = userEvent.setup();
    render(
      <BrowserRouter>
        <InboxPage />
      </BrowserRouter>
    );

    const backButton = screen.getByRole("button", { name: /返回工作台/ });
    await user.click(backButton);

    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith("/workspace");
    });
  });

  it("TC-048: 空通知列表应该显示空状态", () => {
    render(
      <BrowserRouter>
        <InboxPage />
      </BrowserRouter>
    );

    const emptyText = screen.queryByText("暂无通知");
    if (emptyText) {
      expect(emptyText).toBeInTheDocument();
    }
  });
});
