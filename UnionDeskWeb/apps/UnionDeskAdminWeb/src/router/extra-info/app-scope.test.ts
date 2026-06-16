import { beforeEach, describe, expect, it, vi } from "vitest";

import { resolveHomePathFromActions, resolveHomePathFromMenus } from "./resolve-home-path";

describe("resolveHomePathFromActions", () => {
	beforeEach(() => {
		vi.stubEnv("VITE_BASE_HOME_PATH", "/home");
	});

	it("仅 platform.* 权限时进入平台首页", () => {
		expect(resolveHomePathFromActions(["platform.menu.read", "platform.home.read"])).toBe("/platform/home");
	});

	it("仅 domain.* 权限时进入业务域首页", () => {
		expect(resolveHomePathFromActions(["domain.menu.read", "domain.role.read"])).toBe("/home");
	});

	it("platform.* 与 domain.* 同时存在时进入业务域首页", () => {
		expect(resolveHomePathFromActions(["platform.menu.read", "domain.menu.read"])).toBe("/home");
	});

	it("无 actions 时进入业务域默认首页", () => {
		expect(resolveHomePathFromActions([])).toBe("/home");
	});
});

describe("resolveHomePathFromMenus", () => {
	beforeEach(() => {
		vi.stubEnv("VITE_BASE_HOME_PATH", "/home");
	});

	it("传入 actions 时优先走三元规则", () => {
		expect(resolveHomePathFromMenus([], true, undefined, ["platform.menu.read"])).toBe("/platform/home");
		expect(resolveHomePathFromMenus([], true, undefined, ["platform.menu.read", "domain.menu.read"])).toBe("/home");
	});

	it("无 actions 时回退业务域默认首页", () => {
		expect(resolveHomePathFromMenus([], true)).toBe("/home");
	});
});
