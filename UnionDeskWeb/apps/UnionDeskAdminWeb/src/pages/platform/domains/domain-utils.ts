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
