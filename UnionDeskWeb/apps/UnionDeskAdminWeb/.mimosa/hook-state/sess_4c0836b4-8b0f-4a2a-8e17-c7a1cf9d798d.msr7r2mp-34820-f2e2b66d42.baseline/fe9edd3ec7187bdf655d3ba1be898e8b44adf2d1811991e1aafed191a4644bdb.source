import type { ButtonProps } from "antd";

import { switchAppScope } from "#src/api/user";
import { BasicButton } from "#src/components/basic-button";
import { hideLoading } from "#src/plugins/hide-loading";
import { showLoading } from "#src/plugins/loading";
import { appScopes } from "#src/router/extra-info/app-scope";
import { useUserStore } from "#src/store/user";
import { cn } from "#src/utils/cn";

import { AppstoreOutlined, RollbackOutlined } from "@ant-design/icons";
import { App } from "antd";
import { useState } from "react";
import { useLocation, useNavigate } from "react-router";

export function PlatformEntryButton({ className, ...restProps }: ButtonProps) {
	const { message } = App.useApp();
	const navigate = useNavigate();
	const { pathname } = useLocation();
	const platformAccess = useUserStore(state => state.platformAccess);
	const businessDomainAccess = useUserStore(state => state.businessDomainAccess);
	const [switching, setSwitching] = useState(false);

	const isPlatformRoute = pathname.startsWith("/platform");
	const hasBusinessAccess = Boolean(businessDomainAccess);

	if (isPlatformRoute) {
		if (!hasBusinessAccess) {
			return null;
		}
	}
	else if (!platformAccess) {
		return null;
	}

	const label = isPlatformRoute ? "返回业务端" : "平台管理";
	const targetScope = isPlatformRoute ? appScopes.business : appScopes.platform;
	const loadingMessage = isPlatformRoute ? "正在返回业务端…" : "正在进入平台管理…";

	return (
		<BasicButton
			{...restProps}
			type="text"
			aria-label={label}
			title={label}
			loading={switching}
			disabled={switching || restProps.disabled}
			className={cn(className, "rounded-full px-[11px]")}
			icon={isPlatformRoute ? <RollbackOutlined /> : <AppstoreOutlined />}
			onClick={(event) => {
				restProps.onClick?.(event);
				if (switching) {
					return;
				}
				setSwitching(true);
				showLoading(loadingMessage);
				void switchAppScope(targetScope)
					.then((targetPath) => {
						navigate(targetPath);
					})
					.catch((error) => {
						hideLoading();
						message.error(error instanceof Error ? error.message : "切换控制台失败");
					})
					.finally(() => {
						setSwitching(false);
					});
			}}
		>
			{label}
		</BasicButton>
	);
}
