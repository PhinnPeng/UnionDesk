import type { TabPaneProps } from "antd";

import { appScopes, getAppHomePath, type AppScope } from "#src/router/extra-info/app-scope-core";
import { usePreferencesStore } from "#src/store/preferences";
import { getAppNamespace } from "#src/utils/get-app-namespace";

import { create } from "zustand";
import { persist } from "zustand/middleware";

/**
 * @zh 标签页项属性
 * @en Tab item properties interface.
 */
export interface TabItemProps extends Omit<TabPaneProps, "tab"> {
	key: string
	label: React.ReactNode
	/**
	 * @zh 是否可拖拽
	 * @en Whether it can be dragged.
	 */
	draggable?: boolean
	/**
	 * 可选的历史状态值，如 search 和 hash，可存储于此
	 * 在目标路由中可通过 useLocation 钩子访问该状态
	 * @see {@link https://reactrouter.com/en/main/hooks/use-navigate#optionsstate | usenavigate - options state}
	 */
	historyState?: {
		hash?: string
		search?: string
	}
}

export interface TabStateType extends Omit<TabItemProps, "label"> {
	label: string
	/**
	 * @zh 标签页的新标题，用于修改标签页的标题
	 * @en The new title of the tab, used to modify the title of the tab.
	 */
	newTabTitle?: React.ReactNode
}

interface TabsBucket {
	openTabs: Map<string, TabStateType>
	activeKey: string
	isRefresh: boolean
	isMaximize: boolean
}

type TabsState = Record<AppScope, TabsBucket>;
type TabsPersistBucket = Omit<TabsBucket, "activeKey" | "openTabs"> & {
	openTabs: Array<[string, TabStateType]>
};
type TabsPersistState = Record<AppScope, TabsPersistBucket>;

const createBucket = (): TabsBucket => ({
	openTabs: new Map<string, TabStateType>([]),
	activeKey: "",
	isRefresh: false,
	isMaximize: false,
});

const createInitialState = (): TabsState => ({
	[appScopes.business]: createBucket(),
	[appScopes.platform]: createBucket(),
});

const initialState = createInitialState();

const getScopeHomePath = (scope: AppScope) => getAppHomePath(scope);

const serializeBucket = (bucket: TabsBucket): TabsPersistBucket => ({
	openTabs: Array.from(bucket.openTabs.entries()),
	isRefresh: bucket.isRefresh,
	isMaximize: bucket.isMaximize,
});

const deserializeBucket = (bucket: TabsPersistBucket): TabsBucket => ({
	openTabs: new Map(bucket.openTabs),
	activeKey: "",
	isRefresh: bucket.isRefresh,
	isMaximize: bucket.isMaximize,
});

/**
 * @zh 标签页的操作方法
 * @en Tab operation methods.
 */
interface TabsAction {
	setIsRefresh: (scope: AppScope, state: boolean) => void
	addTab: (scope: AppScope, routePath: string, tabProps: TabStateType) => void
	insertBeforeTab: (scope: AppScope, routePath: string, tabProps: TabStateType) => void
	removeTab: (scope: AppScope, routePath: string) => void
	closeRightTabs: (scope: AppScope, routePath: string) => void
	closeLeftTabs: (scope: AppScope, routePath: string) => void
	closeOtherTabs: (scope: AppScope, routePath: string) => void
	closeAllTabs: (scope: AppScope) => void
	setActiveKey: (scope: AppScope, routePath: string) => void
	resetTabs: () => void
	changeTabOrder: (scope: AppScope, from: number, to: number) => void
	toggleMaximize: (scope: AppScope, state: boolean) => void
	setTableTitle: (scope: AppScope, routePath: string, title: React.ReactNode) => void
	resetTableTitle: (scope: AppScope, routePath: string) => void
};

/**
 * @zh 标签页状态管理
 * @en Tab state management.
 */
export const useTabsStore = create<TabsState & TabsAction>()(
	persist<TabsState & TabsAction, [], [], TabsPersistState>(
		set => ({
			...initialState,

			/**
			 * @zh 设置标签页是否处于刷新状态
			 * @en Set whether the tab is in a refresh state.
			 */
			setIsRefresh: (scope: AppScope, state: boolean) => {
				set(currentState => ({
					[scope]: {
						...currentState[scope],
						isRefresh: state,
					},
				}));
			},

			/**
			 * @zh 设置标签页
			 * @en Set the tab.
			 */
			setActiveKey: (scope: AppScope, routePath: string) => {
				set(currentState => ({
					[scope]: {
						...currentState[scope],
						activeKey: routePath,
					},
				}));
			},

			/**
			 * @zh 在最前面插入标签页
			 * @en Insert a tab at the front.
			 */
			insertBeforeTab: (scope: AppScope, routePath: string, tabProps: TabStateType) => {
				set((currentState) => {
					if (!routePath.length) {
						return currentState;
					}
					const newMap = new Map([[routePath, tabProps]]);
					for (const [key, value] of currentState[scope].openTabs) {
						newMap.set(key, value);
					}
					return {
						[scope]: {
							...currentState[scope],
							openTabs: newMap,
						},
					};
				});
			},

			/**
			 * @zh 添加标签页
			 * @en Add a tab.
			 */
			addTab: (scope: AppScope, routePath: string, tabProps: TabStateType) => {
				set((currentState) => {
					if (!routePath.length) {
						return currentState;
					}
					const newTabs = new Map(currentState[scope].openTabs);
					/**
					 * 1. 如果 tab 已经存在，则更新 historyState 属性，所以不会去重，且 ...newTabs.get(routePath) 是为了保证首页的 closable 属性不被覆盖
					 * 2. 如果 tab 不存在，则添加到 Map 中
					 */
					newTabs.set(routePath, { ...newTabs.get(routePath), ...tabProps });
					return {
						[scope]: {
							...currentState[scope],
							openTabs: newTabs,
						},
					};
				});
			},

			/**
			 * @zh 移除标签页
			 * @en Remove a tab.
			 */
			removeTab: (scope: AppScope, routePath: string) => {
				set((currentState) => {
					const bucket = currentState[scope];
					const homePath = getScopeHomePath(scope);

					// 如果是首页，不允许关闭
					if (routePath === homePath) {
						return currentState;
					}

					const newTabs = new Map(bucket.openTabs);
					newTabs.delete(routePath);
					let newActiveKey = bucket.activeKey;

					// 移除当前激活的标签页，则选择最后一个标签页
					if (routePath === bucket.activeKey) {
						const tabsArray = Array.from(newTabs.keys());
						newActiveKey = tabsArray.at(-1) || homePath;
					}

					// 确保至少保留首页标签
					if (newTabs.size === 0) {
						const homeTab = bucket.openTabs.get(homePath);
						if (homeTab) {
							newTabs.set(homePath, homeTab);
						}
						newActiveKey = homePath;
					}

					return {
						[scope]: {
							...bucket,
							openTabs: newTabs,
							activeKey: newActiveKey,
						},
					};
				});
			},

			/**
			 * @zh 关闭右侧标签页
			 * @en Close tabs on the right.
			 */
			closeRightTabs: (scope: AppScope, routePath: string) => {
				set((currentState) => {
					const bucket = currentState[scope];
					const newTabs = new Map<string, TabStateType>();
					let found = false;
					let activeKeyFound = false;
					let newActiveKey = bucket.activeKey;

					for (const [key, value] of bucket.openTabs) {
						if (found) {
							break;
						}
						newTabs.set(key, value);
						if (key === routePath) {
							found = true;
						}
						if (key === bucket.activeKey) {
							activeKeyFound = true;
						}
					}

					if (!activeKeyFound) {
						newActiveKey = routePath;
					}

					return {
						[scope]: {
							...bucket,
							openTabs: newTabs,
							activeKey: newActiveKey,
						},
					};
				});
			},

			/**
			 * @zh 关闭左侧标签页
			 * @en Close tabs on the left.
			 */
			closeLeftTabs: (scope: AppScope, routePath: string) => {
				set((currentState) => {
					const bucket = currentState[scope];
					const newTabs = new Map<string, TabStateType>();
					const homePath = getScopeHomePath(scope);
					let found = false;
					let newActiveKey = bucket.activeKey;
					let activeKeyOnRight = false;

					// 首先添加首页标签，因为它不能被删除
					const homeTab = bucket.openTabs.get(homePath);
					if (homeTab) {
						newTabs.set(homePath, homeTab);
					}

					for (const [key, value] of bucket.openTabs) {
						if (key === homePath) {
							continue;
						}

						if (found || key === routePath) {
							newTabs.set(key, value);
							found = true;
						}

						if (key === bucket.activeKey && found) {
							activeKeyOnRight = true;
						}
					}

					if (!activeKeyOnRight) {
						newActiveKey = routePath;
					}

					return {
						[scope]: {
							...bucket,
							openTabs: newTabs,
							activeKey: newActiveKey,
						},
					};
				});
			},

			/**
			 * @zh 关闭其他标签页
			 * @en Close other tabs.
			 */
			closeOtherTabs: (scope: AppScope, routePath: string) => {
				set((currentState) => {
					const bucket = currentState[scope];
					const newTabs = new Map<string, TabStateType>();
					const homePath = getScopeHomePath(scope);

					const homeTab = bucket.openTabs.get(homePath);
					if (homeTab) {
						newTabs.set(homePath, homeTab);
					}

					if (routePath !== homePath && bucket.openTabs.has(routePath)) {
						newTabs.set(routePath, bucket.openTabs.get(routePath)!);
					}

					let newActiveKey = bucket.activeKey;
					if (!newTabs.has(bucket.activeKey)) {
						newActiveKey = routePath;
					}

					return {
						[scope]: {
							...bucket,
							openTabs: newTabs,
							activeKey: newActiveKey,
						},
					};
				});
			},

			/**
			 * @zh 关闭所有标签页
			 * @en Close all tabs.
			 */
			closeAllTabs: (scope: AppScope) => {
				set((currentState) => {
					const bucket = currentState[scope];
					const homePath = getScopeHomePath(scope);
					const newTabs = new Map<string, TabStateType>();
					const homeTab = bucket.openTabs.get(homePath);
					if (homeTab) {
						newTabs.set(homePath, homeTab);
					}
					return {
						[scope]: {
							...bucket,
							openTabs: newTabs,
							activeKey: homePath,
						},
					};
				});
			},

			/**
			 * @zh 改变标签页顺序
			 * @en Change tab order.
			 */
			changeTabOrder: (scope: AppScope, from: number, to: number) => {
				set((currentState) => {
					const bucket = currentState[scope];
					const newTabs = Array.from(bucket.openTabs.entries());
					const [movedTab] = newTabs.splice(from, 1);
					newTabs.splice(to, 0, movedTab);

					return {
						[scope]: {
							...bucket,
							openTabs: new Map(newTabs),
						},
					};
				});
			},

			/**
			 * @zh 切换标签页最大化状态
			 * @en Toggle tab maximization status.
			 */
			toggleMaximize: (scope: AppScope, state: boolean) => {
				set((currentState) => ({
					[scope]: {
						...currentState[scope],
						isMaximize: state,
					},
				}));
			},

			/**
			 * @zh 设置标签页标题
			 * @en Set the tab title.
			 */
			setTableTitle: (scope: AppScope, routePath: string, title: React.ReactNode) => {
				set((currentState) => {
					const bucket = currentState[scope];
					const newTabs = new Map(bucket.openTabs);
					const targetTab = newTabs.get(routePath);
					if (targetTab) {
						newTabs.set(routePath, { ...targetTab, newTabTitle: title });
						return {
							[scope]: {
								...bucket,
								openTabs: newTabs,
							},
						};
					}
					return currentState;
				});
			},

			/**
			 * @zh 重置标签页标题（删除自定义标题）
			 * @en Reset the tab title (delete custom titles).
			 */
			resetTableTitle: (scope: AppScope, routePath: string) => {
				set((currentState) => {
					const bucket = currentState[scope];
					const newTabs = new Map(bucket.openTabs);
					const targetTab = newTabs.get(routePath);
					if (targetTab) {
						const nextTab = { ...targetTab };
						delete nextTab.newTabTitle;
						newTabs.set(routePath, nextTab);
						return {
							[scope]: {
								...bucket,
								openTabs: newTabs,
							},
						};
					}
					return currentState;
				});
			},

			/**
			 * @zh 重置所有标签页状态
			 * @en Reset all tab states.
			 */
			resetTabs: () => {
				set(() => createInitialState());
			},

		}),
		{
			name: getAppNamespace("tabbar"),
			/**
			 * activeKey 不需要持久化存储
			 *
			 * 假如页面路由为 /home
			 * 手动在地址栏输入 /about
			 * activeKey 仍为 /home 导致 src/layout/layout-tabbar/index.tsx 的自动导航功能失效
			 * @see https://github.com/condorheroblog/react-antd-admin/issues/1
			 */
			partialize: (state): TabsPersistState => ({
				[appScopes.business]: serializeBucket(state[appScopes.business]),
				[appScopes.platform]: serializeBucket(state[appScopes.platform]),
			}),
			/**
			 * openTabs 是一个 Map，持久化存储需要手动处理
			 * How do I use it with Map and Set
			 * @see https://github.com/pmndrs/zustand/blob/v5.0.1/docs/integrations/persisting-store-data.md#how-do-i-use-it-with-map-and-set
			 */
			storage: {
				getItem: (name) => {
					const str = sessionStorage.getItem(name);
					// 是否开启持久化存储，如果未开启则在页面初次进入时返回 null 即可
					const isPersist = usePreferencesStore.getState().tabbarPersist;
					if (!str || !isPersist) {
						return null;
					}

					const existingValue = JSON.parse(str) as { state: TabsPersistState };
					const businessBucket = existingValue.state?.[appScopes.business];
					const platformBucket = existingValue.state?.[appScopes.platform];
					if (!businessBucket || !platformBucket) {
						return null;
					}

					return {
						...existingValue,
					};
				},
				setItem: (name, newValue) => {
					sessionStorage.setItem(name, JSON.stringify(newValue));
				},
				removeItem: name => sessionStorage.removeItem(name),
			},
			merge: (persistedState: unknown, currentState: TabsState & TabsAction): TabsState & TabsAction => {
				const existingValue = persistedState as { state?: TabsPersistState } | null;
				const businessBucket = existingValue?.state?.[appScopes.business];
				const platformBucket = existingValue?.state?.[appScopes.platform];
				if (!businessBucket || !platformBucket) {
					return currentState;
				}
				return {
					...currentState,
					[appScopes.business]: deserializeBucket(businessBucket as TabsPersistBucket),
					[appScopes.platform]: deserializeBucket(platformBucket as TabsPersistBucket),
				};
			},
		},
	),
);
