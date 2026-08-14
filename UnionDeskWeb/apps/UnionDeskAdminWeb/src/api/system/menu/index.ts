import { requestBackendJson } from "#src/utils/request";

import type { CreateMenuPayload, UpdateMenuPayload } from "@uniondesk/shared";

import type { MenuItemType } from "./types";

export * from "./types";

/** 后端 `MenuTreeResultView`：始终按 platform/business 分组返回 */
type MenuTreeResultView = {
	platform?: MenuItemType[]
	business?: MenuItemType[]
};

function normalizeMenuTree(
	raw: MenuItemType[] | MenuTreeResultView | null | undefined,
	scope?: "platform" | "business",
): MenuItemType[] {
	if (Array.isArray(raw)) {
		return raw;
	}
	if (!raw || typeof raw !== "object") {
		return [];
	}
	const platform = Array.isArray(raw.platform) ? raw.platform : [];
	const business = Array.isArray(raw.business) ? raw.business : [];
	if (scope === "platform") {
		return platform;
	}
	if (scope === "business") {
		return business;
	}
	return [...platform, ...business];
}

export async function fetchMenuTree(params: { scope: "platform" | "business" }): Promise<MenuItemType[]>;
export async function fetchMenuTree(params?: { scope?: "platform" | "business" }): Promise<MenuItemType[]>;
export async function fetchMenuTree(params?: { scope?: "platform" | "business" }): Promise<MenuItemType[]> {
	const query = new URLSearchParams();
	if (params?.scope) {
		query.set("scope", params.scope);
	}
	const path = query.size > 0 ? `v1/iam/menus/tree?${query.toString()}` : "v1/iam/menus/tree";
	const raw = await requestBackendJson<MenuItemType[] | MenuTreeResultView>(path);
	return normalizeMenuTree(raw, params?.scope);
}

export function fetchCreateMenu(data: CreateMenuPayload): Promise<unknown> {
	return requestBackendJson("v1/iam/menus", {
		method: "POST",
		json: data,
	});
}

export function fetchUpdateMenu(id: number, data: UpdateMenuPayload): Promise<unknown> {
	return requestBackendJson(`v1/iam/menus/${id}`, {
		method: "PUT",
		json: data,
	});
}

export function fetchDeleteMenu(id: number): Promise<unknown> {
	return requestBackendJson(`v1/iam/menus/${id}`, {
		method: "DELETE",
	});
}

export function fetchMenuList(params?: { scope?: "platform" | "business" }) {
	return fetchMenuTree(params);
}

export function fetchAddMenuItem(data: CreateMenuPayload) {
	return fetchCreateMenu(data);
}

export function fetchUpdateMenuItem(idOrData: number | (UpdateMenuPayload & { id?: number }), data?: UpdateMenuPayload) {
	if (typeof idOrData === "number") {
		return fetchUpdateMenu(idOrData, data ?? {});
	}
	if (typeof idOrData.id !== "number") {
		throw new Error("menu id is required");
	}
	const { id, ...payload } = idOrData;
	return fetchUpdateMenu(id, payload);
}

export function fetchDeleteMenuItem(id: number) {
	return fetchDeleteMenu(id);
}
