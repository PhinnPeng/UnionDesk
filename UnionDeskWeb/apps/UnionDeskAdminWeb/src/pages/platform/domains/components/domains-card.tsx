import type { AdminDomain, P0VisibilityPolicyCode } from "@uniondesk/shared";

import { AuthGuarded } from "#src/components/auth-guarded";

import { PLATFORM_DOMAIN_CONTROL_READ } from "../platform-domain-permissions";

import { ClockCircleOutlined, LoginOutlined, SafetyOutlined } from "@ant-design/icons";
import { Avatar } from "antd";
import dayjs from "dayjs";

import styles from "./domains-card.module.less";

export interface DomainsCardProps {
	domain: AdminDomain;
	onManage: (domain: AdminDomain) => void;
}

const DEFAULT_DOMAIN_LOGO = "/default-domain-logo.svg";

function isDomainEnabled(domain: Pick<AdminDomain, "status">): boolean {
	const status = domain.status;
	return status === "1" || status === "active" || status === "enabled";
}

function isDomainPublic(codes: P0VisibilityPolicyCode[]): boolean {
	return codes.includes("public");
}

function isValidLogoUrl(value: string | undefined): boolean {
	if (!value?.trim()) {
		return false;
	}
	try {
		const url = new URL(value.trim());
		return url.protocol === "http:" || url.protocol === "https:" || value.startsWith("/");
	}
	catch {
		return value.startsWith("/");
	}
}

function resolveDomainLogoUrl(logo?: string | null): string {
	const trimmed = logo?.trim();
	if (!trimmed || trimmed === DEFAULT_DOMAIN_LOGO) {
		return DEFAULT_DOMAIN_LOGO;
	}
	return isValidLogoUrl(trimmed) ? trimmed : DEFAULT_DOMAIN_LOGO;
}

/** 业务域列表单卡 */
export function DomainsCard({ domain, onManage }: DomainsCardProps) {
	const handleEnterConsole = () => {
		onManage(domain);
	};

	const enabled = isDomainEnabled(domain);
	const isPublic = isDomainPublic(domain.visibility_policy_codes);
	const description = domain.description?.trim() || "暂无描述";
	const visibilityLabel = isPublic ? "公开模式" : "私域隔离";
	const createdAtLabel = domain.created_at
		? dayjs(domain.created_at).format("YYYY-MM-DD")
		: "-";

	return (
		<div className={`${styles.card} group`}>
			<div className={styles.cardBody}>
				<div className="mb-4 flex items-start justify-between">
					<div className="flex min-w-0 items-center space-x-3">
						<Avatar
							size={40}
							src={resolveDomainLogoUrl(domain.logo)}
							className="shrink-0 border border-colorBorderSecondary"
						/>
						<div className="min-w-0 truncate">
							<h3
								className="truncate text-xs font-bold text-colorText transition group-hover:text-colorPrimary"
								title={domain.name}
							>
								{domain.name}
							</h3>
							<span className="mt-0.5 block truncate font-mono text-[10px] text-colorTextSecondary" title={domain.code}>
								{domain.code}
							</span>
						</div>
					</div>
					<span
						className={`${styles.statusBadge} ${
							enabled ? styles.statusBadgeEnabled : styles.statusBadgeDisabled
						}`}
					>
						<span
							className={`${styles.statusDot} ${
								enabled ? `${styles.statusDotEnabled} animate-pulse` : styles.statusDotDisabled
							}`}
						/>
						<span className={styles.statusLabel}>{enabled ? "正常" : "禁用"}</span>
					</span>
				</div>

				<p className="mb-4 line-clamp-3 text-xs leading-relaxed text-colorTextSecondary" title={description}>
					{description}
				</p>

				<div className="grid grid-cols-2 gap-x-2 gap-y-1 border-t border-colorBorderSecondary pt-3 text-[10px] leading-normal text-colorTextSecondary">
					<div className="flex items-center gap-1">
						<SafetyOutlined className="w-3.5 text-center text-colorTextTertiary" />
						<span>访问模式：</span>
						<span className="font-bold text-colorText">{visibilityLabel}</span>
					</div>
					<div className="flex items-center gap-1">
						<ClockCircleOutlined className="w-3.5 text-center text-colorTextTertiary" />
						<span>创建时间：</span>
						<span className="font-mono font-bold text-colorText">{createdAtLabel}</span>
					</div>
				</div>
			</div>

			<div className={styles.cardFooter}>
				<AuthGuarded auth={PLATFORM_DOMAIN_CONTROL_READ}>
					<button
						type="button"
						className={styles.enterConsoleButton}
						onClick={handleEnterConsole}
					>
						<LoginOutlined className="text-[11px]" />
						进入控制台
					</button>
				</AuthGuarded>
			</div>
		</div>
	);
}
