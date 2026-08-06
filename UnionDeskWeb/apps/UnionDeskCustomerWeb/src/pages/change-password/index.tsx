import { changePassword, updateStoredMustChangePassword } from "@uniondesk/shared";
import { useState, type FormEvent } from "react";
import { useNavigate } from "react-router-dom";

import { useToast } from "../../components/Toast";

export default function ChangePasswordPage() {
	const toast = useToast();
	const navigate = useNavigate();
	const [currentPassword, setCurrentPassword] = useState("");
	const [newPassword, setNewPassword] = useState("");
	const [confirmPassword, setConfirmPassword] = useState("");
	const [loading, setLoading] = useState(false);

	const handleSubmit = async (event: FormEvent) => {
		event.preventDefault();
		if (newPassword.length < 6) {
			toast.error("新密码至少 6 位");
			return;
		}
		if (newPassword !== confirmPassword) {
			toast.error("两次输入的新密码不一致");
			return;
		}
		if (newPassword === currentPassword) {
			toast.error("新密码不能与当前密码相同");
			return;
		}
		setLoading(true);
		try {
			await changePassword(currentPassword, newPassword);
			updateStoredMustChangePassword(false);
			toast.success("密码修改成功，请使用新密码重新登录");
			navigate("/login", { replace: true });
		}
		catch (error) {
			toast.error(error instanceof Error ? error.message : "修改密码失败");
		}
		finally {
			setLoading(false);
		}
	};

	return (
		<div className="ud-stage">
			<div className="ud-main ud-main--auth">
				<section className="ud-glass ud-glass--lg ud-auth-panel">
					<div>
						<p className="ud-kicker">安全验证</p>
						<h1 className="ud-title" style={{ fontSize: 30 }}>
							修改密码
						</h1>
					</div>

					<form className="ud-form" onSubmit={handleSubmit}>
						<div className="ud-field">
							<label htmlFor="currentPassword">当前密码</label>
							<input
								id="currentPassword"
								className="ud-input"
								type="password"
								value={currentPassword}
								onChange={event => setCurrentPassword(event.target.value)}
								placeholder="输入当前（一次性）密码"
								autoComplete="current-password"
								required
							/>
						</div>
						<div className="ud-field">
							<label htmlFor="newPassword">新密码</label>
							<input
								id="newPassword"
								className="ud-input"
								type="password"
								value={newPassword}
								onChange={event => setNewPassword(event.target.value)}
								placeholder="至少 6 位"
								autoComplete="new-password"
								required
							/>
						</div>
						<div className="ud-field">
							<label htmlFor="confirmPassword">确认新密码</label>
							<input
								id="confirmPassword"
								className="ud-input"
								type="password"
								value={confirmPassword}
								onChange={event => setConfirmPassword(event.target.value)}
								placeholder="再次输入新密码"
								autoComplete="new-password"
								required
							/>
						</div>
						<button className="ud-btn ud-btn--primary ud-btn--block" type="submit" disabled={loading}>
							{loading ? "提交中…" : "确认修改"}
						</button>
					</form>
				</section>
			</div>
		</div>
	);
}
