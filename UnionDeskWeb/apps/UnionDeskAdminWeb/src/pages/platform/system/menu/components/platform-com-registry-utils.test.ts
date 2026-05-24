import { describe, expect, it } from "vitest";

import { platformComRegistry } from "./platform-com-registry";
import {
	buildPlatformComCascaderOptions,
	componentKeyToCascaderValue,
	joinComponentKey,
	splitComponentKey,
} from "./platform-com-registry-utils";

describe("platform-com-registry-utils", () => {
	it("splits and joins component keys", () => {
		expect(splitComponentKey("./platform/system/menu")).toEqual(["platform", "system", "menu"]);
		expect(joinComponentKey(["platform", "platform/system", "platform/system/menu"])).toBe("platform/system/menu");
		expect(joinComponentKey("platform/home")).toBe("platform/home");
		expect(componentKeyToCascaderValue("platform/system/menu")).toEqual([
			"platform",
			"platform/system",
			"platform/system/menu",
		]);
	});

	it("builds cascader tree with shared prefixes", () => {
		const options = buildPlatformComCascaderOptions([
			{ label: "平台管理/平台首页", value: "platform/home" },
			{ label: "平台管理/菜单管理", value: "platform/system/menu" },
		]);

		const platformNode = options.find(option => option.value === "platform");
		expect(platformNode?.label).toBe("平台管理");
		expect(platformNode?.children?.some(child => child.value === "platform/home")).toBe(true);
		expect(platformNode?.children?.some(child => child.value === "platform/system")).toBe(true);

		const systemNode = platformNode?.children?.find(child => child.value === "platform/system");
		expect(systemNode?.children?.some(child => child.value === "platform/system/menu" && child.label === "菜单管理")).toBe(true);
	});

	it("builds domain list leaf under platform branch", () => {
		const options = buildPlatformComCascaderOptions([
			{ label: "平台管理/业务域管理/业务域列表", value: "platform/domains" },
		]);

		const platformNode = options.find(option => option.value === "platform");
		expect(platformNode?.children?.some(
			child => child.value === "platform/domains" && child.label === "业务域列表",
		)).toBe(true);
	});

	it("covers all registry entries as leaf paths", () => {
		const options = buildPlatformComCascaderOptions(platformComRegistry);
		const leafKeys = new Set<string>();

		const visit = (nodes: ReturnType<typeof buildPlatformComCascaderOptions>) => {
			for (const node of nodes) {
				if (node.children?.length) {
					visit(node.children);
				}
				else {
					leafKeys.add(node.value);
				}
			}
		};

		visit(options);
		for (const item of platformComRegistry) {
			expect(leafKeys.has(item.value)).toBe(true);
		}
	});
});
