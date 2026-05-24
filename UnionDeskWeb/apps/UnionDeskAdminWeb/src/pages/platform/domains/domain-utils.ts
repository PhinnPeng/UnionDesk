import type { AdminDomain, P0VisibilityPolicyCode } from "@uniondesk/shared";

import { visibilityOptions } from "./constants";

/** 默认业务域 Logo（静态资源） */
export const DEFAULT_DOMAIN_LOGO = "/default-domain-logo.svg";

/** Logo 上传：允许的图片 MIME */
export const DOMAIN_LOGO_ACCEPT = "image/png,image/jpeg,image/webp";

/** Logo 上传最大体积（2MB） */
export const DOMAIN_LOGO_MAX_BYTES = 2 * 1024 * 1024;

/** 新建域时 Logo 上传暂用默认业务域 ID（附件 API 要求 domain_id） */
export const DOMAIN_LOGO_UPLOAD_FALLBACK_DOMAIN_ID = 1;

/** 业务域是否处于启用状态 */
export function isDomainEnabled(domain: Pick<AdminDomain, "status">): boolean {
	const status = domain.status;
	return status === "1" || status === "active" || status === "enabled";
}

/** 将平台业务域 ID 转为域配置接口使用的数字 ID */
export function resolveNumericDomainId(domainId: string): number | null {
	const trimmed = domainId.trim();
	if (!trimmed || !/^\d+$/.test(trimmed)) {
		return null;
	}
	const numeric = Number(trimmed);
	return Number.isSafeInteger(numeric) ? numeric : null;
}

/** 解析展示用 Logo URL（空则回退默认图） */
export function resolveDomainLogoUrl(logo?: string | null): string {
	const trimmed = logo?.trim();
	return trimmed && isValidLogoUrl(trimmed) ? trimmed : DEFAULT_DOMAIN_LOGO;
}

/** Logo URL 校验 */
export function isValidLogoUrl(value: string | undefined, options?: { required?: boolean }): boolean {
	const required = options?.required ?? false;
	if (!value?.trim()) {
		return !required;
	}
	try {
		const url = new URL(value.trim());
		return url.protocol === "http:" || url.protocol === "https:" || value.startsWith("/");
	}
	catch {
		return value.startsWith("/");
	}
}

/** 可见策略标签文案 */
export function formatVisibilityLabels(codes: P0VisibilityPolicyCode[]): string[] {
	return codes.map(code => visibilityOptions.find(item => item.value === code)?.label ?? code);
}

/** 业务域是否为公开可见策略 */
export function isDomainPublic(codes: P0VisibilityPolicyCode[]): boolean {
	return codes.includes("public");
}

/** 非 public 的可见策略标签（用于卡片次要 Tag） */
export function formatNonPublicVisibilityLabels(codes: P0VisibilityPolicyCode[]): string[] {
	return formatVisibilityLabels(codes.filter(code => code !== "public"));
}

/** Logo 占位文字（名称首字母） */
export function domainLogoText(name: string): string {
	return name.trim().charAt(0).toUpperCase() || "?";
}

/** 是否使用自定义 Logo 图片（非默认静态图） */
export function hasCustomDomainLogo(logo?: string | null): boolean {
	const trimmed = logo?.trim();
	if (!trimmed || trimmed === DEFAULT_DOMAIN_LOGO) {
		return false;
	}
	return isValidLogoUrl(trimmed);
}

/**
 * 可见策略勾选变更（PRD：public 与后两项互斥；domain_customer_only 与 channel_only 可组合）
 */
export function applyVisibilityPolicyChange(
	previous: P0VisibilityPolicyCode[],
	next: P0VisibilityPolicyCode[],
): P0VisibilityPolicyCode[] {
	const added = next.filter(code => !previous.includes(code));
	if (added.includes("public")) {
		return ["public"];
	}
	if (previous.includes("public") && next.some(code => code !== "public")) {
		return next.filter(code => code !== "public");
	}
	if (next.includes("public") && next.length > 1) {
		return next.filter(code => code !== "public");
	}
	return next.length > 0 ? next : ["public"];
}

/** 由名称生成短码候选（小写、数字、下划线、连字符） */
export function slugifyDomainCode(seed: string): string {
	return seed
		.trim()
		.toLowerCase()
		.replace(/[^a-z0-9]+/g, "-")
		.replace(/^-+|-+$/g, "")
		.replace(/-{2,}/g, "-")
		.slice(0, 48);
}

/** 生成随机业务域短码 */
export function generateDomainCode(seed?: string): string {
	const base = seed?.trim() ? slugifyDomainCode(seed) : "domain";
	const suffix = Math.random().toString(36).slice(2, 8);
	const combined = base ? `${base}-${suffix}` : `domain-${suffix}`;
	return combined.replace(/[^a-z0-9_-]/g, "").slice(0, 64);
}
