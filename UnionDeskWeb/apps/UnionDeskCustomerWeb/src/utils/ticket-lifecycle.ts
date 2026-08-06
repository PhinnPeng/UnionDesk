import type { CustomerPortalTicketStatus } from "@uniondesk/shared";

export type LifecycleBucket = "pending" | "active" | "done";

export const LIFECYCLE_FILTERS: Array<{ key: "all" | LifecycleBucket; label: string }> = [
	{ key: "all", label: "全部" },
	{ key: "pending", label: "待处理" },
	{ key: "active", label: "进行中" },
	{ key: "done", label: "已完成" },
];

/** Map configurable statuses into start / in-progress / final buckets. */
export function toLifecycleBucket(status: CustomerPortalTicketStatus): LifecycleBucket {
	switch (status) {
		case "open":
			return "pending";
		case "processing":
		case "waiting_customer":
			return "active";
		case "resolved":
		case "closed":
		case "withdrawn":
			return "done";
		default:
			return "active";
	}
}

export function matchesLifecycle(
	status: CustomerPortalTicketStatus,
	bucket: "all" | LifecycleBucket,
): boolean {
	if (bucket === "all") {
		return true;
	}
	return toLifecycleBucket(status) === bucket;
}

export function countByLifecycle(
	statuses: CustomerPortalTicketStatus[],
): Record<LifecycleBucket, number> {
	const counts: Record<LifecycleBucket, number> = { pending: 0, active: 0, done: 0 };
	for (const status of statuses) {
		counts[toLifecycleBucket(status)] += 1;
	}
	return counts;
}
