import type { PlatformOrganizationView } from "@uniondesk/shared";
import type { Dayjs } from "dayjs";

import dayjs from "dayjs";

export type OrganizationSearchValues = {
	keyword?: string;
	createdRange?: [Dayjs, Dayjs] | null;
};

export type OrganizationTreeNode = PlatformOrganizationView & {
	children: OrganizationTreeNode[];
};

export function generateDepartmentCode(): string {
	const randomPart = Math.random().toString(36).slice(2, 10).padEnd(8, "0");
	return `dept-${randomPart}`;
}

export function buildOrganizationTree(rows: PlatformOrganizationView[]): OrganizationTreeNode[] {
	const nodeMap = new Map<number, OrganizationTreeNode>();
	const roots: OrganizationTreeNode[] = [];

	for (const row of rows) {
		nodeMap.set(row.id, {
			...row,
			children: [],
		});
	}

	for (const row of rows) {
		const node = nodeMap.get(row.id);
		if (!node) {
			continue;
		}
		if (row.parentId == null) {
			roots.push(node);
			continue;
		}
		const parent = nodeMap.get(row.parentId);
		if (!parent) {
			roots.push(node);
			continue;
		}
		parent.children.push(node);
	}

	const sortNodes = (nodes: OrganizationTreeNode[]) => {
		nodes.sort((left, right) => left.orderNo - right.orderNo || left.id - right.id);
		for (const node of nodes) {
			if (node.children.length > 0) {
				sortNodes(node.children);
			}
		}
	};

	sortNodes(roots);
	return roots;
}

export function filterOrganizationTree(nodes: OrganizationTreeNode[], search: OrganizationSearchValues): OrganizationTreeNode[] {
	const keyword = search.keyword?.trim().toLowerCase() ?? "";
	const range = search.createdRange;
	const minTime = range?.[0]?.valueOf();
	const maxTime = range?.[1]?.valueOf();

	const matchesNode = (node: OrganizationTreeNode) => {
		const matchesName = !keyword || node.name.toLowerCase().includes(keyword) || node.code.toLowerCase().includes(keyword);
		const createdTime = dayjs(node.createdAt).valueOf();
		const matchesRange = minTime == null || maxTime == null || (createdTime >= minTime && createdTime <= maxTime);
		return matchesName && matchesRange;
	};

	const visit = (currentNodes: OrganizationTreeNode[]): OrganizationTreeNode[] => {
		const filtered: OrganizationTreeNode[] = [];
		for (const node of currentNodes) {
			const nextChildren = node.children.length > 0 ? visit(node.children) : [];
			if (matchesNode(node) || nextChildren.length > 0) {
				filtered.push({
					...node,
					children: nextChildren,
				});
			}
		}
		return filtered;
	};

	return visit(nodes);
}

export function collectTreeKeys(nodes: OrganizationTreeNode[]): string[] {
	const keys: string[] = [];
	const visit = (currentNodes: OrganizationTreeNode[]) => {
		for (const node of currentNodes) {
			keys.push(String(node.id));
			if (node.children.length > 0) {
				visit(node.children);
			}
		}
	};
	visit(nodes);
	return keys;
}

export function formatOrganizationDateTime(value?: string | null): string {
	if (!value) {
		return "-";
	}
	return dayjs(value).format("YYYY-MM-DD HH:mm");
}

export function buildLeaderOptionLabel(username: string, mobile?: string | null): string {
	return mobile ? `${username} / ${mobile}` : username;
}
