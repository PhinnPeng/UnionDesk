import { describe, expect, it } from "vitest";

import { componentRegistry, registeredComponentKeys } from "./component-registry";

describe("component registry", () => {
	it("uses normalized component values without a leading ./ prefix", () => {
		expect(componentRegistry.every(item => !item.value.startsWith("./"))).toBe(true);
		expect(registeredComponentKeys.has("system/menu")).toBe(true);
		expect(registeredComponentKeys.has("./system/menu")).toBe(false);
	});
});
