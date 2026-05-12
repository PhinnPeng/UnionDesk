import type { CSSProperties } from "react";

import { Icon } from "@iconify/react";

/**
 * 图标存储格式：Iconify 标准格式 `"prefix:name"`，如 `"mdi:home"`
 */
export function parseIconValue(value: string | null | undefined): { prefix: string; name: string } | null {
	if (!value) return null;
	const idx = value.indexOf(":");
	if (idx < 1) return null;
	return { prefix: value.slice(0, idx), name: value.slice(idx + 1) };
}

export function serializeIconValue(icon: { prefix: string; name: string }): string {
	return `${icon.prefix}:${icon.name}`;
}

/**
 * 渲染 Iconify 图标
 */
export function ReactIconRenderer({ iconValue, style }: { iconValue: string; style?: CSSProperties }) {
	if (!parseIconValue(iconValue)) return null;
	return <Icon icon={iconValue} style={{ fontSize: 16, verticalAlign: "-0.125em", ...style }} />;
}

/**
 * 图标占位元素：当菜单项无图标时使用，保持与有图标项的视觉对齐
 */
export function IconPlaceholder({ style }: { style?: CSSProperties }) {
	return (
		<span
			aria-hidden="true"
			style={{
				display: "inline-block",
				width: 16,
				height: 16,
				verticalAlign: "-0.125em",
				...style,
			}}
		/>
	);
}
