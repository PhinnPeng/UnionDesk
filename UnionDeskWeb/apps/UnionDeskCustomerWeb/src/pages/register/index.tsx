import { registerCustomerLive, useCustomerPortal } from "@uniondesk/shared";
import { useMemo, useState, type FormEvent } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";

import { useToast } from "../../components/Toast";
import { enterDedicatedDomain, findDomainByCode } from "../../utils/domain-flow";

export default function RegisterPage() {
	const portal = useCustomerPortal();
	const toast = useToast();
	const navigate = useNavigate();
	const params = useParams();
	const domainCode = params.domainCode;
	const dedicatedDomain = useMemo(
		() => findDomainByCode(portal.domains, domainCode),
		[domainCode, portal.domains],
	);

	const openDomains = portal.domains.filter(item => item.registrationPolicy === "open");

	const [form, setForm] = useState({
		loginName: "",
		password: "",
		displayName: "",
		phone: "",
		email: "",
		domainId: openDomains[0]?.id ? String(openDomains[0].id) : "",
		invitationCode: dedicatedDomain?.registrationPolicy === "invitation_only"
			? dedicatedDomain.invitationCode
			: "",
	});
	const [loading, setLoading] = useState(false);

	const loginPath = domainCode ? `/d/${domainCode}/login` : "/login";

	const handleSubmit = async (event: FormEvent) => {
		event.preventDefault();
		setLoading(true);
		try {
			await registerCustomerLive({
				loginName: form.loginName.trim(),
				password: form.password,
				displayName: form.displayName.trim(),
				phone: form.phone.trim(),
				email: form.email.trim() || undefined,
				domainId: domainCode
					? dedicatedDomain?.id ?? null
					: form.domainId
						? form.domainId
						: null,
				invitationCode: form.invitationCode.trim() || undefined,
			});

			if (domainCode) {
				const result = await enterDedicatedDomain(domainCode);
				if (result.ok) {
					toast.success("注册成功，已进入业务域");
					navigate(result.path, { replace: true });
				}
				else {
					toast.error(result.message);
					navigate(result.path, { replace: true });
				}
				return;
			}

			toast.success("注册成功");
			navigate("/domains", { replace: true });
		}
		catch (error) {
			toast.error(error instanceof Error ? error.message : "注册失败");
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
						<p className="ud-kicker">{domainCode ? "专属注册" : "创建账号"}</p>
						<h1 className="ud-title" style={{ fontSize: 30 }}>
							{dedicatedDomain ? `加入 ${dedicatedDomain.name}` : "开始使用 UnionDesk"}
						</h1>
						<p className="ud-subtitle">
							{dedicatedDomain
								? "完成注册后将进入该业务域。"
								: "注册后可选择开放业务域，或使用邀请码加入。"}
						</p>
					</div>

					<form className="ud-form" onSubmit={handleSubmit}>
						<div className="ud-field">
							<label htmlFor="displayName">显示名</label>
							<input
								id="displayName"
								className="ud-input"
								value={form.displayName}
								onChange={event => setForm(prev => ({ ...prev, displayName: event.target.value }))}
								required
							/>
						</div>
						<div className="ud-field">
							<label htmlFor="loginName">登录名</label>
							<input
								id="loginName"
								className="ud-input"
								value={form.loginName}
								onChange={event => setForm(prev => ({ ...prev, loginName: event.target.value }))}
								required
							/>
						</div>
						<div className="ud-field">
							<label htmlFor="phone">手机号</label>
							<input
								id="phone"
								className="ud-input"
								value={form.phone}
								onChange={event => setForm(prev => ({ ...prev, phone: event.target.value }))}
								required
							/>
						</div>
						<div className="ud-field">
							<label htmlFor="password">密码</label>
							<input
								id="password"
								type="password"
								className="ud-input"
								value={form.password}
								onChange={event => setForm(prev => ({ ...prev, password: event.target.value }))}
								required
							/>
						</div>
						<div className="ud-field">
							<label htmlFor="email">邮箱（可选）</label>
							<input
								id="email"
								className="ud-input"
								value={form.email}
								onChange={event => setForm(prev => ({ ...prev, email: event.target.value }))}
							/>
						</div>

						{!domainCode
							? (
								<div className="ud-field">
									<label htmlFor="domainId">开放业务域（可选）</label>
									<select
										id="domainId"
										className="ud-select"
										value={form.domainId}
										onChange={event => setForm(prev => ({ ...prev, domainId: event.target.value }))}
									>
										<option value="">稍后在业务域页选择</option>
										{openDomains.map(domain => (
											<option key={domain.id} value={domain.id}>{domain.name}</option>
										))}
									</select>
								</div>
							)
							: null}

						<div className="ud-field">
							<label htmlFor="invitationCode">邀请码（可选）</label>
							<input
								id="invitationCode"
								className="ud-input"
								placeholder={dedicatedDomain?.registrationPolicy === "invitation_only" ? "该域需要邀请码" : "有邀请码可直接加入"}
								value={form.invitationCode}
								onChange={event => setForm(prev => ({ ...prev, invitationCode: event.target.value }))}
							/>
						</div>

						<button className="ud-btn ud-btn--primary ud-btn--block" type="submit" disabled={loading}>
							{loading ? "提交中…" : "注册并继续"}
						</button>
					</form>

					<p className="ud-muted" style={{ margin: 0, textAlign: "center" }}>
						已有账号？
						{" "}
						<Link to={loginPath}>去登录</Link>
					</p>
				</section>
			</div>
		</div>
	);
}
