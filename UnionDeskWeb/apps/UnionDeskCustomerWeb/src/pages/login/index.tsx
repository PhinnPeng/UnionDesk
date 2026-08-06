import { fetchLoginConfig, loginCustomerLive, useCustomerPortal } from "@uniondesk/shared";
import { useEffect, useMemo, useState, type FormEvent } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";

import { useToast } from "../../components/Toast";
import { enterDedicatedDomain, findDomainByCode } from "../../utils/domain-flow";

import slideNotify from "./assets/slide-notify.png";
import slideSubmit from "./assets/slide-submit.png";
import slideTrack from "./assets/slide-track.png";
import { LoginCaptcha } from "./LoginCaptcha";
import "./login.css";

const SLIDES = [
	{
		title: "把问题说清楚，我们马上跟进",
		text: "选择服务类型，填写现象与期望。提交后立刻拿到工单号，进度随时可查。",
		image: slideSubmit,
		alt: "提交服务请求示意",
	},
	{
		title: "公开回复清晰可见",
		text: "客服进展、补充请求都在时间线里。需要时一键补充说明，不用反复追问。",
		image: slideTrack,
		alt: "工单进度跟踪示意",
	},
	{
		title: "重要更新不会错过",
		text: "工单变更与系统消息汇入通知中心。点开即可回到对应服务记录。",
		image: slideNotify,
		alt: "服务通知提醒示意",
	},
] as const;

export default function LoginPage() {
	const portal = useCustomerPortal();
	const toast = useToast();
	const navigate = useNavigate();
	const params = useParams();
	const domainCode = params.domainCode;
	const dedicatedDomain = useMemo(
		() => findDomainByCode(portal.domains, domainCode),
		[domainCode, portal.domains],
	);

	const [loginName, setLoginName] = useState("");
	const [password, setPassword] = useState("");
	const [loading, setLoading] = useState(false);
	const [slideIndex, setSlideIndex] = useState(0);
	const [captchaEnabled, setCaptchaEnabled] = useState(false);
	const [captchaHint, setCaptchaHint] = useState<string | null>(null);
	const [captchaToken, setCaptchaToken] = useState("");
	const [captchaKey, setCaptchaKey] = useState(0);

	const registerPath = domainCode ? `/d/${domainCode}/register` : "/register";

	useEffect(() => {
		let cancelled = false;
		void fetchLoginConfig().then((config) => {
			if (cancelled) {
				return;
			}
			setCaptchaEnabled(Boolean(config.captchaEnabled));
			setCaptchaHint(config.captchaHint ?? null);
		});
		return () => {
			cancelled = true;
		};
	}, []);

	useEffect(() => {
		const reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
		if (reduceMotion) {
			return;
		}
		const timer = window.setInterval(() => {
			setSlideIndex(prev => (prev + 1) % SLIDES.length);
		}, 4200);
		return () => window.clearInterval(timer);
	}, []);

	const handleSubmit = async (event: FormEvent) => {
		event.preventDefault();
		if (captchaEnabled && !captchaToken) {
			toast.error("请先完成滑动验证");
			return;
		}
		setLoading(true);
		try {
			const { snapshot, riskLoginNotified } = await loginCustomerLive({
				loginName: loginName.trim(),
				password,
				captchaToken: captchaToken || undefined,
			});
			if (riskLoginNotified) {
				toast.success("检测到新登录环境，已发送站内提醒");
			}
			if (domainCode) {
				const result = await enterDedicatedDomain(domainCode);
				if (result.ok) {
					toast.success(`已进入 ${dedicatedDomain?.name ?? "业务域"}`);
					navigate(result.path, { replace: true });
				}
				else {
					toast.error(result.message);
					navigate(result.path, { replace: true });
				}
				return;
			}
			if (!riskLoginNotified) {
				toast.success("登录成功");
			}
			navigate(snapshot.activeDomain ? "/home" : "/domains", { replace: true });
		}
		catch (error) {
			setCaptchaToken("");
			setCaptchaKey(prev => prev + 1);
			toast.error(error instanceof Error ? error.message : "登录失败");
		}
		finally {
			setLoading(false);
		}
	};

	return (
		<div className="auth">
			<main className="auth__main">
				<div className="auth__dock">
					<section className="auth__story" aria-label="产品介绍">
						<header className="auth__top">
							<Link to="/login" className="auth__brand" aria-label="UnionDesk 首页">
								<span className="auth__mark" aria-hidden>U</span>
								<span className="auth__brand-name">UnionDesk</span>
							</Link>
						</header>

						<div className="auth__story-body">
							<div className="auth__story-inner">
								<h1 className="auth__headline">
									服务请求，
									<em>清晰可跟</em>
								</h1>
								<p className="auth__lead">
									提交工单、查看进展、接收通知。登录后即可开始。
								</p>

								<div className="auth__carousel" aria-live="polite">
									{SLIDES.map((slide, index) => (
										<article
											key={slide.title}
											className={`auth__slide${index === slideIndex ? " is-active" : ""}`}
											aria-hidden={index !== slideIndex}
										>
											<figure className="auth__slide-figure">
												<img
													className="auth__slide-image"
													src={slide.image}
													alt={slide.alt}
													width={960}
													height={540}
													decoding="async"
												/>
												<figcaption className="auth__slide-caption">
													<h2 className="auth__slide-title">{slide.title}</h2>
													<p className="auth__slide-text">{slide.text}</p>
												</figcaption>
											</figure>
										</article>
									))}
								</div>
							</div>
						</div>
					</section>

					<section className="auth__form-panel" aria-label="登录">
						<div className="auth__form-stack">
							<h2 className="auth__card-title">登录</h2>
							<p className="auth__card-desc">
								{dedicatedDomain
									? "验证通过后将直接进入当前业务域服务台"
									: "使用客户账号登录，随后选择业务域"}
							</p>
							{dedicatedDomain
								? (
									<div className="auth__domain">
										专属入口 · {dedicatedDomain.name}
									</div>
								)
								: null}

							<form className="auth__form" onSubmit={handleSubmit}>
								<div className="auth__field">
									<label htmlFor="loginName">账号</label>
									<input
										id="loginName"
										name="loginName"
										value={loginName}
										onChange={event => setLoginName(event.target.value)}
										placeholder="手机号或登录名"
										autoComplete="username"
										autoCapitalize="none"
										spellCheck={false}
										required
									/>
								</div>
								<div className="auth__field">
									<label htmlFor="password">密码</label>
									<input
										id="password"
										name="password"
										type="password"
										value={password}
										onChange={event => setPassword(event.target.value)}
										placeholder="输入密码"
										autoComplete="current-password"
										required
									/>
								</div>
								<LoginCaptcha
									key={captchaKey}
									enabled={captchaEnabled}
									hint={captchaHint}
									disabled={loading}
									onVerified={setCaptchaToken}
									onError={message => toast.error(message)}
								/>
								<button
									className="auth__submit"
									type="submit"
									disabled={loading || (captchaEnabled && !captchaToken)}
								>
									{loading ? "正在登录…" : "继续"}
								</button>
							</form>

							<div className="auth__links">
								<Link to={registerPath}>创建账号</Link>
								{domainCode
									? <Link to="/login">统一登录</Link>
									: <Link to="/d/online-service/login">专属入口</Link>}
							</div>
							<p className="auth__hint">演示：customer / customer123</p>
						</div>
					</section>
				</div>
			</main>
		</div>
	);
}
