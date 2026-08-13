import type { CSSProperties, ReactNode } from "react";

import { createElement } from "react";

import { menuIcons } from "./menu-icons";
import { IconPlaceholder, parseIconValue, ReactIconRenderer } from "./render-icon";

/** 将菜单库中的 Ant Design 图标名或 Iconify 值解析为侧栏可渲染节点 */
export function resolveMenuIcon(iconName?: string | null, style?: CSSProperties): ReactNode {
	const trimmed = iconName?.trim();
	if (!trimmed) {
		return createElement(IconPlaceholder, { style });
	}
	if (parseIconValue(trimmed)) {
		return createElement(ReactIconRenderer, { iconValue: trimmed, style });
	}
	const AntdIcon = menuIcons[trimmed];
	if (AntdIcon) {
		return createElement(AntdIcon, { style: { fontSize: 16, ...style } });
	}
	return createElement(IconPlaceholder, { style });
}
