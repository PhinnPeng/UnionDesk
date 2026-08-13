import type { MenuProps } from "antd";
import { useAppScope } from "#src/hooks/use-app-scope";
import { getAppHomePath } from "#src/router/extra-info";
import { useTabsStore } from "#src/store/tabs";
import {
	CloseOutlined,
	RedoOutlined,
	SwapOutlined,
	VerticalAlignBottomOutlined,
	VerticalAlignMiddleOutlined,
	VerticalAlignTopOutlined,
} from "@ant-design/icons";
import { useKeepAliveContext } from "keepalive-for-react";
import { useCallback, useMemo } from "react";
import { useShallow } from "zustand/shallow";

import { useTranslation } from "react-i18next";

/**
 * 标签页操作的键值对象
 * @readonly
 * @enum {string}
 * @property {string} REFRESH - 重新加载当前标签页
 * @property {string} CLOSE - 关闭当前标签页
 * @property {string} CLOSE_RIGHT - 关闭右侧标签页
 * @property {string} CLOSE_LEFT - 关闭左侧标签页
 * @property {string} CLOSE_OTHERS - 关闭其他标签页
 * @property {string} CLOSE_ALL - 关闭所有标签页
 */
export const TabActionKeys = {
	REFRESH: "refresh",
	CLOSE: "close",
	CLOSE_RIGHT: "closeRight",
	CLOSE_LEFT: "closeLeft",
	CLOSE_OTHERS: "closeOthers",
	CLOSE_ALL: "closeAll",
} as const;

export type TabActionKey = typeof TabActionKeys[keyof typeof TabActionKeys];

/**
 * 自定义钩子，用于处理标签页的下拉菜单
 * @returns {[Function, Function]} 返回一个元组，包含菜单项生成函数和菜单点击处理函数
 */
export function useDropdownMenu() {
	const { t } = useTranslation();
	const appScope = useAppScope();
	const homePath = getAppHomePath(appScope);
	const {
		openTabs,
		activeKey,
		removeTab,
		closeLeftTabs,
		closeRightTabs,
		closeOtherTabs,
		closeAllTabs,
		setIsRefresh,
	} = useTabsStore(useShallow(state => ({
		openTabs: state[appScope].openTabs,
		activeKey: state[appScope].activeKey,
		removeTab: state.removeTab,
		closeLeftTabs: state.closeLeftTabs,
		closeRightTabs: state.closeRightTabs,
		closeOtherTabs: state.closeOtherTabs,
		closeAllTabs: state.closeAllTabs,
		setIsRefresh: state.setIsRefresh,
	})));
	const { refresh } = useKeepAliveContext();
	/**
	 * 生成菜单项
	 * @param {string} tabKey - 当前标签页的键
	 * @returns {MenuProps["items"]} 菜单项配置
	 */
	const items = useCallback((tabKey: string): MenuProps["items"] => {
		const isOnlyTab = openTabs.size === 2 && openTabs.has(homePath);
		const isLastTab = Array.from(openTabs.keys()).pop() === tabKey;
		return [
			{
				key: TabActionKeys.REFRESH,
				icon: <RedoOutlined rotate={270} />,
				label: t("preferences.tabbar.contextMenu.refresh"),
				disabled: activeKey !== tabKey,
			},
			{
				key: TabActionKeys.CLOSE,
				icon: <CloseOutlined />,
				label: t("preferences.tabbar.contextMenu.close"),
				disabled: tabKey === homePath,
			},
			{ type: "divider" },
			{
				key: TabActionKeys.CLOSE_LEFT,
				icon: <VerticalAlignBottomOutlined rotate={90} />,
				label: t("preferences.tabbar.contextMenu.closeLeft"),
				disabled: tabKey === homePath || isOnlyTab,
			},
			{
				key: TabActionKeys.CLOSE_RIGHT,
				icon: <VerticalAlignTopOutlined rotate={90} />,
				label: t("preferences.tabbar.contextMenu.closeRight"),
				disabled: tabKey === homePath || isOnlyTab || isLastTab,
			},
			{ type: "divider" },
			{
				key: TabActionKeys.CLOSE_OTHERS,
				icon: <VerticalAlignMiddleOutlined rotate={90} />,
				label: t("preferences.tabbar.contextMenu.closeOthers"),
				disabled: tabKey === homePath || isOnlyTab,
			},
			{
				key: TabActionKeys.CLOSE_ALL,
				icon: <SwapOutlined />,
				label: t("preferences.tabbar.contextMenu.closeAll"),
				disabled: tabKey === homePath,
			},
		];
	}, [t, activeKey, homePath, openTabs]);

	/**
	 * 定义菜单操作与对应的处理函数
	 */
	const actions = useMemo(() => ({
		[TabActionKeys.REFRESH]: (currentPath: string) => {
			// 刷新 KeepAlive 缓存的页面
			refresh(currentPath);
			// 重新渲染页面
			setIsRefresh(appScope, true);
		},
		[TabActionKeys.CLOSE]: (currentPath: string) => removeTab(appScope, currentPath),
		[TabActionKeys.CLOSE_RIGHT]: (currentPath: string) => closeRightTabs(appScope, currentPath),
		[TabActionKeys.CLOSE_LEFT]: (currentPath: string) => closeLeftTabs(appScope, currentPath),
		[TabActionKeys.CLOSE_OTHERS]: (currentPath: string) => closeOtherTabs(appScope, currentPath),
		[TabActionKeys.CLOSE_ALL]: (_currentPath: string) => closeAllTabs(appScope),
	}), [removeTab, closeRightTabs, closeLeftTabs, closeOtherTabs, closeAllTabs, setIsRefresh, refresh, appScope]);

	/**
	 * 处理菜单点击事件
	 * @param {string} menuKey - 被点击的菜单项键
	 * @param {string} nodeKey - 当前标签页的键
	 */
	const onClickMenu = useCallback((menuKey: string, nodeKey: string) => {
		const action = actions[menuKey as keyof typeof actions];
		if (action) {
			action(nodeKey);
		}
	}, [actions]);

	return [items, onClickMenu] as const;
}
