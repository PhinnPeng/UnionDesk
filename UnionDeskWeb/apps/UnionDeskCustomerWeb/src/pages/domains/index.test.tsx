import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { BrowserRouter } from "react-router-dom";
import { describe, it, expect, beforeEach, vi } from "vitest";
import DomainsPage from "./index";

const mockNavigate = vi.fn();
vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual("react-router-dom");
  return {
    ...actual,
    useNavigate: () => mockNavigate
  };
});

describe("DomainsPage - 业务域选择页面", () => {
  beforeEach(() => {
    mockNavigate.mockClear();
  });

  it("TC-004: 应该渲染业务域列表", () => {
    render(
      <BrowserRouter>
        <DomainsPage />
      </BrowserRouter>
    );

    expect(screen.getByText("选择业务域")).toBeInTheDocument();
    expect(screen.getByText("默认业务域")).toBeInTheDocument();
    expect(screen.getByText("在线客服域")).toBeInTheDocument();
    expect(screen.getByText("售后支持域")).toBeInTheDocument();
  });

  it("TC-004: 应该显示业务域的注册策略标签", () => {
    render(
      <BrowserRouter>
        <DomainsPage />
      </BrowserRouter>
    );

    expect(screen.getByText("开放注册")).toBeInTheDocument();
    expect(screen.getByText("仅邀请码可入域")).toBeInTheDocument();
    expect(screen.getByText("仅管理员可分配")).toBeInTheDocument();
  });

  it("TC-004: 应该能够选择已加入的业务域", async () => {
    render(
      <BrowserRouter>
        <DomainsPage />
      </BrowserRouter>
    );

    const enterButtons = screen.getAllByText(/进入此域|当前业务域/);
    if (enterButtons.length > 0) {
      fireEvent.click(enterButtons[0]);

      await waitFor(() => {
        expect(mockNavigate).toHaveBeenCalledWith("/workspace", { replace: true });
      });
    }
  });

  it("TC-005: 应该渲染邀请码入域表单", () => {
    render(
      <BrowserRouter>
        <DomainsPage />
      </BrowserRouter>
    );

    expect(screen.getByText("邀请码入域")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("输入邀请码直接入域")).toBeInTheDocument();
  });

  it("TC-005: 应该能够通过邀请码入域", async () => {
    const user = userEvent.setup();
    render(
      <BrowserRouter>
        <DomainsPage />
      </BrowserRouter>
    );

    const invitationCodeInput = screen.getByPlaceholderText("输入邀请码直接入域");

    await user.type(invitationCodeInput, "INV-0002");
    const form = invitationCodeInput.closest("form");
    expect(form).not.toBeNull();
    fireEvent.submit(form as HTMLFormElement);

    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith("/workspace", { replace: true });
    });
  });

  it("TC-005: 邀请码入域应该验证必填字段", async () => {
    render(
      <BrowserRouter>
        <DomainsPage />
      </BrowserRouter>
    );

    expect(screen.getByPlaceholderText("输入邀请码直接入域")).toHaveAttribute("aria-required", "true");
  });

  it("TC-004: 应该显示业务域的邀请码", () => {
    render(
      <BrowserRouter>
        <DomainsPage />
      </BrowserRouter>
    );

    expect(screen.getByText(/邀请码：INV-0001/)).toBeInTheDocument();
    expect(screen.getByText(/邀请码：INV-0002/)).toBeInTheDocument();
    expect(screen.getByText(/邀请码：INV-0003/)).toBeInTheDocument();
  });
});
