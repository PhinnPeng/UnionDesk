import type { AppScope } from "#src/router/extra-info/app-scope";
import type { TabStateType } from "#src/store/tabs";
import { useTabsStore } from "#src/store/tabs";

/** 打开非菜单/隐藏路由：先注册顶栏页签再 navigate */
export function openAppScopeTab(
	scope: AppScope,
	navigate: (path: string) => void,
	path: string,
	tabProps: TabStateType,
): void {
	useTabsStore.getState().addTab(scope, path, {
		closable: true,
		draggable: true,
		...tabProps,
		key: tabProps.key ?? path.split("?")[0],
	});
	navigate(path);
}
