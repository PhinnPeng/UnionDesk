import type { ButtonProps } from "antd";
import { useDeviceType } from "#src/hooks/use-device-type";
import { useAppScope } from "#src/hooks/use-app-scope";
import { usePreferences } from "#src/hooks/use-preferences";
import { useLayout } from "#src/layout/hooks/use-layout";
import { GlobalSearch } from "#src/layout/widgets/global-search";
import { NotificationContainer } from "#src/layout/widgets/notification/notification-container";
import { Preferences } from "#src/layout/widgets/preferences";
import { useTabsStore } from "#src/store/tabs";
import { cn } from "#src/utils/cn";

import { MenuFoldOutlined, MenuUnfoldOutlined } from "@ant-design/icons";
import { theme as antdTheme, Button, ConfigProvider, theme } from "antd";

import { headerHeight } from "../constants";
import { FullscreenButton } from "./components/fullscreen-button";
import { LanguageButton } from "./components/language-button";
import { PlatformEntryButton } from "./components/platform-entry-button";
import { ThemeButton } from "./components/theme-button";
import { UserMenu } from "./components/user-menu";

export interface LayoutHeaderProps {
	className?: string
	children?: React.ReactNode
}

const buttonProps: ButtonProps = {
	size: "large",
	className: "px-[11px]",
};

export default function LayoutHeader({ className, children }: LayoutHeaderProps) {
	const {
		token: { Menu },
	} = theme.useToken();
	const appScope = useAppScope();
	const {
		sidebarCollapsed,
		setPreferences,
		isDark,
		sidebarTheme,
	} = usePreferences();
	const { isMobile } = useDeviceType();
	const isMaximize = useTabsStore(state => state[appScope].isMaximize);
	const { isTopNav, isMixedNav } = useLayout();
	const isFixedDarkTheme = isDark || (sidebarTheme === "dark" && (isMixedNav || isTopNav));
	/** 有侧栏/移动端抽屉底部坞时入口在侧栏；纯顶栏导航仍保留顶栏入口 */
	const showHeaderAccountEntries = isTopNav;

	return (
		<ConfigProvider
			theme={{
				algorithm: isFixedDarkTheme
					? antdTheme.darkAlgorithm
					: antdTheme.defaultAlgorithm,
			}}
		>
			<header
				className={cn(
					"flex-shrink-0 flex gap-5 justify-between items-center transition-all md:px-4",
					{ "overflow-hidden": isMaximize },
					className,
				)}
				style={{
					background: isFixedDarkTheme ? Menu?.darkItemBg : Menu?.itemBg,
					height: isMaximize ? 0 : headerHeight,
				}}
			>

				{
					isMobile
						? (
							<Button
								type="text"
								icon={sidebarCollapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
								onClick={() => setPreferences("sidebarCollapsed", !sidebarCollapsed)}
								className="h-full"
							/>
						)
						: null
				}

				<div className="flex items-center grow h-full overflow-hidden">
					{children}
				</div>

				<div className="flex items-center">
					<GlobalSearch />
					{/* 抽屉始终挂载；由用户菜单打开，顶栏不再展示设置按钮 */}
					<Preferences hideTrigger />
					<ThemeButton {...buttonProps} />
					<LanguageButton {...buttonProps} />
					<FullscreenButton {...buttonProps} target={document.documentElement} />
					<NotificationContainer {...buttonProps} />
					{showHeaderAccountEntries ? <PlatformEntryButton {...buttonProps} /> : null}
					{showHeaderAccountEntries ? <UserMenu {...buttonProps} /> : null}
				</div>
			</header>
		</ConfigProvider>
	);
}
