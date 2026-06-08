import type { LoginInfo } from "#src/api/user";

import { fetchLoginConfig } from "#src/api/auth";
import { BasicButton } from "#src/components/basic-button";
import { PASSWORD_RULES, USERNAME_RULES } from "#src/constants/rules";
import { useAuthStore } from "#src/store/auth";
import { resolveBackHomePath } from "#src/router/extra-info/app-scope";

import { Button, Form, Input, message, Space } from "antd";
import { use, useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { useNavigate, useSearchParams } from "react-router";

import { FormModeContext } from "../form-mode-context";
import { LoginCaptcha } from "./login-captcha";
import { resolveRequestErrorMessage } from "#src/utils/resolve-request-error";

import { buildPasswordLoginPayload } from "../utils";

const FORM_INITIAL_VALUES: LoginInfo = {
	username: "admin",
	password: "admin123",
};

const DEFAULT_CAPTCHA_HINT = "请拖动滑块完成验证";

export function PasswordLogin() {
	const [loading, setLoading] = useState(false);
	const [configLoading, setConfigLoading] = useState(true);
	const [captchaEnabled, setCaptchaEnabled] = useState(false);
	const [captchaHint, setCaptchaHint] = useState<string>("");
	const [captchaToken, setCaptchaToken] = useState("");
	const [captchaKey, setCaptchaKey] = useState(0);
	const [passwordLoginForm] = Form.useForm();
	const { t } = useTranslation();
	const [messageLoadingApi, contextLoadingHolder] = message.useMessage();
	const [searchParams] = useSearchParams();
	const navigate = useNavigate();
	const login = useAuthStore(state => state.login);
	const { setFormMode } = use(FormModeContext);

	useEffect(() => {
		let active = true;

		const loadLoginConfig = async () => {
			try {
				const config = await fetchLoginConfig();
				if (!active) {
					return;
				}
				setCaptchaEnabled(config.captchaEnabled);
				setCaptchaHint(config.captchaHint?.trim() || DEFAULT_CAPTCHA_HINT);
			}
			catch (error) {
				if (active) {
					setCaptchaEnabled(true);
					setCaptchaHint(DEFAULT_CAPTCHA_HINT);
				}
			}
			finally {
				if (active) {
					setConfigLoading(false);
				}
			}
		};

		void loadLoginConfig();
		return () => {
			active = false;
		};
	}, []);

	const handleFinish = async (values: LoginInfo) => {
		if (configLoading) {
			return;
		}
		if (captchaEnabled && !captchaToken) {
			window.$message?.warning("请先完成滑块验证");
			return;
		}

		setLoading(true);
		messageLoadingApi?.loading(t("authority.loginInProgress"), 0);

		try {
			await login(buildPasswordLoginPayload(values, captchaToken));
			messageLoadingApi?.destroy();
			window.$message?.success(t("authority.loginSuccess"));
			const redirect = searchParams.get("redirect")?.trim();
			navigate(redirect && redirect.length > 0 ? redirect : resolveBackHomePath(), { replace: true });
		}
		catch (error) {
			messageLoadingApi?.destroy();
			window.$message?.error(resolveRequestErrorMessage(error, "登录失败，请检查账号、密码与验证码后重试"));
			setCaptchaToken("");
			setCaptchaKey(key => key + 1);
		}
		finally {
			messageLoadingApi?.destroy();
			setTimeout(() => {
				window.$message?.destroy();
				setLoading(false);
			}, 1000);
		}
	};

	return (
		<>
			{contextLoadingHolder}
			<Space orientation="vertical">
				<h2 className="text-colorText mb-3 text-3xl font-bold leading-9 tracking-tight lg:text-4xl">
					{t("authority.welcomeBack")}
					&nbsp;
					👋
				</h2>
				<p className="lg:text-base text-sm text-colorTextSecondary">
					{t("authority.loginDescription")}
				</p>
			</Space>

			<Form
				name="passwordLoginForm"
				form={passwordLoginForm}
				layout="vertical"
				initialValues={FORM_INITIAL_VALUES}
				onFinish={handleFinish}
			>
				<Form.Item
					label={t("authority.username")}
					name="username"
					rules={USERNAME_RULES(t)}
				>
					<Input placeholder={t("form.username.required")} />
				</Form.Item>

				<Form.Item
					label={t("authority.password")}
					name="password"
					rules={PASSWORD_RULES(t)}
				>
					<Input.Password placeholder={t("form.password.required")} />
				</Form.Item>

				<div className="mb-5 -mt-1">
					<LoginCaptcha
						key={captchaKey}
						enabled={!configLoading && captchaEnabled}
						hint={captchaHint}
						disabled={loading}
						onVerified={setCaptchaToken}
						onError={(messageText) => {
							setCaptchaToken("");
							window.$message?.error(messageText);
						}}
					/>
				</div>

				<Form.Item>
					<div className="mb-5 -mt-1 flex justify-between text-sm">
						<BasicButton
							type="link"
							className="p-0"
							onPointerDown={() => {
								setFormMode("codeLogin");
							}}
						>
							{t("authority.codeLogin")}
						</BasicButton>
						<BasicButton
							type="link"
							className="p-0"
							onPointerDown={() => {
								setFormMode("forgotPassword");
							}}
						>
							{t("authority.forgotPassword")}
						</BasicButton>
					</div>
					<Button
						block
						type="primary"
						htmlType="submit"
						loading={loading}
						disabled={configLoading || (captchaEnabled && !captchaToken)}
					>
						{t("authority.login")}
					</Button>
				</Form.Item>

				<div className="text-center text-sm">
					{t("authority.noAccountYet")}
					<BasicButton
						type="link"
						className="px-1"
						onPointerDown={() => {
							setFormMode("register");
						}}
					>
						{t("authority.goToRegister")}
					</BasicButton>
				</div>
			</Form>
		</>
	);
}
