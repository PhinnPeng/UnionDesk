/** 判断 iframe 地址是否可安全加载（禁止嵌入本站，避免 chrome-error 与同源策略报错） */
export function resolveSafeIframeSrc(link?: string | null): string | null {
	const trimmed = link?.trim();
	if (!trimmed) {
		return null;
	}

	try {
		const parsed = new URL(trimmed, window.location.origin);
		if (!["http:", "https:"].includes(parsed.protocol)) {
			return null;
		}
		if (parsed.origin === window.location.origin) {
			return null;
		}
		return parsed.toString();
	}
	catch {
		return null;
	}
}
