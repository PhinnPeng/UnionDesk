import type { ButtonProps } from "antd";

import { BasicButton } from "#src/components/basic-button";
import type { AppRouteRecordRaw } from "#src/router/types";
import { useUserStore } from "#src/store/user";
import { hasBusinessDomainAccess } from "#src/utils/access/business-domain";
import { cn } from "#src/utils/cn";

import { AppstoreOutlined, RollbackOutlined } from "@ant-design/icons";
import { useLocation, useNavigate } from "react-router";

import { businessHomePath, platformHomePath } from "#src/router/extra-info";
const EMPTY_MENUS: AppRouteRecordRaw[] = [];

export function PlatformEntryButton({ className, ...restProps }: ButtonProps) {
	const navigate = useNavigate();
	const { pathname } = useLocation();
	const platformAccess = useUserStore(state => state.platformAccess);
	const menus = useUserStore(state => state.menus);
	const businessDomainAccess = useUserStore(state => state.businessDomainAccess);

	if (!platformAccess) {
		return null;
	}

	const isPlatformRoute = pathname.startsWith("/platform");
	const hasBusinessAccess = typeof businessDomainAccess === "boolean"
		? businessDomainAccess
		: hasBusinessDomainAccess(menus ?? EMPTY_MENUS);
	if (isPlatformRoute && !hasBusinessAccess) {
		return null;
	}
	const label = isPlatformRoute ? "返回业务端" : "平台管理";
	const targetPath = isPlatformRoute ? businessHomePath : platformHomePath;

	return (
		<BasicButton
			{...restProps}
			type="text"
			aria-label={label}
			title={label}
			className={cn(className, "rounded-full px-[11px]")}
			icon={isPlatformRoute ? <RollbackOutlined /> : <AppstoreOutlined />}
			onClick={(event) => {
				restProps.onClick?.(event);
				if (isPlatformRoute) {
					window.location.assign(targetPath);
					return;
				}
				navigate(targetPath);
			}}
		>
			{label}
		</BasicButton>
	);
}
