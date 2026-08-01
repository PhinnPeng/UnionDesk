import { create } from "zustand";

interface PreferencesDrawerState {
	open: boolean
	openDrawer: () => void
	closeDrawer: () => void
	setOpen: (open: boolean) => void
}

/** 偏好设置抽屉开关（非持久化，供顶栏/侧栏用户菜单共用） */
export const usePreferencesDrawerStore = create<PreferencesDrawerState>(set => ({
	open: false,
	openDrawer: () => set({ open: true }),
	closeDrawer: () => set({ open: false }),
	setOpen: open => set({ open }),
}));
