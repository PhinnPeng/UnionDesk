import type { RoleItemType } from "#src/api/system/role";
import type { ProColumns } from "@ant-design/pro-components";
import type { TFunction } from "i18next";

import { Tag } from "antd";

function getScopeLabel(t: TFunction<"translation", undefined>, scope?: string) {
	switch (scope) {
		case "global":
			return t("system.role.scopeGlobal");
		case "domain":
			return t("system.role.scopeDomain");
		default:
			return scope || "-";
	}
}

function getScopeColor(scope?: string) {
	return scope === "global" ? "blue" : "green";
}

export function getConstantColumns(t: TFunction<"translation", undefined>): ProColumns<RoleItemType>[] {
	return [
		{
			dataIndex: "index",
			title: t("common.index"),
			valueType: "index",
			width: 80,
		},
		{
			title: t("system.role.name"),
			dataIndex: "name",
			ellipsis: true,
			width: 150,
		},
		{
			title: t("system.role.id"),
			dataIndex: "code",
			width: 150,
			ellipsis: true,
		},
		{
			title: t("system.role.scope"),
			dataIndex: "scope",
			width: 110,
			render: (_, record) => {
				const label = getScopeLabel(t, record.scope);
				return <Tag color={getScopeColor(record.scope)}>{label}</Tag>;
			},
		},
		{
			title: t("system.role.systemRole"),
			dataIndex: "system",
			width: 100,
			render: (_, record) => {
				return record.system
					? <Tag color="orange">{t("common.yes")}</Tag>
					: <Tag color="default">{t("common.no")}</Tag>;
			},
		},
	];
}
