import { Scrollbar } from "#src/components/scrollbar";
import { usePreferences } from "#src/hooks/use-preferences";

import { theme as antdTheme, ConfigProvider } from "antd";

import { Logo } from "../widgets/logo";
import { SidebarFooter } from "../widgets/sidebar-footer";

export interface LayoutSidebarProps {
	children?: React.ReactNode
	computedSidebarWidth: number
}

export default function LayoutSidebar({ children, computedSidebarWidth }: LayoutSidebarProps) {
	const { sidebarCollapsed, sidebarTheme, isDark, sideCollapsedWidth } = usePreferences();
	const {
		token: { Menu },
	} = antdTheme.useToken();

	const isFixedDarkTheme = isDark || sidebarTheme === "dark";

	return (
		<ConfigProvider
			theme={{
				algorithm: isFixedDarkTheme
					? antdTheme.darkAlgorithm
					: antdTheme.defaultAlgorithm,
				components: {
					Menu: {
						collapsedWidth: sideCollapsedWidth,
						itemHeight: 40,
					},
				},
			}}
		>
			<aside
				style={
					{
						// 一个像素的 border
						width: computedSidebarWidth + 1,
						backgroundColor: isFixedDarkTheme ? Menu?.darkItemBg : Menu?.itemBg,
						boxShadow: "3px 0 5px 0 rgb(29, 35, 41, 0.05)",
					}
				}
				className="fixed top-0 bottom-0 left-0 flex flex-col overflow-hidden transition-all border-r border-r-colorBorderSecondary"
			>
				<Logo sidebarCollapsed={sidebarCollapsed} showSiderTrigger />
				<div className="min-h-0 flex-1 overflow-hidden">
					<Scrollbar>
						{children}
					</Scrollbar>
				</div>
				<SidebarFooter />
			</aside>
		</ConfigProvider>
	);
}
