import { logoutCustomerLive, useCustomerPortal } from "@uniondesk/shared";
import { useState } from "react";
import { useNavigate } from "react-router-dom";

import { IconChevron } from "../../components/Icons";
import { useToast } from "../../components/Toast";

export default function MePage() {
	const portal = useCustomerPortal();
	const toast = useToast();
	const navigate = useNavigate();
	const [loggingOut, setLoggingOut] = useState(false);

	const logout = async () => {
		setLoggingOut(true);
		try {
			await logoutCustomerLive();
			toast.success("已退出登录");
			navigate("/login", { replace: true });
		}
		catch (error) {
			toast.error(error instanceof Error ? error.message : "退出失败");
		}
		finally {
			setLoggingOut(false);
		}
	};

	return (
		<div className="ud-stack ud-stack--lg">
			<header className="ud-glass ud-glass--lg" style={{ padding: 22 }}>
				<p className="ud-kicker">账号</p>
				<h1 className="ud-title" style={{ fontSize: 30 }}>{portal.account?.displayName ?? "未登录"}</h1>
				<p className="ud-subtitle">
					{portal.account?.loginName}
					{portal.account?.phone ? ` · ${portal.account.phone}` : ""}
				</p>
			</header>

			<section className="ud-glass ud-settings" style={{ overflow: "hidden" }}>
				<button type="button" className="ud-settings__item" onClick={() => navigate("/domains")}>
					<div>
						<strong>业务域</strong>
						<p className="ud-muted" style={{ margin: "4px 0 0" }}>
							当前：
							{portal.activeDomain?.name ?? "未选择"}
						</p>
					</div>
					<IconChevron />
				</button>
				<button type="button" className="ud-settings__item" onClick={() => navigate("/inbox")}>
					<div>
						<strong>通知</strong>
						<p className="ud-muted" style={{ margin: "4px 0 0" }}>
							未读
							{" "}
							{portal.unreadCount}
						</p>
					</div>
					<IconChevron />
				</button>
				<button
					type="button"
					className="ud-settings__item"
					onClick={() => toast.show("通知偏好将在联调后提供（UI 占位）")}
				>
					<div>
						<strong>通知偏好</strong>
						<p className="ud-muted" style={{ margin: "4px 0 0" }}>邮件 / 站内信（即将上线）</p>
					</div>
					<IconChevron />
				</button>
			</section>

			<button type="button" className="ud-btn ud-btn--danger ud-btn--block" disabled={loggingOut} onClick={() => void logout()}>
				{loggingOut ? "退出中…" : "退出登录"}
			</button>

			<p className="ud-muted" style={{ textAlign: "center", margin: 0 }}>
				专属入口示例：
				<code className="ud-mono">/d/online-service/login</code>
			</p>
		</div>
	);
}
