import type { MenuProps } from "antd";
import type { MenuItemType } from "../layout-menu/types";

import { Scrollbar } from "#src/components/scrollbar";
import { usePreferences } from "#src/hooks/use-preferences";
import { cn } from "#src/utils/cn";

import { theme as antdTheme, ConfigProvider, Typography } from "antd";

import { sidebarTitleHeight, siderTriggerHeight } from "../constants";
import LayoutMenu from "../layout-menu";
import { SidebarFooter } from "../widgets/sidebar-footer";
import { SiderTrigger } from "../widgets/sider-trigger";
import FirstColumnMenu from "./first-column-menu";

interface LayoutMixedSidebarProps {
	computedSidebarWidth?: number
	topNavItems?: MenuItemType[]
	sideNavItems?: MenuItemType[]
	sideNavMenuKeyInSplitMode?: string
	handleMenuSelect?: (key: string, mode: MenuProps["mode"]) => void
}

const emptyArray: MenuItemType[] = [];
const zero = 0;

/**
 * 双列布局侧边栏
 */
export default function LayoutMixedSidebar({
	computedSidebarWidth = zero,
	sideNavItems = emptyArray,
	topNavItems = emptyArray,
	handleMenuSelect,
	sideNavMenuKeyInSplitMode,
}: LayoutMixedSidebarProps) {
	const { isDark, sidebarTheme, sidebarCollapsed, firstColumnWidthInTwoColumnNavigation } = usePreferences();
	const {
		token: { Menu },
	} = antdTheme.useToken();
	const isFixedDarkTheme = isDark || sidebarTheme === "dark";
	const topChromeHeight = sidebarCollapsed
		? siderTriggerHeight
		: sidebarTitleHeight;

	return (
		<ConfigProvider
			theme={{
				algorithm: isFixedDarkTheme
					? antdTheme.darkAlgorithm
					: antdTheme.defaultAlgorithm,
			}}
		>
			<aside
				className="fixed left-0 top-0 bottom-0 flex"
				style={{
					backgroundColor: isFixedDarkTheme ? Menu?.darkItemBg : Menu?.itemBg,
					boxShadow: "3px 0 5px 0 rgb(29, 35, 41, 0.05)",
				}}
			>
				<FirstColumnMenu sideNavMenuKeyInSplitMode={sideNavMenuKeyInSplitMode} menus={topNavItems} handleMenuSelect={handleMenuSelect} />
				<div
					style={{ width: computedSidebarWidth - firstColumnWidthInTwoColumnNavigation }}
					className="relative flex flex-col transition-all"
				>
					<div
						className={cn(
							"flex shrink-0 border-b border-b-colorBorderSecondary",
							sidebarCollapsed ? "flex-col items-stretch" : "items-center justify-between",
						)}
						style={{
							minHeight: topChromeHeight,
							...(sidebarCollapsed ? undefined : { height: sidebarTitleHeight }),
						}}
					>
						{!sidebarCollapsed
							? (
								<Typography.Title level={1} ellipsis className="flex items-center my-0 pl-2 text-lg mx-3 flex-1 min-w-0" style={{ height: sidebarTitleHeight }}>
									{import.meta.env.VITE_GLOB_APP_TITLE}
								</Typography.Title>
							)
							: null}
						<div className={cn("flex justify-center", sidebarCollapsed && "border-t border-t-colorBorderSecondary")}>
							<SiderTrigger variant="header" />
						</div>
					</div>
					<div className="min-h-0 flex-1 overflow-hidden">
						<Scrollbar>
							<LayoutMenu
								autoExpandCurrentMenu
								menus={sideNavItems}
								handleMenuSelect={handleMenuSelect}
							/>
						</Scrollbar>
					</div>
					<SidebarFooter />
				</div>
			</aside>

		</ConfigProvider>
	);
}
