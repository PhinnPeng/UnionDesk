import type { PlatformOrganizationView } from "@uniondesk/shared";

import dayjs from "dayjs";
import { describe, expect, it } from "vitest";

import { buildOrganizationTree, collectTreeKeys, filterOrganizationTree, generateDepartmentCode } from "./utils";

const rows: PlatformOrganizationView[] = [
	{
		id: 1,
		code: "root",
		name: "平台组织",
		parentId: null,
		parentName: null,
		leaderUserId: 1,
		leaderName: "admin",
		orderNo: 1,
		status: 1,
		remark: null,
		createdAt: "2026-05-03T08:00:00",
	},
	{
		id: 2,
		code: "ops",
		name: "运营部",
		parentId: 1,
		parentName: "平台组织",
		leaderUserId: 2,
		leaderName: "ops",
		orderNo: 2,
		status: 1,
		remark: null,
		createdAt: "2026-05-04T08:00:00",
	},
	{
		id: 3,
		code: "audit",
		name: "审计部",
		parentId: 1,
		parentName: "平台组织",
		leaderUserId: 3,
		leaderName: "audit",
		orderNo: 3,
		status: 1,
		remark: null,
		createdAt: "2026-05-05T08:00:00",
	},
];

describe("organization utils", () => {
	it("generates department code with expected prefix and length", () => {
		expect(generateDepartmentCode()).toMatch(/^dept-[a-z0-9]{8}$/);
	});

	it("builds a tree and keeps root keys", () => {
		const tree = buildOrganizationTree(rows);
		expect(tree).toHaveLength(1);
		expect(tree[0].children).toHaveLength(2);
		expect(collectTreeKeys(tree)).toEqual(["1", "2", "3"]);
	});

	it("keeps ancestor nodes when filtering by child keyword", () => {
		const tree = buildOrganizationTree(rows);
		const filtered = filterOrganizationTree(tree, {
			keyword: "运营",
			createdRange: null,
		});
		expect(filtered).toHaveLength(1);
		expect(filtered[0].id).toBe(1);
		expect(filtered[0].children).toHaveLength(1);
		expect(filtered[0].children[0].id).toBe(2);
	});

	it("filters by code keyword", () => {
		const tree = buildOrganizationTree(rows);
		const filtered = filterOrganizationTree(tree, {
			keyword: "ops",
			createdRange: null,
		});
		expect(filtered).toHaveLength(1);
		expect(filtered[0].children).toHaveLength(1);
		expect(filtered[0].children[0].code).toBe("ops");
	});

	it("filters by created range", () => {
		const tree = buildOrganizationTree(rows);
		const filtered = filterOrganizationTree(tree, {
			keyword: "",
			createdRange: [dayjs("2026-05-04 00:00:00"), dayjs("2026-05-04 23:59:59")],
		});
		expect(filtered).toHaveLength(1);
		expect(filtered[0].children).toHaveLength(1);
		expect(filtered[0].children[0].id).toBe(2);
	});
});
