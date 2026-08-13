import type { AppRouteRecordRaw } from "#src/router/types";
import ContainerLayout from "#src/layout/container-layout";

import { home } from "#src/router/extra-info";
import { HomeOutlined } from "@ant-design/icons";
import { createElement, lazy } from "react";

const Home = lazy(() => import("#src/pages/home"));

const routes: AppRouteRecordRaw[] = [
	{
		path: "/home",
		Component: ContainerLayout,
		handle: {
			order: home,
			title: "概览",
			icon: createElement(HomeOutlined),
			scope: "business",
		},
		children: [
			{
				index: true,
				Component: Home,
				handle: {
					title: "概览",
					icon: createElement(HomeOutlined),
					scope: "business",
					auth: "domain.home.read",
				},
			},
		],
	},
];

export default routes;
