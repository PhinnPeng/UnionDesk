import logo from "#src/assets/svg/logo.svg?url";
import { usePreferences } from "#src/hooks/use-preferences";
import { resolveBackHomePath } from "#src/router/extra-info/app-scope";
import { cn } from "#src/utils/cn";

import { Typography } from "antd";
import { useMemo } from "react";
import { useNavigate } from "react-router";

import { headerHeight, tabbarHeight } from "../../constants";
import { SiderTrigger } from "../sider-trigger";

import "./index.less";

const { Title } = Typography;

export interface LogoProps {
	sidebarCollapsed: boolean
	className?: string
	/** 是否在标题栏右侧显示侧栏收缩按钮 */
	showSiderTrigger?: boolean
}

/**
 * 侧栏顶部标题栏：展开时 Logo+标题与收缩按钮横排；折叠时垂直排列，
 * 底部分割线与「顶栏+页签」底边对齐（无页签时对齐顶栏底边）。
 */
export function Logo({ sidebarCollapsed, className, showSiderTrigger = false }: LogoProps) {
	const navigate = useNavigate();
	const { tabbarEnable } = usePreferences();

	const collapsedBrandHeight = useMemo(() => {
		return headerHeight + (tabbarEnable ? tabbarHeight : 0);
	}, [tabbarEnable]);

	return (
		<div
			className={cn(
				"sidebar-brand",
				sidebarCollapsed && "sidebar-brand--collapsed",
				className,
			)}
			style={
				sidebarCollapsed && showSiderTrigger
					? { ["--ud-sidebar-brand-collapsed-h" as string]: `${collapsedBrandHeight}px` }
					: undefined
			}
		>
			<div
				style={{ height: headerHeight }}
				className="sidebar-brand__main"
				onClick={() => navigate(resolveBackHomePath())}
			>
				<img
					src={logo}
					alt="logo"
					width={32}
					height={32}
				/>

				<Title
					level={1}
					className={cn("sidebar-brand__title text-sm m-0", { hidden: sidebarCollapsed })}
					ellipsis
				>
					{import.meta.env.VITE_GLOB_APP_TITLE}
				</Title>
			</div>

			{showSiderTrigger
				? (
					<div className="sidebar-brand__trigger">
						<SiderTrigger variant="header" fillCollapsedHeight={sidebarCollapsed} />
					</div>
				)
				: null}
		</div>
	);
}
