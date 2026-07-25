import { describe, expect, it } from "vitest";

import { buildLoginRedirectSearch, getSafeRedirect } from "./safe-redirect";

describe("buildLoginRedirectSearch", () => {
	it("encodes path and query", () => {
		expect(buildLoginRedirectSearch("/platform/ticket-config", "?section=types"))
			.toBe(`?redirect=${encodeURIComponent("/platform/ticket-config?section=types")}`);
	});

	it("returns empty for login or root", () => {
		expect(buildLoginRedirectSearch("/login")).toBe("");
		expect(buildLoginRedirectSearch("/")).toBe("");
	});
});

describe("getSafeRedirect", () => {
	it("accepts encoded in-app paths with query", () => {
		const target = "/platform/ticket-config?section=types";
		expect(getSafeRedirect(encodeURIComponent(target))).toBe(target);
		expect(getSafeRedirect(target)).toBe(target);
	});

	it("rejects open redirects and login loop", () => {
		expect(getSafeRedirect("https://evil.com")).toBeNull();
		expect(getSafeRedirect("//evil.com")).toBeNull();
		expect(getSafeRedirect("/login")).toBeNull();
		expect(getSafeRedirect("/login?x=1")).toBeNull();
		expect(getSafeRedirect("")).toBeNull();
		expect(getSafeRedirect(null)).toBeNull();
	});
});
