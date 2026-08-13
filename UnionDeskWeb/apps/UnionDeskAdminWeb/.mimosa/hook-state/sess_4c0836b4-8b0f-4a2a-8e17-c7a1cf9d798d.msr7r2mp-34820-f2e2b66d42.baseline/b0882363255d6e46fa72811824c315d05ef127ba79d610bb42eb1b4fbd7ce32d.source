import { beforeEach, describe, expect, it, vi } from "vitest";

import { resolveDefaultHomePath, resolveHomePathFromActions, resolveHomePathFromMenus } from "./resolve-home-path";

describe("resolveDefaultHomePath", () => {
	beforeEach(() => {
		vi.stubEnv("VITE_BASE_HOME_PATH", "/home");
	});

	it("仅平台能力进入平台首页", () => {
		expect(resolveDefaultHomePath(true, false)).toBe("/platform/home");
	});

	it("仅域名单进入业务域首页", () => {
		expect(resolveDefaultHomePath(false, true)).toBe("/home");
	});

	it("双端进入业务域首页", () => {
		expect(resolveDefaultHomePath(true, true)).toBe("/home");
	});

	it("双无回退业务域首页", () => {
		expect(resolveDefaultHomePath(false, false)).toBe("/home");
	});
});

describe("resolveHomePathFromMenus", () => {
	beforeEach(() => {
		vi.stubEnv("VITE_BASE_HOME_PATH", "/home");
	});

	it("按平台能力与域名单矩阵解析", () => {
		expect(resolveHomePathFromMenus([], true, undefined, [], false)).toBe("/platform/home");
		expect(resolveHomePathFromMenus([], true, undefined, [], true)).toBe("/home");
		expect(resolveHomePathFromMenus([], false, undefined, [], true)).toBe("/home");
	});
});

describe("resolveHomePathFromActions (legacy)", () => {
	beforeEach(() => {
		vi.stubEnv("VITE_BASE_HOME_PATH", "/home");
	});

	it("仅 platform.* 权限时进入平台首页", () => {
		expect(resolveHomePathFromActions(["platform.menu.read", "platform.home.read"])).toBe("/platform/home");
	});

	it("仅 domain.* 权限时进入业务域首页", () => {
		expect(resolveHomePathFromActions(["domain.menu.read", "domain.role.read"])).toBe("/home");
	});
});
