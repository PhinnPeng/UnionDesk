export const TICKET_CONFIG_BASE = "/platform/ticket-config";

export type TicketConfigSection = "attributes" | "types" | "statuses";

export type PlatformTicketTypeConfigTab = "attributes" | "workflow" | "template";

export const TICKET_TYPE_CONFIG_TABS: { key: PlatformTicketTypeConfigTab; label: string }[] = [
	{ key: "attributes", label: "属性" },
	{ key: "workflow", label: "工作流" },
	{ key: "template", label: "描述模板" },
];

const VALID_SECTIONS: TicketConfigSection[] = ["attributes", "types", "statuses"];

const VALID_TYPE_TABS: PlatformTicketTypeConfigTab[] = ["attributes", "workflow", "template"];

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

export function buildTicketConfigPath(params: {
	section?: TicketConfigSection
	typeId?: string
	tab?: PlatformTicketTypeConfigTab
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
	const query = search.toString();
	return query ? `${TICKET_CONFIG_BASE}?${query}` : TICKET_CONFIG_BASE;
}

export function resolveEffectiveTicketConfigSection(
	section: TicketConfigSection | null,
	canViewAttributes: boolean,
	canViewTypes: boolean,
	canViewStatuses: boolean,
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
	if (canViewAttributes) {
		return "attributes";
	}
	if (canViewTypes) {
		return "types";
	}
	if (canViewStatuses) {
		return "statuses";
	}
	return "attributes";
}
