import { describe, expect, it } from "vitest";

import type { P0VisibilityPolicyCode } from "@uniondesk/shared";

import {
	applyVisibilityPolicyChange,
	domainLogoText,
	generateDomainCode,
	hasCustomDomainLogo,
	isDomainEnabled,
	isDomainPublic,
	isValidLogoUrl,
	resolveDomainLogoUrl,
	resolveNumericDomainId,
	slugifyDomainCode,
} from "./domain-utils";

describe("domain utils", () => {
	it("detects enabled status variants", () => {
		expect(isDomainEnabled({ status: "1" })).toBe(true);
		expect(isDomainEnabled({ status: "active" })).toBe(true);
		expect(isDomainEnabled({ status: "0" })).toBe(false);
	});

	it("resolves numeric domain id", () => {
		expect(resolveNumericDomainId("42")).toBe(42);
		expect(resolveNumericDomainId("uuid")).toBeNull();
	});

	it("validates logo url", () => {
		expect(isValidLogoUrl(undefined)).toBe(true);
		expect(isValidLogoUrl(undefined, { required: true })).toBe(false);
		expect(isValidLogoUrl("https://cdn.example.com/a.png")).toBe(true);
		expect(isValidLogoUrl("/default-domain-logo.svg")).toBe(true);
		expect(isValidLogoUrl("ftp://bad")).toBe(false);
	});

	it("resolves default logo", () => {
		expect(resolveDomainLogoUrl(null)).toContain("default-domain-logo");
		expect(resolveDomainLogoUrl("https://cdn.example.com/a.png")).toBe("https://cdn.example.com/a.png");
	});

	it("generates domain code from seed", () => {
		expect(generateDomainCode("Acme Support")).toMatch(/^acme-support-[a-z0-9]+$/);
		expect(slugifyDomainCode("Hello World!")).toBe("hello-world");
	});

	it("derives logo text and public visibility", () => {
		expect(domainLogoText("Acme")).toBe("A");
		expect(domainLogoText("  ")).toBe("?");
		expect(isDomainPublic(["public"])).toBe(true);
		expect(isDomainPublic(["domain_customer_only"])).toBe(false);
		expect(hasCustomDomainLogo("/default-domain-logo.svg")).toBe(false);
		expect(hasCustomDomainLogo("https://cdn.example.com/logo.png")).toBe(true);
	});

	it("applies visibility policy mutual exclusion", () => {
		const pub: P0VisibilityPolicyCode[] = ["public"];
		const both: P0VisibilityPolicyCode[] = ["domain_customer_only", "channel_only"];

		expect(applyVisibilityPolicyChange(pub, ["public", "domain_customer_only"])).toEqual(["domain_customer_only"]);
		expect(applyVisibilityPolicyChange(pub, ["public"])).toEqual(["public"]);
		expect(applyVisibilityPolicyChange(both, ["public", ...both])).toEqual(["public"]);
		expect(applyVisibilityPolicyChange(both, both)).toEqual(both);
		expect(applyVisibilityPolicyChange(both, [])).toEqual(["public"]);
	});
});
