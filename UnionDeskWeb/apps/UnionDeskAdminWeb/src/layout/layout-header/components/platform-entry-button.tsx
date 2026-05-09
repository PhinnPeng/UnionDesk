import type { ButtonProps } from "antd";

import { BasicButton } from "#src/components/basic-button";
import { useUserStore } from "#src/store/user";
import { cn } from "#src/utils/cn";

import { AppstoreOutlined, RollbackOutlined } from "@ant-design/icons";
import { useLocation, useNavigate } from "react-router";

import { platformHomePath } from "#src/router/extra-info";

const businessHomePath = import.meta.env.VITE_BASE_HOME_PATH || "/system/menu";

export function PlatformEntryButton({ className, ...restProps }: ButtonProps) {
	const navigate = useNavigate();
	const { pathname } = useLocation();
	const platformAccess = useUserStore(state => state.platformAccess);

	const canUsePlatformEntry = platformAccess;
	if (!canUsePlatformEntry) {
		return null;
	}

	const isPlatformRoute = pathname.startsWith("/platform");
	const label = isPlatformRoute ? "返回业务端" : "平台管理";
	const targetPath = isPlatformRoute ? businessHomePath : platformHomePath;

	return (
		<BasicButton
			{...restProps}
			type="text"
			aria-label={label}
			title={label}
			className={cn(className, "rounded-full px-3")}
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
