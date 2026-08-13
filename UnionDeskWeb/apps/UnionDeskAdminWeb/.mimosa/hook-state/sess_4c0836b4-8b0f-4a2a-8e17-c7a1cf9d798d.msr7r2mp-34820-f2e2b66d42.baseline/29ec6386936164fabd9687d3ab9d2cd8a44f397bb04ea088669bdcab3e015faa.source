import type { BreadcrumbProps } from "antd";
import type { RouteMeta } from "#src/router/types";

import { isString } from "#src/utils/is";

import { Breadcrumb } from "antd";
import { useTranslation } from "react-i18next";
import { useMatches } from "react-router";

interface BreadcrumbMatch {
	pathname: string
	handle?: RouteMeta
}

const itemRender: BreadcrumbProps["itemRender"] = (route, params, routes) => {
	const last = routes.indexOf(route) === routes.length - 1;
	return last || !route.path
		? (
			<span>{route.title}</span>
		)
		: (
			<span>{route.title}</span>
			// <NavLink to={route.path}>{route.title}</NavLink>
		);
};

function resolveBreadcrumbTitle(title: string, t: (key: string) => string) {
	if (title.includes(".") && /^[\w.-]+$/.test(title)) {
		return t(title);
	}
	return title;
}

export function buildBreadcrumbItems(matches: BreadcrumbMatch[], t: (key: string) => string): NonNullable<BreadcrumbProps["items"]> {
	return matches
		.filter(match => match.handle && !match.handle.hideInBreadcrumb && !match.pathname.endsWith("/"))
		.map((match) => {
			const rawTitle = match.handle?.title;
			return {
				title: isString(rawTitle) ? resolveBreadcrumbTitle(rawTitle, t) : rawTitle,
				path: match.pathname,
			};
		});
}

export function BreadcrumbViews() {
	const { t } = useTranslation();
	const matches = useMatches();

	return (
		<Breadcrumb
			className="hidden md:block"
			separator="->"
			// https://ant.design/components/breadcrumb#use-with-browserhistory
			itemRender={itemRender}
			items={buildBreadcrumbItems(matches, t)}
		/>
	);
}
