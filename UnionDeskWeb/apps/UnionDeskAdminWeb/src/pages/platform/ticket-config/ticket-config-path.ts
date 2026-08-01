export const TICKET_CONFIG_BASE = "/platform/ticket-config";

export type TicketConfigSection = "attributes" | "types" | "statuses" | "templates";

export type PlatformTicketTypeConfigTab = "attributes" | "workflow" | "template";

export type TeamTemplateConfigModule = "basic" | "collaboration";

export const TICKET_TYPE_CONFIG_TABS: { key: PlatformTicketTypeConfigTab; label: string }[] = [
	{ key: "attributes", label: "属性" },
	{ key: "workflow", label: "工作流" },
	{ key: "template", label: "描述模板" },
];

const VALID_SECTIONS: TicketConfigSection[] = ["attributes", "types", "statuses", "templates"];

const VALID_TYPE_TABS: PlatformTicketTypeConfigTab[] = ["attributes", "workflow", "template"];

const VALID_TEMPLATE_MODULES: TeamTemplateConfigModule[] = ["basic", "collaboration"];

export function parseTicketConfigSection(value: string | null): TicketConfigSection | null {
	if (value && VALID_SECTIONS.includes(value as TicketConfigSection)) {
		return value as TicketConfigSection;
	}
	return null;
}

export function parsePlatformTicketTypeConfigTab(value: string | null): PlatformTicketTypeConfigTab {
	if (value && VALID_TYPE_TABS.includes(value as PlatformTicketTypeConfigTab)) {
		return value as PlatformTicketTypeConfigTab;
	}
	return "attributes";
}

export function parseTeamTemplateConfigModule(value: string | null): TeamTemplateConfigModule {
	if (value && VALID_TEMPLATE_MODULES.includes(value as TeamTemplateConfigModule)) {
		return value as TeamTemplateConfigModule;
	}
	return "collaboration";
}

export function buildTicketConfigPath(params: {
	section?: TicketConfigSection
	typeId?: string
	tab?: PlatformTicketTypeConfigTab
	templateId?: string
	module?: TeamTemplateConfigModule
}): string {
	const search = new URLSearchParams();
	if (params.section) {
		search.set("section", params.section);
	}
	if (params.typeId) {
		search.set("typeId", params.typeId);
	}
	if (params.tab) {
		search.set("tab", params.tab);
	}
	if (params.templateId) {
		search.set("templateId", params.templateId);
	}
	if (params.module) {
		search.set("module", params.module);
	}
	const query = search.toString();
	return query ? `${TICKET_CONFIG_BASE}?${query}` : TICKET_CONFIG_BASE;
}

/** 团队模板配置：与事项类型配置一致，走同一 pathname，避免新开顶栏页签 */
export function buildTeamTemplateConfigPath(
	templateId: string,
	module: TeamTemplateConfigModule = "collaboration",
): string {
	return buildTicketConfigPath({
		section: "templates",
		templateId: templateId.trim(),
		module,
	});
}

export function isTeamTemplateConfigPath(pathname: string): boolean {
	return /^\/platform\/ticket-config\/templates\/[^/]+/.test(pathname);
}

export function resolveEffectiveTicketConfigSection(
	section: TicketConfigSection | null,
	canViewAttributes: boolean,
	canViewTypes: boolean,
	canViewStatuses: boolean,
	canViewTemplates = false,
): TicketConfigSection {
	if (section === "attributes" && canViewAttributes) {
		return "attributes";
	}
	if (section === "types" && canViewTypes) {
		return "types";
	}
	if (section === "statuses" && canViewStatuses) {
		return "statuses";
	}
	if (section === "templates" && canViewTemplates) {
		return "templates";
	}
	if (canViewAttributes) {
		return "attributes";
	}
	if (canViewTypes) {
		return "types";
	}
	if (canViewStatuses) {
		return "statuses";
	}
	if (canViewTemplates) {
		return "templates";
	}
	return "attributes";
}
