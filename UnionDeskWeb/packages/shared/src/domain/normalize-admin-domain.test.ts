import { describe, expect, it } from "vitest";

import { normalizeAdminDomain, normalizeAdminDomainsPageResult } from "./normalize-admin-domain";

describe("normalizeAdminDomain", () => {
	it("maps snake_case fields and numeric status", () => {
		const row = normalizeAdminDomain({
			id: 1,
			code: "default",
			name: "默认域",
			visibility_policy_codes: ["public"],
			registration_enabled: "allowed",
			invitation_enabled: "allowed",
			status: 1,
			creator_name: "管理员",
		});
		expect(row).toMatchObject({
			id: "1",
			code: "default",
			name: "默认域",
			registration_enabled: "allowed",
			invitation_enabled: "allowed",
			status: "1",
			creator_name: "管理员",
		});
	});

	it("defaults missing visibility codes and access policies", () => {
		const row = normalizeAdminDomain({
			id: "2",
			code: "ops",
			name: "运维域",
		});
		expect(row?.visibility_policy_codes).toEqual(["public"]);
		expect(row?.registration_enabled).toBe("allowed");
		expect(row?.invitation_enabled).toBe("allowed");
	});

	it("normalizes disallowed access policies", () => {
		const row = normalizeAdminDomain({
			id: "3",
			code: "invite",
			name: "邀请域",
			registration_enabled: "disallowed",
			invitation_enabled: "allowed",
		});
		expect(row?.registration_enabled).toBe("disallowed");
		expect(row?.invitation_enabled).toBe("allowed");
	});
});

describe("normalizeAdminDomainsPageResult", () => {
	it("unwraps list from page result", () => {
		const page = normalizeAdminDomainsPageResult({
			total: 1,
			list: [{
				id: 3,
				code: "a",
				name: "A",
				registration_enabled: "allowed",
				invitation_enabled: "disallowed",
			}],
		});
		expect(page.total).toBe(1);
		expect(page.list[0]?.code).toBe("a");
		expect(page.list[0]?.invitation_enabled).toBe("disallowed");
	});
});
