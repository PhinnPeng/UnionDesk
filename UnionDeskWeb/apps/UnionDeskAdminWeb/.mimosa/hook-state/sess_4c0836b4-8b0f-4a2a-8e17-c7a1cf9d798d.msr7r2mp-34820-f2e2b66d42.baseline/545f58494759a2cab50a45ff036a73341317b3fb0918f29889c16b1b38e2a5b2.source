import { BasicButton } from "#src/components/basic-button";

import { usePreferences } from "#src/hooks/use-preferences";
import { cn } from "#src/utils/cn";

import { MenuFoldOutlined, MenuUnfoldOutlined } from "@ant-design/icons";

import { siderTriggerHeight } from "../../constants";

interface SiderTriggerProps {
	className?: string
	/** header：顶栏内联；footer：侧栏底栏全宽（兼容旧用法） */
	variant?: "header" | "footer"
	/** 收起态顶区：按钮高度铺满 trigger 行，使品牌区总高对齐顶栏+页签 */
	fillCollapsedHeight?: boolean
}

export function SiderTrigger({
	className,
	variant = "header",
	fillCollapsedHeight = false,
}: SiderTriggerProps) {
	const { sidebarCollapsed, setPreferences, sidebarTheme } = usePreferences();
	const isHeader = variant === "header";
	const height = isHeader && fillCollapsedHeight ? "100%" : siderTriggerHeight;

	return (
		<BasicButton
			type="text"
			style={{
				height,
				...(isHeader ? undefined : { boxShadow: "0px -3px 5px 0 rgb(29, 35, 41, 0.05)" }),
			}}
			icon={sidebarCollapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
			onClick={() => setPreferences("sidebarCollapsed", !sidebarCollapsed)}
			className={cn(
				"rounded-none",
				isHeader
					? cn("shrink-0", fillCollapsedHeight ? "w-full" : "w-10")
					: cn(
						"w-full border-t",
						sidebarTheme === "dark" ? "border-t-[#303030]" : "border-t-colorBorderSecondary",
					),
				className,
			)}
		/>
	);
}
