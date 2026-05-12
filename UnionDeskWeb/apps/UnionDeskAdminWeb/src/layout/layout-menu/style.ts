import { createUseStyles } from "react-jss";

export const useStyles = createUseStyles({
	menuBackgroundColor: {
		"& .ant-menu-submenu-selected .ant-menu-submenu-title": {
			backgroundColor: "var(--ant-menu-item-selected-bg)",
		},
	},
	/**
	 * 解决 Iconify 图标在 antd Menu 中与文字贴近的问题
	 * antd 6 不再把 icon prop 自动包装为 .ant-menu-item-icon span，
	 * 所以使用通用兄弟选择器，匹配任意前置元素（svg/img/span）+ 文字容器
	 */
	iconSpacing: {
		"& .ant-menu-item > * + .ant-menu-title-content, & .ant-menu-submenu-title > * + .ant-menu-title-content": {
			marginInlineStart: 10,
		},
	},
	collapsedMenu: {
		width: "100%",

		"&.ant-menu-inline-collapsed > .ant-menu-item, &.ant-menu-inline-collapsed > .ant-menu-submenu > .ant-menu-submenu-title": {
			display: "flex",
			alignItems: "center",
			justifyContent: "center",
			width: 40,
			height: 40,
			marginBlock: 4,
			marginInline: "auto",
			paddingInline: "0 !important",
			borderRadius: 10,
			lineHeight: "40px",
		},

		"&.ant-menu-inline-collapsed > .ant-menu-item .ant-menu-item-icon, &.ant-menu-inline-collapsed > .ant-menu-submenu > .ant-menu-submenu-title .ant-menu-item-icon": {
			marginInlineEnd: 0,
			fontSize: 16,
		},

		"&.ant-menu-inline-collapsed .ant-menu-title-content": {
			display: "none",
		},
	},
});
