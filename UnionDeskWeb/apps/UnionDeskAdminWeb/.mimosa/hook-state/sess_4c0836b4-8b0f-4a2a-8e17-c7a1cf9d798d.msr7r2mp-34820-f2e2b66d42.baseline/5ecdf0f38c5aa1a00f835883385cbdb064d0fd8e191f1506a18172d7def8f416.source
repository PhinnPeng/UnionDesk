import { GlobalSpin } from "#src/components/global-spin";
import { Scrollbar } from "#src/components/scrollbar";
import { useAppScope } from "#src/hooks/use-app-scope";
import { useLayoutContentStyle } from "#src/hooks/use-layout-style";
import { CSS_VARIABLE_LAYOUT_CONTENT_HEIGHT, ELEMENT_ID_MAIN_CONTENT } from "#src/layout/constants";
import LayoutFooter from "#src/layout/layout-footer";
import { useAccessStore } from "#src/store/access";
import { usePreferencesStore } from "#src/store/preferences";
import { useTabsStore } from "#src/store/tabs";

import { theme } from "antd";
import { KeepAlive, useKeepAliveRef } from "keepalive-for-react";
import { useEffect, useMemo } from "react";

import { useLocation, useOutlet } from "react-router";

export interface LayoutContentProps { }

export default function LayoutContent() {
	const {
		token: { colorBgLayout },
	} = theme.useToken();
	const { pathname } = useLocation();
	const outlet = useOutlet();
	const { contentElement } = useLayoutContentStyle();
	const appScope = useAppScope();

	const aliveRef = useKeepAliveRef();
	const isRefresh = useTabsStore(state => state[appScope].isRefresh);
	const businessOpenTabs = useTabsStore(state => state.business.openTabs);
	const platformOpenTabs = useTabsStore(state => state.platform.openTabs);
	const openTabs = useMemo(() => new Map([...businessOpenTabs, ...platformOpenTabs]), [businessOpenTabs, platformOpenTabs]);
	const tabbarEnable = usePreferencesStore(state => state.tabbarEnable);
	const flatRouteList = useAccessStore(state => state.flatRouteList);
	const transitionName = usePreferencesStore(state => state.transitionName);
	const transitionEnable = usePreferencesStore(state => state.transitionEnable);
	const enableFooter = usePreferencesStore(state => state.enableFooter);
	const fixedFooter = usePreferencesStore(state => state.fixedFooter);

	/**
	 * 区分页面缓存：仅使用 pathname，避免同一路由内 query（如详情 Tab）切换时被误判为未注册页签而销毁缓存。
	 */
	const cacheKey = useMemo(() => {
		return pathname;
	}, [pathname]);

	const resolveTabPathname = (key: string) => key.split(/[?#]/)[0];

	/**
	 * 当使用关闭当前标签页、关闭右侧标签页、关闭左侧标签页、关闭其他标签页、关闭所有标签页功能时，需要清除这个标签页的缓存
	 */
	useEffect(() => {
		const cacheNodes = aliveRef.current?.getCacheNodes?.();
		cacheNodes?.forEach((node) => {
			if (!openTabs.has(resolveTabPathname(node.cacheKey))) {
				aliveRef.current?.destroy(node.cacheKey);
			}
		});
	}, [openTabs]);

	/**
	 * 关闭多 tab 功能，清空所有的缓存页面
	 */
	useEffect(() => {
		if (!tabbarEnable) {
			const cacheNodes = aliveRef.current?.getCacheNodes?.();
			cacheNodes?.forEach((node) => {
				/* 不包含当前页面 */
				if (node.cacheKey !== cacheKey) {
					aliveRef.current?.destroy(node.cacheKey);
				}
			});
		}
	}, [tabbarEnable]);

	/* KeepAlive 的刷新 */
	useEffect(() => {
		/* 仅在启用标签栏时生效 */
		if (tabbarEnable && isRefresh) {
			aliveRef.current?.refresh();
		}
	}, [isRefresh]);

	/* 路由设置 keepAlive = false 则不缓存页面 */
	const keepAliveExclude = useMemo(() => {
		/**
		 * 如果不开启多 tab 功能，则不需要 KeepAlive 功能
		 * 为了保留页面的切换动画，只需要把所有的路由放到 exclude 数组中
		 */
		if (!tabbarEnable) {
			return Object.keys(flatRouteList);
		}
		return Object.entries(flatRouteList).reduce<string[]>((acc, [key, value]) => {
			if (value.handle.keepAlive === false) {
				acc.push(key);
			}
			return acc;
		}, []);
	}, [flatRouteList, tabbarEnable]);

	return (
		<main
			id={ELEMENT_ID_MAIN_CONTENT}
			ref={contentElement}
			className="relative overflow-y-auto overflow-x-hidden grow"
			style={
				{
					backgroundColor: colorBgLayout,
				}
			}
		>
			<Scrollbar>
				<GlobalSpin>
					<div
						className="flex flex-col h-full"
					>
						<div
							style={{
								height: `var(${CSS_VARIABLE_LAYOUT_CONTENT_HEIGHT})`,
							}}
						>
							<KeepAlive
								max={20}
								transition
								duration={300}
								cacheNodeClassName={transitionEnable ? `keepalive-${transitionName}` : undefined}
								exclude={keepAliveExclude}
								activeCacheKey={cacheKey}
								aliveRef={aliveRef}
							>
								{outlet}
							</KeepAlive>
						</div>
						{enableFooter && !fixedFooter ? <LayoutFooter /> : null}
					</div>
				</GlobalSpin>
			</Scrollbar>

		</main>
	);
}
