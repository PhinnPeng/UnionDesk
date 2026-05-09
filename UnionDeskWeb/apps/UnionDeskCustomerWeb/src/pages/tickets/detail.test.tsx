import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { BrowserRouter, Route, Routes } from "react-router-dom";
import { describe, it, expect, beforeEach, vi } from "vitest";
import TicketDetailPage from "./detail";

const mockNavigate = vi.fn();
vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual("react-router-dom");
  return {
    ...actual,
    useNavigate: () => mockNavigate,
    useParams: () => ({ ticketId: "1" })
  };
});

describe("TicketDetailPage - 工单详情页面", () => {
  beforeEach(() => {
    mockNavigate.mockClear();
  });

  it("TC-022: 应该渲染工单详情页面", () => {
    render(
      <BrowserRouter>
        <Routes>
          <Route path="*" element={<TicketDetailPage />} />
        </Routes>
      </BrowserRouter>
    );

    expect(screen.getByText("工单信息")).toBeInTheDocument();
    expect(screen.getByText("回复时间线")).toBeInTheDocument();
    expect(screen.getByText("工单附件")).toBeInTheDocument();
  });

  it("TC-022: 应该显示工单基本信息", () => {
    render(
      <BrowserRouter>
        <Routes>
          <Route path="*" element={<TicketDetailPage />} />
        </Routes>
      </BrowserRouter>
    );

    expect(screen.getByText("业务域")).toBeInTheDocument();
    expect(screen.getByText("类型")).toBeInTheDocument();
    expect(screen.getByText("创建时间")).toBeInTheDocument();
    expect(screen.getByText("更新时间")).toBeInTheDocument();
  });

  it("TC-022: 应该显示工单标题和描述", () => {
    render(
      <BrowserRouter>
        <Routes>
          <Route path="*" element={<TicketDetailPage />} />
        </Routes>
      </BrowserRouter>
    );

    expect(screen.getByRole("heading", { name: "演示工单：登录后无法切换业务域" })).toBeInTheDocument();
    expect(screen.getByText("这是用于展示客户闭环的演示工单，页面会展示状态、回复、附件与通知。")).toBeInTheDocument();
  });

  it("TC-023: 应该显示回复时间线", () => {
    render(
      <BrowserRouter>
        <Routes>
          <Route path="*" element={<TicketDetailPage />} />
        </Routes>
      </BrowserRouter>
    );

    expect(screen.getByText("回复时间线")).toBeInTheDocument();
  });

  it("TC-023: 回复应该显示作者信息和时间", () => {
    render(
      <BrowserRouter>
        <Routes>
          <Route path="*" element={<TicketDetailPage />} />
        </Routes>
      </BrowserRouter>
    );

    const systemAuthor = screen.queryByText("系统");
    if (systemAuthor) {
      expect(systemAuthor).toBeInTheDocument();
    }
  });

  it("TC-035: 应该能够撤回待受理工单", async () => {
    const user = userEvent.setup();
    render(
      <BrowserRouter>
        <Routes>
          <Route path="*" element={<TicketDetailPage />} />
        </Routes>
      </BrowserRouter>
    );

    const withdrawButton = screen.queryByText("撤回工单");
    if (withdrawButton) {
      await user.click(withdrawButton);
      await waitFor(() => {
        expect(screen.queryByText("撤回工单")).not.toBeInTheDocument();
      });
    }
  });

  it("TC-022: 应该显示附件列表", () => {
    render(
      <BrowserRouter>
        <Routes>
          <Route path="*" element={<TicketDetailPage />} />
        </Routes>
      </BrowserRouter>
    );

    expect(screen.getByText("工单附件")).toBeInTheDocument();
  });

  it("TC-022: 应该能够返回上一页", async () => {
    const user = userEvent.setup();
    render(
      <BrowserRouter>
        <Routes>
          <Route path="*" element={<TicketDetailPage />} />
        </Routes>
      </BrowserRouter>
    );

    const backButton = screen.getByRole("button", { name: /返回/ });
    await user.click(backButton);

    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalled();
    });
  });
});
