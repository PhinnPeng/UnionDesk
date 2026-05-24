import { describe, expect, it } from "vitest";

import { normalizeAdminDomain, normalizeAdminDomainsPageResult } from "./normalize-admin-domain";

describe("normalizeAdminDomain", () => {
	it("maps snake_case fields and numeric status", () => {
		const row = normalizeAdminDomain({
			id: 1,
			code: "default",
			name: "默认域",
			visibility_policy_codes: ["public"],
			registration_policy: "open",
			status: 1,
			creator_name: "管理员",
		});
		expect(row).toMatchObject({
			id: "1",
			code: "default",
			name: "默认域",
			status: "1",
			creator_name: "管理员",
		});
	});

	it("defaults missing visibility codes", () => {
		const row = normalizeAdminDomain({
			id: "2",
			code: "ops",
			name: "运维域",
			registration_policy: "invitation_only",
		});
		expect(row?.visibility_policy_codes).toEqual(["public"]);
	});
});

describe("normalizeAdminDomainsPageResult", () => {
	it("unwraps list from page result", () => {
		const page = normalizeAdminDomainsPageResult({
			total: 1,
			list: [{ id: 3, code: "a", name: "A", registration_policy: "open" }],
		});
		expect(page.total).toBe(1);
		expect(page.list[0]?.code).toBe("a");
	});
});
