import type { CustomerPortalTicketStatus } from "@uniondesk/shared";

const STATUS_LABEL: Record<CustomerPortalTicketStatus, string> = {
	open: "待处理",
	processing: "处理中",
	waiting_customer: "待补充",
	resolved: "已解决",
	closed: "已关闭",
	withdrawn: "已撤回",
};

const STATUS_CLASS: Record<CustomerPortalTicketStatus, string> = {
	open: "ud-tag--blue",
	processing: "ud-tag--orange",
	waiting_customer: "ud-tag--orange",
	resolved: "ud-tag--green",
	closed: "",
	withdrawn: "ud-tag--red",
};

export function StatusTag({ status }: { status: CustomerPortalTicketStatus }) {
	return (
		<span className={`ud-tag ${STATUS_CLASS[status]}`.trim()}>
			{STATUS_LABEL[status]}
		</span>
	);
}
