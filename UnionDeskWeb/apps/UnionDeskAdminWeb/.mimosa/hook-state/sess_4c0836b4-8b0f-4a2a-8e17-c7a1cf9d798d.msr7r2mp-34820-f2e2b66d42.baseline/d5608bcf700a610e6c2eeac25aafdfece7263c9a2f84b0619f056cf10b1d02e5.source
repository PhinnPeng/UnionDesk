import type { AppScope } from "#src/router/extra-info/app-scope";
import type { TabStateType } from "#src/store/tabs";
import { useTabsStore } from "#src/store/tabs";

function splitPathAndSearch(path: string): { pathname: string; search: string } {
	const queryIndex = path.indexOf("?");
	if (queryIndex === -1) {
		return { pathname: path, search: "" };
	}
	return {
		pathname: path.slice(0, queryIndex),
		search: path.slice(queryIndex),
	};
}

/** 打开非菜单/隐藏路由：先注册顶栏页签再 navigate */
export function openAppScopeTab(
	scope: AppScope,
	navigate: (path: string, options?: { state?: unknown }) => void,
	path: string,
	tabProps: TabStateType,
	navigateState?: unknown,
): void {
	const { pathname, search } = splitPathAndSearch(path);
	useTabsStore.getState().addTab(scope, pathname, {
		closable: true,
		draggable: true,
		...tabProps,
		key: tabProps.key ?? pathname,
		historyState: {
			search,
			hash: "",
			...(tabProps.historyState ?? {}),
		},
	});
	navigate(path, navigateState ? { state: navigateState } : undefined);
}
