import type { PlatformTicketType } from "@uniondesk/shared";

import {
	buildTicketConfigPath,
	type PlatformTicketTypeConfigTab,
} from "#src/pages/platform/ticket-config/ticket-config-path";

import type { NavigateFunction } from "react-router";

export type { PlatformTicketTypeConfigTab } from "#src/pages/platform/ticket-config/ticket-config-path";

export interface PlatformTicketTypeConfigLocationState {
	ticketType?: PlatformTicketType
}

/** @deprecated 使用 buildTicketConfigPath({ section: "types", typeId, tab }) */
export function buildPlatformTicketTypeConfigPath(typeId: string, tab: PlatformTicketTypeConfigTab): string {
	return buildTicketConfigPath({ section: "types", typeId, tab });
}

export function navigatePlatformTicketTypeConfig(
	navigate: NavigateFunction,
	record: PlatformTicketType,
	tab: PlatformTicketTypeConfigTab,
): void {
	const path = buildTicketConfigPath({ section: "types", typeId: record.id, tab });
	navigate(path, { replace: true, state: { ticketType: record } satisfies PlatformTicketTypeConfigLocationState });
}

/** @deprecated 使用 navigatePlatformTicketTypeConfig */
export function openPlatformTicketTypeConfigTab(
	navigate: NavigateFunction,
	record: PlatformTicketType,
	tab: PlatformTicketTypeConfigTab,
): void {
	navigatePlatformTicketTypeConfig(navigate, record, tab);
}
