export type TicketTypeTemplateKey = "simple_ticket" | "standard_ticket";

export interface TicketTypeTemplateOption {
	key: TicketTypeTemplateKey;
	label: string;
	defaultName: string;
	defaultDescription: string;
	defaultIcon: string;
	helperText: string;
}

export const TICKET_TYPE_TEMPLATES: TicketTypeTemplateOption[] = [
	{
		key: "simple_ticket",
		label: "简单事项",
		defaultName: "简单事项",
		defaultDescription: "适用于快速记录的轻量事项，默认仅描述必填",
		defaultIcon: "mdi:file-document-outline",
		helperText: "仅需要简单说明的事项，默认关联系统属性「描述」，且描述为必填。",
	},
	{
		key: "standard_ticket",
		label: "标准事项",
		defaultName: "标准事项",
		defaultDescription: "适用于标准工单流程的事项，默认标题与描述均必填",
		defaultIcon: "mdi:ticket-outline",
		helperText: "需要详细说明的事项，默认关联系统属性「标题」「描述」，且均为必填。",
	},
];

export function findTicketTypeTemplate(key: TicketTypeTemplateKey) {
	return TICKET_TYPE_TEMPLATES.find(item => item.key === key);
}
