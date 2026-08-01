import type { ButtonProps } from "antd";

import { BasicButton } from "#src/components/basic-button";
import { useDeviceType } from "#src/hooks/use-device-type";
import { usePreferences } from "#src/hooks/use-preferences";
import { loginPath } from "#src/router/extra-info";
import { useAuthStore } from "#src/store/auth";
import { usePreferencesStore } from "#src/store/preferences";

import { CopyOutlined, RedoOutlined, RocketOutlined, SettingOutlined } from "@ant-design/icons";
import { theme as antdTheme, Badge, ConfigProvider, Divider, Drawer, FloatButton } from "antd";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router";

import {
	Animation,
	BuiltinTheme,
	General,
	PreferencesFooter,
	PreferencesLayout,
	Sidebar,
	SiteTheme,
	Tabbar,
} from "./blocks";
import { usePreferencesDrawerStore } from "./preferences-drawer-store";

const preferencesContentId = "__react-antd-admin__preferences_drawer__";

interface PreferencesProps extends ButtonProps {
	/** 仅挂载抽屉，不渲染顶栏设置按钮（由用户菜单打开） */
	hideTrigger?: boolean
}

export function Preferences({ hideTrigger = false, ...restProps }: PreferencesProps) {
	const { t } = useTranslation();
	const navigate = useNavigate();
	const isOpen = usePreferencesDrawerStore(state => state.open);
	const setOpen = usePreferencesDrawerStore(state => state.setOpen);
	const openDrawer = usePreferencesDrawerStore(state => state.openDrawer);
	const { isMobile } = useDeviceType();
	const { reset, isDefault, isDark } = usePreferences();
	const preferences = usePreferencesStore();
	const logout = useAuthStore(state => state.logout);

	const clearAndLogout = async () => {
		await logout();
		usePreferencesStore.persist.clearStorage();
		navigate(loginPath);
	};

	const handleCopyPreferences = async () => {
		const data = JSON.stringify(preferences, null, 2);
		await navigator.clipboard.writeText(data);
		window.$modal?.success?.({
			title: t("preferences.copyPreferencesSuccessTitle"),
			content: t("preferences.copyPreferencesSuccess"),
		});
	};

	return (
		<>
			{hideTrigger
				? null
				: (
					<BasicButton
						type="text"
						{...restProps}
						onClick={(e) => {
							restProps?.onClick?.(e);
							openDrawer();
						}}
					>
						<SettingOutlined />
					</BasicButton>
				)}
			<ConfigProvider
				theme={{
					/**
					 * 当侧边栏深色模式，且是顶部导航或者混合导航时，会影响下面组件的样式，所以这里要重置算法
					 */
					algorithm: isDark
						? antdTheme.darkAlgorithm
						: antdTheme.defaultAlgorithm,
				}}
			>

				<Drawer
					title={t("preferences.title")}
					placement="right"
					onClose={() => {
						setOpen(false);
					}}
					extra={(
						<Badge
							style={{ width: 8, height: 8 }}
							dot={!isDefault}
							color="blue"
							offset={[-5, 5]}
						>
							<BasicButton
								onPointerDown={() => !isDefault && reset()}
								type="text"
								icon={<RedoOutlined rotate={270} />}
							/>
						</Badge>
					)}
					footer={(
						<div className="flex justify-between">
							<BasicButton
								icon={<CopyOutlined rotate={180} />}
								onPointerDown={handleCopyPreferences}
							>
								{t("preferences.copyPreferences")}
							</BasicButton>
							<BasicButton
								type="text"
								onPointerDown={clearAndLogout}
							>
								{t("preferences.clearAndLogout")}
							</BasicButton>
						</div>
					)}
					{...(isMobile
						? {
							width: "100vw",
						}
						: {})}
					open={isOpen}
					id={preferencesContentId}
				>
					<div
						style={{
							display: "flex",
							flexDirection: "column",
							alignItems: "center",
						}}
					>
						<Divider>{t("preferences.general.title")}</Divider>
						<General />
						<Divider>{t("preferences.theme.title")}</Divider>
						<SiteTheme />
						<Divider>{t("preferences.theme.builtin.title")}</Divider>
						<BuiltinTheme />
						<Divider>{t("preferences.layout.title")}</Divider>
						<PreferencesLayout />
						<Divider>{t("preferences.sidebar.title")}</Divider>
						<Sidebar />
						<Divider>{t("preferences.tabbar.title")}</Divider>
						<Tabbar />
						<Divider>{t("preferences.animation.title")}</Divider>
						<Animation />
						<Divider>{t("preferences.footer.title")}</Divider>
						<PreferencesFooter />
					</div>
					<FloatButton.BackTop
						icon={<RocketOutlined />}
						target={() => document.querySelector(`#${preferencesContentId} .ant-drawer-body`) as HTMLElement}
					/>
				</Drawer>
			</ConfigProvider>
		</>
	);
}
