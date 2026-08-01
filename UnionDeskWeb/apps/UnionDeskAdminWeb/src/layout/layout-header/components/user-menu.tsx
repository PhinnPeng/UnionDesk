import type { ButtonProps, MenuProps } from "antd";

import { BasicButton } from "#src/components/basic-button";
import { RiAccountCircleLine } from "#src/icons";
import { usePreferencesDrawerStore } from "#src/layout/widgets/preferences/preferences-drawer-store";
import { loginPath } from "#src/router/extra-info";
import { useAuthStore } from "#src/store/auth";
import { useUserStore } from "#src/store/user";
import { cn } from "#src/utils/cn";
import { isWindowsOs } from "#src/utils/is-windows-os";

import { LogoutOutlined, SettingOutlined } from "@ant-design/icons";
import { useKeyPress } from "ahooks";
import { Avatar, Dropdown } from "antd";
import { useMemo } from "react";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router";

export function UserMenu({ ...restProps }: ButtonProps) {
	const navigate = useNavigate();
	const { t } = useTranslation();
	const avatar = useUserStore(state => state.avatar);
	const logout = useAuthStore(state => state.logout);
	const openPreferences = usePreferencesDrawerStore(state => state.openDrawer);

	const onClick: MenuProps["onClick"] = async ({ key }) => {
		if (key === "logout") {
			await logout();
			navigate(loginPath);
		}
		if (key === "personal-center") {
			navigate("/personal-center/my-profile");
		}
		if (key === "preferences") {
			openPreferences();
		}
	};

	const altView = useMemo(() => isWindowsOs() ? "Alt" : "⌥", [isWindowsOs]);
	const items: MenuProps["items"] = [
		{
			label: t("common.menu.personalCenter"),
			key: "personal-center",
			icon: <RiAccountCircleLine />,
			extra: `${altView}P`,
		},
		{
			label: t("preferences.title"),
			key: "preferences",
			icon: <SettingOutlined />,
		},
		{ type: "divider" },
		{
			label: t("authority.logout"),
			key: "logout",
			icon: <LogoutOutlined />,
			extra: `${altView}Q`,
		},
	];

	useKeyPress(["alt.P"], () => {
		navigate("/personal-center/my-profile");
	});

	useKeyPress(["alt.Q"], () => {
		onClick({ key: "logout" } as any);
	});

	return (
		<Dropdown
			menu={{ items, onClick }}
			arrow={false}
			placement="bottomRight"
			trigger={["click"]}
		>
			<BasicButton
				type="text"
				{...restProps}
				className={cn(restProps.className, "rounded-full px-1")}
			>
				<Avatar src={avatar || undefined} />
			</BasicButton>
		</Dropdown>
	);
}
