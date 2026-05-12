import { requestBackendJson } from "#src/api/backend";

import type { CreateMenuPayload, UpdateMenuPayload } from "@uniondesk/shared";

import type { MenuItemType } from "./types";

export * from "./types";

export function fetchMenuTree(params: { scope: "platform" | "business" }): Promise<MenuItemType[]>;
export function fetchMenuTree(params?: { scope?: "platform" | "business" }): Promise<MenuItemType[] | Record<string, MenuItemType[]>>;
export function fetchMenuTree(params?: { scope?: "platform" | "business" }): Promise<MenuItemType[] | Record<string, MenuItemType[]>> {
	const query = new URLSearchParams();
	if (params?.scope) {
		query.set("scope", params.scope);
	}
	const path = query.size > 0 ? `v1/iam/menus/tree?${query.toString()}` : "v1/iam/menus/tree";
	return requestBackendJson<MenuItemType[] | Record<string, MenuItemType[]>>(path);
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
