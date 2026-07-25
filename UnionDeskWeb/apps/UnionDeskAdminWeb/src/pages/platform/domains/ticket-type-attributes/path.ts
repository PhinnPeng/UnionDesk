export function buildTicketTypeAttributesPath(domainId: string, typeId: string) {
	return `/platform/domains/ticket-type-attributes/${encodeURIComponent(domainId)}/${encodeURIComponent(typeId)}`;
}
