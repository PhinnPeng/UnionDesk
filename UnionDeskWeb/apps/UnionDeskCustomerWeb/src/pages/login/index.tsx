import { fetchLoginConfig, loginCustomerLive, useCustomerPortal } from "@uniondesk/shared";
import {
	useEffect,
	useMemo,
	useState,
	type FormEvent,
	type MouseEvent,
} from "react";
import { Link, useNavigate, useParams } from "react-router-dom";

import { useToast } from "../../components/Toast";
import { enterDedicatedDomain, findDomainByCode } from "../../utils/domain-flow";

import { LoginCaptcha } from "./LoginCaptcha";
import "./login.css";

const REMEMBER_KEY = "ud_login_remembered";

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
	const [captchaEnabled, setCaptchaEnabled] = useState(false);
	const [captchaToken, setCaptchaToken] = useState("");
	const [captchaKey, setCaptchaKey] = useState(0);
	const [remember, setRemember] = useState(false);

	const registerPath = domainCode ? `/d/${domainCode}/register` : "/register";

	useEffect(() => {
		const remembered = window.localStorage.getItem(REMEMBER_KEY);
		if (remembered) {
			setLoginName(remembered);
			setRemember(true);
		}
	}, []);

	useEffect(() => {
		let cancelled = false;
		void fetchLoginConfig().then((config) => {
			if (cancelled) {
				return;
			}
			setCaptchaEnabled(Boolean(config.captchaEnabled));
		});
		return () => {
			cancelled = true;
		};
	}, []);

	const handleRememberChange = (checked: boolean) => {
		setRemember(checked);
		if (!checked) {
			window.localStorage.removeItem(REMEMBER_KEY);
		}
	};

	const handleForgotPassword = (event: MouseEvent<HTMLAnchorElement>) => {
		event.preventDefault();
		toast.error("忘记密码功能开发中，敬请期待");
	};

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
			if (remember) {
				window.localStorage.setItem(REMEMBER_KEY, loginName.trim());
			}
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
			<div className="auth__glow auth__glow--one" aria-hidden="true" />
			<div className="auth__glow auth__glow--two" aria-hidden="true" />
			<div className="auth__glow auth__glow--three" aria-hidden="true" />

			<header className="auth__top">
				<Link to="/login" className="auth__brand" aria-label="UnionDesk 首页">
					<span className="auth__mark" aria-hidden>U</span>
					<span className="auth__brand-name">UnionDesk</span>
				</Link>
			</header>

			<main className="auth__main">
				<section className="auth__card" aria-label="登录">
					<div className="auth__card-head">
						<h2 className="auth__card-title">欢迎回来！</h2>
						<p className="auth__card-desc">
							{dedicatedDomain
								? "验证通过后将直接进入当前业务域服务台"
								: "您发起的每一项请求，都能在这里实时追踪进展。"}
						</p>
						{dedicatedDomain
							? (
								<div className="auth__domain">
									专属入口 · {dedicatedDomain.name}
								</div>
							)
							: null}
					</div>

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
							hint="安全验证"
							disabled={loading}
							onVerified={setCaptchaToken}
							onError={message => toast.error(message)}
						/>
						<div className="auth__links">
							<label className="auth__remember">
								<input
									type="checkbox"
									checked={remember}
									onChange={event => handleRememberChange(event.target.checked)}
								/>
								<span>记住账号</span>
							</label>
							<div className="auth__links-right">
								<Link to={registerPath}>注册账号</Link>
								<span className="auth__links-sep" aria-hidden="true" />
								<a href="/login" onClick={handleForgotPassword}>忘记密码</a>
							</div>
						</div>
						<button
							className="auth__submit"
							type="submit"
							disabled={loading || (captchaEnabled && !captchaToken)}
						>
							{loading ? "正在登录…" : "登录"}
						</button>
					</form>

					<p className="auth__hint">演示：customer / customer123</p>
				</section>
			</main>

			<footer className="auth__foot">© 2026 UnionDesk · 客户服务中心</footer>
		</div>
	);
}
