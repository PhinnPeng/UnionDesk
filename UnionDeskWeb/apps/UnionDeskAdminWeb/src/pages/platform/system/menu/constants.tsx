import type { MenuItemType } from "#src/api/system/menu";
import type { ProColumns } from "@ant-design/pro-components";
import type { TFunction } from "i18next";

import { parseIconValue, ReactIconRenderer } from "#src/icons/render-icon";
import { Tag } from "antd";

/**
 * 菜单类型标签（模块化组件设计）
 */
function MenuTypeTag({ type, t }: { type?: string; t: TFunction<"translation", undefined> }) {
	switch (type) {
		case "catalog":
			return <Tag color="cyan">{t("system.menu.catalog")}</Tag>;
		case "menu":
			return <Tag color="blue">{t("system.menu.menu")}</Tag>;
		case "button":
			return <Tag color="orange">{t("system.menu.button")}</Tag>;
		default:
			return <span>{type || "-"}</span>;
	}
}

export function getMenuTypeOptions(t: TFunction<"translation", undefined>) {
	return [
		{
			label: t("system.menu.catalog"),
			value: "catalog",
		},
		{
			label: t("system.menu.menu"),
			value: "menu",
		},
		{
			label: t("system.menu.button"),
			value: "button",
		},
	];
}

export function getConstantColumns(t: TFunction<"translation", undefined>): ProColumns<MenuItemType>[] {
	const columns: ProColumns<MenuItemType>[] = [
		{
			dataIndex: "index",
			title: t("common.index"),
			valueType: "index",
			width: 80,
		},
		{
			title: t("system.menu.name"),
			dataIndex: "name",
			ellipsis: true,
			width: 180,
			render: (_, record) => record.name,
			formItemProps: {
				rules: [
					{
						required: true,
						message: t("form.required"),
					},
				],
			},
		},
		{
			title: t("system.menu.menuType"),
			dataIndex: "nodeType",
			width: 110,
			render: (_, record) => <MenuTypeTag type={record.nodeType} t={t} />,
		},
		{
			title: t("system.menu.routePath"),
			dataIndex: "routePath",
			width: 180,
			ellipsis: true,
		},
		{
			title: t("system.menu.componentUrl"),
			dataIndex: "componentKey",
			width: 180,
			ellipsis: true,
			search: false,
			render: (_, record) => record.componentKey?.replace(/^\.\//, "") ?? "-",
		},
		{
			title: t("system.menu.permissionCode"),
			dataIndex: "permissionCode",
			width: 180,
			ellipsis: true,
		},
		{
			title: t("system.menu.menuOrder"),
			dataIndex: "orderNo",
			valueType: "digit",
			width: 90,
		},
		{
			title: t("system.menu.menuIcon"),
			dataIndex: "icon",
			width: 130,
			search: false,
			render: (_, record) => {
				const iconName = record.icon?.trim();
				if (!iconName) {
					return "-";
				}
				if (!parseIconValue(iconName)) {
					return <span title={iconName}>-</span>;
				}
				return <span title={iconName}><ReactIconRenderer iconValue={iconName} style={{ fontSize: 18 }} /></span>;
			},
		},
		{
			title: t("system.menu.scope"),
			dataIndex: "scope",
			width: 110,
			render: (_, record) => {
				if (record.scope === "platform") {
					return <Tag color="blue">{t("system.menu.platformScope")}</Tag>;
				}
				if (record.scope === "business") {
					return <Tag color="green">{t("system.menu.businessScope")}</Tag>;
				}
				return "-";
			},
		},
		{
			title: t("system.menu.hideInMenu"),
			dataIndex: "hidden",
			width: 110,
			render: (_, record) => {
				const hiddenLabel = record.hidden ? t("common.yes") : t("common.no");
				return <Tag color={record.hidden ? "orange" : "green"}>{hiddenLabel}</Tag>;
			},
		},
		{
			disable: true,
			title: t("common.status"),
			dataIndex: "status",
			valueType: "select",
			width: 90,
			render: (text, record) => {
				return <Tag color={record.status === 1 ? "success" : "default"}>{text}</Tag>;
			},
			valueEnum: {
				1: {
					text: t("common.enabled"),
				},
				0: {
					text: t("common.deactivated"),
				},
			},
		},
	];

	return columns;
}
