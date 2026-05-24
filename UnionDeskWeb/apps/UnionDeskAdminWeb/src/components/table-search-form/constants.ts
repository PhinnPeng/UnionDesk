import type { QueryFilterProps } from "@ant-design/pro-components";

export const TABLE_SEARCH_FORM_SPAN = {
	xs: 24,
	sm: 12,
	md: 8,
	lg: 6,
	xl: 6,
	xxl: 6,
} as const satisfies NonNullable<QueryFilterProps["span"]>;

export function tableSearchFormCollapseRender(collapsed: boolean) {
	return collapsed ? "展开" : "收起";
}

export const TABLE_SEARCH_FORM_SEARCH_DEFAULTS = {
	labelWidth: "auto" as const,
	span: TABLE_SEARCH_FORM_SPAN,
	defaultCollapsed: true,
	searchText: "查询",
	resetText: "重置",
	collapseRender: tableSearchFormCollapseRender,
} satisfies Partial<QueryFilterProps>;
