import type { AdminDomain } from "@uniondesk/shared";

import { AuthGuarded } from "#src/components/auth-guarded";
import { useAuth } from "#src/hooks/use-auth";

import type { MouseEvent } from "react";

import {
	CopyOutlined,
	DeleteOutlined,
	EditOutlined,
	EllipsisOutlined,
	GlobalOutlined,
	LoginOutlined,
	PoweroffOutlined,
	UserOutlined,
} from "@ant-design/icons";
import { App, Avatar, Button, Dropdown } from "antd";

import {
	domainLogoText,
	formatNonPublicVisibilityLabels,
	hasCustomDomainLogo,
	isDomainEnabled,
	isDomainPublic,
	resolveDomainLogoUrl,
} from "../domain-utils";

interface DomainCardProps {
	domain: AdminDomain;
	onManage: (domain: AdminDomain) => void;
	onEdit: (domain: AdminDomain) => void;
	onToggleStatus: (domain: AdminDomain) => void;
	onDelete: (domain: AdminDomain) => void;
}

export function DomainCard({
	domain,
	onManage,
	onEdit,
	onToggleStatus,
	onDelete,
}: DomainCardProps) {
	const { message } = App.useApp();
	const { hasPermission } = useAuth();
	const enabled = isDomainEnabled(domain);
	const isPublic = isDomainPublic(domain.visibility_policy_codes);
	const secondaryVisibilityLabels = formatNonPublicVisibilityLabels(domain.visibility_policy_codes);
	const showLogoImage = hasCustomDomainLogo(domain.logo);
	const logoText = domainLogoText(domain.name);
	const adminName = domain.creator_name?.trim() || "-";
	const description = domain.description?.trim() || "暂无描述";

	const stopBubble = (event: { stopPropagation: () => void }) => {
		event.stopPropagation();
	};

	const action = (handler: (d: AdminDomain) => void) => (event: MouseEvent) => {
		event.stopPropagation();
		handler(domain);
	};

	const handleClone = (event: MouseEvent) => {
		event.stopPropagation();
		message.info("复制业务域功能开发中");
	};

	const moreMenuItems = [
		hasPermission("domain.admin.update")
			? {
					key: "edit",
					label: "编辑",
					icon: <EditOutlined />,
					onClick: () => onEdit(domain),
				}
			: null,
		hasPermission("domain.admin.delete")
			? {
					key: "delete",
					label: "删除",
					danger: true,
					icon: <DeleteOutlined />,
					onClick: () => onDelete(domain),
				}
			: null,
	].filter((item): item is NonNullable<typeof item> => item != null);

	return (
		<div
			className="flex h-full w-full max-w-sm flex-col justify-between rounded-lg border border-[#f0f0f0] bg-white text-left shadow-sm transition-all duration-300 hover:border-[#1677ff]"
			role="button"
			tabIndex={0}
			onClick={() => onManage(domain)}
			onKeyDown={(event) => {
				if (event.key === "Enter" || event.key === " ") {
					event.preventDefault();
					onManage(domain);
				}
			}}
		>
			<div className="flex-grow cursor-pointer p-5">
				<div className="mb-4 flex items-start justify-between">
					<div className="flex min-w-0 items-center space-x-3">
						{showLogoImage ? (
							<Avatar
								size={40}
								src={resolveDomainLogoUrl(domain.logo)}
								className="shrink-0 border border-blue-200"
							/>
						) : (
							<div className="flex h-10 w-10 shrink-0 select-none items-center justify-center rounded border border-blue-200 bg-blue-50 text-xs font-bold text-[#1677ff]">
								{logoText}
							</div>
						)}
						<div className="min-w-0 truncate">
							<h3 className="truncate text-xs font-bold text-[rgba(0,0,0,0.88)]" title={domain.name}>
								{domain.name}
							</h3>
							<span className="mt-0.5 block truncate font-mono text-[10px] text-[rgba(0,0,0,0.45)]" title={domain.code}>
								{domain.code}
							</span>
						</div>
					</div>
					<span
						className={`inline-flex shrink-0 items-center rounded border px-2 py-0.5 text-[10px] font-semibold ${
							enabled
								? "border-[#b7eb8f] bg-[#f6ffed] text-[#52c41a]"
								: "border-[#f0f0f0] bg-[#fafafa] text-[rgba(0,0,0,0.45)]"
						}`}
					>
						<span className={`mr-1 h-1.5 w-1.5 rounded-full ${enabled ? "bg-[#52c41a]" : "bg-slate-400"}`} />
						{enabled ? "已启用" : "已禁用"}
					</span>
				</div>

				<p className="mb-4 line-clamp-3 text-xs leading-relaxed text-[rgba(0,0,0,0.45)]" title={description}>
					{description}
				</p>

				<div className="flex flex-wrap gap-1.5">
					{isPublic ? (
						<span className="flex items-center gap-1 rounded border border-[#91caec] bg-[#e6f4ff] px-2 py-0.5 text-[10px] font-medium text-[#1677ff]">
							<GlobalOutlined className="text-[9px]" />
							公开访问 (Public)
						</span>
					) : (
						<span className="flex items-center gap-1 rounded border border-[#ffccc7] bg-[#fff2f0] px-2 py-0.5 text-[10px] font-medium text-[#ff4d4f]">
							<UserOutlined className="text-[9px]" />
							私域保护 (Private)
						</span>
					)}
					{secondaryVisibilityLabels.map(label => (
						<span
							key={label}
							className="flex items-center gap-1 rounded border border-[#f0f0f0] bg-[#fafafa] px-2 py-0.5 text-[10px] text-[rgba(0,0,0,0.45)]"
						>
							{label}
						</span>
					))}
					<span className="flex items-center gap-1 rounded border border-[#f0f0f0] bg-[#fafafa] px-2 py-0.5 text-[10px] text-[rgba(0,0,0,0.45)]">
						<UserOutlined className="text-[9px]" />
						管理员:
						{" "}
						{adminName}
					</span>
				</div>
			</div>

			<div
				className="flex items-center justify-between gap-1.5 rounded-b-lg border-t border-[#f0f0f0] bg-[#fafafa]/50 px-4 py-2.5"
				onClick={stopBubble}
				onKeyDown={stopBubble}
			>
				<div className="flex items-center space-x-1">
					<AuthGuarded auth="domain.admin.update">
						<Button
							type="text"
							size="small"
							icon={<PoweroffOutlined className="text-[10px]" />}
							onClick={action(onToggleStatus)}
							className="text-[11px] text-[rgba(0,0,0,0.45)] hover:text-[#1677ff]"
						>
							启停
						</Button>
					</AuthGuarded>
					<AuthGuarded auth="domain.admin.create">
						<Button
							type="text"
							size="small"
							icon={<CopyOutlined className="text-[10px]" />}
							onClick={handleClone}
							className="text-[11px] text-[rgba(0,0,0,0.45)] hover:text-[#1677ff]"
						>
							克隆
						</Button>
					</AuthGuarded>
					{moreMenuItems.length > 0 ? (
						<Dropdown menu={{ items: moreMenuItems }} trigger={["click"]}>
							<Button
								type="text"
								size="small"
								icon={<EllipsisOutlined className="text-[10px]" />}
								onClick={stopBubble}
								className="text-[11px] text-[rgba(0,0,0,0.45)] hover:text-[#1677ff]"
							>
								更多
							</Button>
						</Dropdown>
					) : null}
				</div>
				<AuthGuarded auth="domain.admin.detail.read">
					<Button
						type="primary"
						size="small"
						icon={<LoginOutlined className="text-[10px]" />}
						onClick={action(onManage)}
						className="rounded bg-[#1677ff] text-xs font-semibold hover:bg-[#4096ff] active:bg-[#0958d9]"
					>
						进入控制台
					</Button>
				</AuthGuarded>
			</div>
		</div>
	);
}
