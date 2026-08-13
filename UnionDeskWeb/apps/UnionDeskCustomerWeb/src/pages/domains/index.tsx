import { selectCustomerDomainLive, useCustomerPortal } from "@uniondesk/shared";
import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";

import { useToast } from "../../components/Toast";

export default function DomainsPage() {
	const portal = useCustomerPortal();
	const toast = useToast();
	const navigate = useNavigate();
	const [switchingId, setSwitchingId] = useState<number | null>(null);

	const joined = portal.domains.filter(item => item.joined);
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

	return (
		<div className="ud-stack ud-stack--lg">
			<header>
				<p className="ud-kicker">业务域</p>
				<h1 className="ud-title">选择服务空间</h1>
				<p className="ud-subtitle">统一登录后在此选择业务域；专属链接会跳过这一步。</p>
			</header>

			<section className="ud-glass ud-glass--lg" style={{ padding: 18 }}>
				<h2 className="ud-section-title">凭邀请码注册入域</h2>
				<p className="ud-muted">业务域入域通过注册完成（注册即入域）；持有邀请码时，可在注册页填写并加入对应业务域。</p>
				<Link className="ud-btn ud-btn--primary" to="/register" style={{ marginTop: 12 }}>去注册</Link>
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
