import type { BusinessDomainView } from "@uniondesk/shared";
import type { MenuProps } from "antd";
import type { MouseEvent } from "react";

import { reloadBusinessAccessAfterDomainSwitch, switchAppScope } from "#src/api/user";
import { usePreferences } from "#src/hooks/use-preferences";
import { RiAccountCircleLine } from "#src/icons";
import { usePreferencesDrawerStore } from "#src/layout/widgets/preferences/preferences-drawer-store";
import { hideLoading } from "#src/plugins/hide-loading";
import { showLoading } from "#src/plugins/loading";
import { loginPath } from "#src/router/extra-info";
import { appScopes } from "#src/router/extra-info/app-scope";
import { useAuthStore } from "#src/store/auth";
import { useTabsStore } from "#src/store/tabs";
import { useUserStore } from "#src/store/user";
import { cn } from "#src/utils/cn";

import {
	ApartmentOutlined,
	AppstoreOutlined,
	CaretDownOutlined,
	CaretUpOutlined,
	CheckOutlined,
	LogoutOutlined,
	MoreOutlined,
	QuestionCircleOutlined,
	RollbackOutlined,
	SettingOutlined,
} from "@ant-design/icons";
import { App, Avatar, Button, ConfigProvider, Menu, Popover, Spin, theme as antdTheme } from "antd";
import { useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { useLocation, useNavigate } from "react-router";

import "./index.less";

interface SidebarFooterProps {
	className?: string
	/** 移动端抽屉内强制展示业务域（忽略平台路由隐藏） */
	forceShowDomain?: boolean
}

function useSidebarPopupStyles() {
	const { token } = antdTheme.useToken();
	return {
		content: {
			padding: 4,
			background: token.colorBgElevated,
			color: token.colorText,
		},
	} as const;
}

function SidebarPopupTheme({
	isDark,
	children,
}: {
	isDark: boolean
	children: React.ReactNode
}) {
	return (
		<ConfigProvider
			theme={{
				algorithm: isDark ? antdTheme.darkAlgorithm : antdTheme.defaultAlgorithm,
			}}
		>
			{children}
		</ConfigProvider>
	);
}

function sortDomains(
	domains: BusinessDomainView[],
	preferredDefaultDomainId: number | null,
): BusinessDomainView[] {
	if (!preferredDefaultDomainId) {
		return domains;
	}
	const preferred = domains.find(domain => domain.id === preferredDefaultDomainId);
	if (!preferred) {
		return domains;
	}
	return [preferred, ...domains.filter(domain => domain.id !== preferredDefaultDomainId)];
}

function DomainFooterItem({
	collapsed,
	isDark,
	forceShow,
}: {
	collapsed: boolean
	isDark: boolean
	forceShow?: boolean
}) {
	const { message } = App.useApp();
	const navigate = useNavigate();
	const { pathname } = useLocation();
	const [open, setOpen] = useState(false);
	const [switching, setSwitching] = useState(false);
	const [starringId, setStarringId] = useState<number | null>(null);

	const currentDomainId = useAuthStore(state => state.defaultBusinessDomainId);
	const preferredDefaultDomainId = useAuthStore(state => state.preferredDefaultDomainId);
	const accessibleDomains = useAuthStore(state => state.accessibleDomains);
	const switchDomain = useAuthStore(state => state.switchDomain);
	const setDefaultDomain = useAuthStore(state => state.setDefaultDomain);
	const popupStyles = useSidebarPopupStyles();

	const isPlatformRoute = pathname.startsWith("/platform");
	const domains = accessibleDomains ?? [];

	const sortedDomains = useMemo(
		() => sortDomains(domains, preferredDefaultDomainId),
		[domains, preferredDefaultDomainId],
	);

	const currentDomain = useMemo(() => {
		if (!domains.length) {
			return null;
		}
		return domains.find(domain => domain.id === currentDomainId) ?? domains[0] ?? null;
	}, [domains, currentDomainId]);

	if (!forceShow && isPlatformRoute) {
		return null;
	}
	if (!domains.length || !currentDomain) {
		return null;
	}

	const handleSwitch = async (domainId: number) => {
		if (domainId === currentDomainId || switching) {
			return;
		}
		setSwitching(true);
		showLoading("正在切换业务域…");
		try {
			await switchDomain(domainId);
			useTabsStore.getState().resetTabs();
			const targetPath = await reloadBusinessAccessAfterDomainSwitch(pathname);
			setOpen(false);
			await navigate(targetPath, { replace: true });
			message.success("已切换业务域");
		}
		catch (error) {
			hideLoading();
			message.error(error instanceof Error ? error.message : "切换业务域失败");
		}
		finally {
			setSwitching(false);
		}
	};

	const handleSetDefault = async (domainId: number, event: MouseEvent) => {
		event.stopPropagation();
		if (starringId != null || preferredDefaultDomainId === domainId) {
			return;
		}
		setStarringId(domainId);
		try {
			await setDefaultDomain(domainId);
			message.success("已设为默认业务域");
		}
		catch (error) {
			message.error(error instanceof Error ? error.message : "设置默认业务域失败");
		}
		finally {
			setStarringId(null);
		}
	};

	const popoverContent = (
		<SidebarPopupTheme isDark={isDark}>
			<div className="sidebar-domain-popover">
				<div className="sidebar-domain-popover__title">切换业务域</div>
				<Spin spinning={switching}>
					<ul className="sidebar-domain-popover__list">
						{sortedDomains.map((domain) => {
							const isCurrent = domain.id === currentDomainId;
							const isPreferred = domain.id === preferredDefaultDomainId;
							return (
								<li key={domain.id}>
									<button
										type="button"
										className={cn(
											"sidebar-domain-popover__row",
											isCurrent && "sidebar-domain-popover__row--active",
										)}
										onClick={() => void handleSwitch(domain.id)}
									>
										<span className="sidebar-domain-popover__row-main">
											<span className="sidebar-domain-popover__name">{domain.name}</span>
											{isCurrent ? <CheckOutlined className="sidebar-domain-popover__check" /> : null}
										</span>
										<Button
											size="small"
											type="text"
											disabled={isPreferred || starringId != null}
											loading={starringId === domain.id}
											onClick={event => void handleSetDefault(domain.id, event)}
										>
											{isPreferred ? "默认" : "设为默认"}
										</Button>
									</button>
								</li>
							);
						})}
					</ul>
				</Spin>
			</div>
		</SidebarPopupTheme>
	);

	const trigger = (
		<button
			type="button"
			title={collapsed ? currentDomain.name : undefined}
			className={cn(
				"sidebar-footer__item",
				isDark && "sidebar-footer__item--dark",
				collapsed && "sidebar-footer__item--collapsed",
			)}
		>
			<ApartmentOutlined className="sidebar-footer__item-icon" />
			{collapsed
				? null
				: (
					<>
						<span className="sidebar-footer__item-label">{currentDomain.name}</span>
						<MoreOutlined className="sidebar-footer__item-extra" />
					</>
				)}
		</button>
	);

	return (
		<Popover
			trigger="click"
			placement="rightTop"
			open={open}
			onOpenChange={setOpen}
			content={popoverContent}
			arrow={false}
			classNames={{ root: cn("sidebar-footer-popover", isDark && "sidebar-footer-popover--dark") }}
			styles={popupStyles}
		>
			{trigger}
		</Popover>
	);
}

function PlatformFooterItem({
	collapsed,
	isDark,
}: {
	collapsed: boolean
	isDark: boolean
}) {
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
	const icon = isPlatformRoute ? <RollbackOutlined className="sidebar-footer__item-icon" /> : <AppstoreOutlined className="sidebar-footer__item-icon" />;
	const targetScope = isPlatformRoute ? appScopes.business : appScopes.platform;
	const loadingMessage = isPlatformRoute ? "正在返回业务端…" : "正在进入平台管理…";

	return (
		<button
			type="button"
			title={collapsed ? label : undefined}
			disabled={switching}
			className={cn(
				"sidebar-footer__item",
				isDark && "sidebar-footer__item--dark",
				collapsed && "sidebar-footer__item--collapsed",
			)}
			onClick={() => {
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
			{icon}
			{collapsed ? null : <span className="sidebar-footer__item-label">{label}</span>}
		</button>
	);
}

function HelpFooterItem({
	collapsed,
	isDark,
}: {
	collapsed: boolean
	isDark: boolean
}) {
	const { message } = App.useApp();

	return (
		<button
			type="button"
			title={collapsed ? "帮助中心" : undefined}
			className={cn(
				"sidebar-footer__item",
				isDark && "sidebar-footer__item--dark",
				collapsed && "sidebar-footer__item--collapsed",
			)}
			onClick={() => {
				message.info("功能开发中");
			}}
		>
			<QuestionCircleOutlined className="sidebar-footer__item-icon" />
			{collapsed ? null : <span className="sidebar-footer__item-label">帮助中心</span>}
		</button>
	);
}

function UserFooterPanel({
	collapsed,
	isDark,
}: {
	collapsed: boolean
	isDark: boolean
}) {
	const { t } = useTranslation();
	const navigate = useNavigate();
	const [open, setOpen] = useState(false);
	const avatar = useUserStore(state => state.avatar);
	const username = useUserStore(state => state.username);
	const logout = useAuthStore(state => state.logout);
	const openPreferences = usePreferencesDrawerStore(state => state.openDrawer);
	const popupStyles = useSidebarPopupStyles();

	const onClick: MenuProps["onClick"] = async ({ key }) => {
		setOpen(false);
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

	const items: MenuProps["items"] = [
		{
			label: t("common.menu.personalCenter"),
			key: "personal-center",
			icon: <RiAccountCircleLine />,
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
		},
	];

	const menu = (
		<SidebarPopupTheme isDark={isDark}>
			<Menu
				items={items}
				onClick={onClick}
				selectable={false}
				style={{ minWidth: 160, border: "none", background: "transparent" }}
			/>
		</SidebarPopupTheme>
	);

	return (
		<div className="sidebar-footer__user-wrap">
			<Popover
				trigger="click"
				placement="rightTop"
				arrow={false}
				open={open}
				onOpenChange={setOpen}
				content={menu}
				classNames={{ root: cn("sidebar-footer-popover", isDark && "sidebar-footer-popover--dark") }}
				styles={popupStyles}
			>
				<button
					type="button"
					title={collapsed ? (username || "用户") : undefined}
					className={cn(
						"sidebar-footer__user-bar",
						open && "sidebar-footer__user-bar--open",
						collapsed && "sidebar-footer__user-bar--collapsed",
					)}
				>
					<span className="sidebar-footer__avatar">
						<Avatar size={22} src={avatar || undefined}>
							{(username || "用").slice(0, 1)}
						</Avatar>
					</span>
					{collapsed
						? null
						: (
							<>
								<span className="sidebar-footer__user-name">
									{username || "用户"}
								</span>
								<span className="sidebar-footer__user-swap" aria-hidden>
									<CaretUpOutlined />
									<CaretDownOutlined />
								</span>
							</>
						)}
				</button>
			</Popover>
		</div>
	);
}

export function SidebarFooter({ className, forceShowDomain = false }: SidebarFooterProps) {
	const { sidebarCollapsed, sidebarTheme, isDark } = usePreferences();
	const isFixedDarkTheme = isDark || sidebarTheme === "dark";

	return (
		<div
			className={cn(
				"sidebar-footer",
				isFixedDarkTheme && "sidebar-footer--dark",
				sidebarCollapsed && "sidebar-footer--collapsed",
				className,
			)}
		>
			<DomainFooterItem
				collapsed={sidebarCollapsed}
				isDark={isFixedDarkTheme}
				forceShow={forceShowDomain}
			/>
			<PlatformFooterItem collapsed={sidebarCollapsed} isDark={isFixedDarkTheme} />
			<HelpFooterItem collapsed={sidebarCollapsed} isDark={isFixedDarkTheme} />
			<UserFooterPanel collapsed={sidebarCollapsed} isDark={isFixedDarkTheme} />
		</div>
	);
}
