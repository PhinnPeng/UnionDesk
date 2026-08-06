import { selectCustomerDomainLive, useCustomerPortal } from "@uniondesk/shared";
import { useState, type FormEvent } from "react";
import { useNavigate } from "react-router-dom";

import { useToast } from "../../components/Toast";

export default function DomainsPage() {
	const portal = useCustomerPortal();
	const toast = useToast();
	const navigate = useNavigate();
	const [invitationCode, setInvitationCode] = useState("");
	const [switchingId, setSwitchingId] = useState<number | null>(null);

	const joined = portal.domains.filter(item => item.joined);
	const joinable = portal.domains.filter(item => !item.joined && item.canJoin);
	const locked = portal.domains.filter(item => !item.joined && !item.canJoin);

	const enterDomain = async (domainId: number) => {
		setSwitchingId(domainId);
		try {
			await selectCustomerDomainLive(domainId);
			toast.success("已切换业务域");
			navigate("/home", { replace: true });
		}
		catch (error) {
			toast.error(error instanceof Error ? error.message : "切换失败");
		}
		finally {
			setSwitchingId(null);
		}
	};

	const joinDomain = (code: string) => {
		try {
			portal.joinDomainByInvitation({ invitationCode: code });
			toast.success("已加入业务域（本地演示）");
			navigate("/home", { replace: true });
		}
		catch (error) {
			toast.error(error instanceof Error ? error.message : "入域失败");
		}
	};

	const handleInviteSubmit = (event: FormEvent) => {
		event.preventDefault();
		if (!invitationCode.trim()) {
			toast.error("请输入邀请码");
			return;
		}
		joinDomain(invitationCode.trim());
	};

	return (
		<div className="ud-stack ud-stack--lg">
			<header>
				<p className="ud-kicker">业务域</p>
				<h1 className="ud-title">选择服务空间</h1>
				<p className="ud-subtitle">统一登录后在此选择业务域；专属链接会跳过这一步。</p>
			</header>

			<section className="ud-glass ud-glass--lg" style={{ padding: 18 }}>
				<h2 className="ud-section-title">邀请码加入</h2>
				<p className="ud-muted">拿到邀请码后可加入对应业务域（P1 将接真实入域 API）。</p>
				<form className="ud-row" onSubmit={handleInviteSubmit} style={{ marginTop: 12 }}>
					<input
						className="ud-input"
						style={{ flex: 1, minWidth: 180 }}
						placeholder="输入邀请码"
						value={invitationCode}
						onChange={event => setInvitationCode(event.target.value)}
					/>
					<button className="ud-btn ud-btn--primary" type="submit">加入</button>
				</form>
			</section>

			{joined.length > 0
				? (
					<section className="ud-stack">
						<h2 className="ud-section-title">已加入</h2>
						<div className="ud-domain-grid">
							{joined.map(domain => (
								<article key={domain.id} className="ud-glass ud-domain-card">
									<div className="ud-row">
										<span className="ud-tag ud-tag--green">已加入</span>
										{domain.selected ? <span className="ud-tag ud-tag--blue">当前</span> : null}
									</div>
									<h3 className="ud-ticket__title">{domain.name}</h3>
									<p className="ud-muted" style={{ margin: 0 }}>{domain.description || domain.code}</p>
									<button
										type="button"
										className="ud-btn ud-btn--primary"
										disabled={switchingId === domain.id}
										onClick={() => void enterDomain(domain.id)}
									>
										{switchingId === domain.id ? "切换中…" : domain.selected ? "进入首页" : "进入此域"}
									</button>
								</article>
							))}
						</div>
					</section>
				)
				: (
					<div className="ud-glass ud-empty">当前账号暂无已加入的业务域，请联系管理员开通。</div>
				)}

			{joinable.length > 0
				? (
					<section className="ud-stack">
						<h2 className="ud-section-title">可加入（演示）</h2>
						<div className="ud-domain-grid">
							{joinable.map(domain => (
								<article key={domain.id} className="ud-glass ud-domain-card">
									<span className={`ud-tag ${domain.registrationPolicy === "open" ? "ud-tag--green" : "ud-tag--orange"}`}>
										{domain.joinHint}
									</span>
									<h3 className="ud-ticket__title">{domain.name}</h3>
									<p className="ud-muted" style={{ margin: 0 }}>{domain.description}</p>
									<button
										type="button"
										className="ud-btn ud-btn--ghost"
										onClick={() => joinDomain(domain.invitationCode)}
									>
										{domain.registrationPolicy === "open" ? "直接加入" : "使用邀请加入"}
									</button>
								</article>
							))}
						</div>
					</section>
				)
				: null}

			{locked.length > 0
				? (
					<section className="ud-stack">
						<h2 className="ud-section-title">需管理员开通</h2>
						<div className="ud-domain-grid">
							{locked.map(domain => (
								<article key={domain.id} className="ud-glass ud-domain-card" style={{ opacity: 0.72 }}>
									<span className="ud-tag">仅管理员分配</span>
									<h3 className="ud-ticket__title">{domain.name}</h3>
									<p className="ud-muted" style={{ margin: 0 }}>{domain.description || domain.code}</p>
								</article>
							))}
						</div>
					</section>
				)
				: null}
		</div>
	);
}
