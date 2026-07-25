import { TicketAttributesPanel } from "#src/pages/platform/ticket-config/attributes/ticket-attributes-panel";

export interface DetailTicketAttributesProps {
	domainId: string;
}

export function DetailTicketAttributes({ domainId }: DetailTicketAttributesProps) {
	return <TicketAttributesPanel scope="domain" domainId={domainId} />;
}
