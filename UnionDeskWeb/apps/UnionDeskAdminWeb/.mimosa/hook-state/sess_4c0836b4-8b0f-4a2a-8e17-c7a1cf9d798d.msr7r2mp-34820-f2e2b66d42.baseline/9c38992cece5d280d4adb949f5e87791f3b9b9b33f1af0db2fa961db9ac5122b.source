import { describe, expect, it } from "vitest";

import { parseApiResponse } from "./utils";

describe("parseApiResponse", () => {
	it("parses wrapped success envelope", () => {
		expect(parseApiResponse<{ id: number }>({
			success: true,
			code: "0",
			message: "ok",
			data: { id: 1 },
		})).toEqual({ id: 1 });
	});

	it("parses role entity without treating code as api status", () => {
		expect(parseApiResponse({
			id: 9,
			code: "platform_admin",
			name: "平台管理员",
			scope: "global",
			system: false,
		})).toEqual({
			id: 9,
			code: "platform_admin",
			name: "平台管理员",
			scope: "global",
			system: false,
		});
	});

	it("throws on wrapped failure envelope", () => {
		expect(() => parseApiResponse({
			success: false,
			code: "400",
			message: "domain role cannot own platform permission",
		})).toThrow("domain role cannot own platform permission");
	});
});
