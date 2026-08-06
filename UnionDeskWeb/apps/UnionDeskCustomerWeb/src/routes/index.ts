export type AppRouteItem = {
	key: string;
	path: string;
	name: string;
};

export const appRoutes: AppRouteItem[] = [
	{ key: "home", path: "/home", name: "首页" },
	{ key: "tickets", path: "/tickets", name: "工单" },
	{ key: "chat", path: "/chat", name: "咨询" },
	{ key: "inbox", path: "/inbox", name: "通知" },
	{ key: "me", path: "/me", name: "我的" },
];
