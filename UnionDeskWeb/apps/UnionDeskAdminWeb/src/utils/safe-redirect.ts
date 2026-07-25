import { loginPath } from "#src/router/extra-info/route-path";

/**
 * Build `?redirect=` query for the login page. Returns "" when there is nothing safe to remember.
 */
export function buildLoginRedirectSearch(pathname: string, search = ""): string {
	const target = `${pathname}${search}`;
	if (!isInternalAppPath(target) || isLoginPath(target)) {
		return "";
	}
	return `?redirect=${encodeURIComponent(target)}`;
}

/**
 * Parse and validate a redirect target from the login URL. Rejects open redirects.
 */
export function getSafeRedirect(raw: string | null | undefined): string | null {
	if (raw == null) {
		return null;
	}
	const trimmed = raw.trim();
	if (!trimmed) {
		return null;
	}
	let decoded = trimmed;
	try {
		decoded = decodeURIComponent(trimmed);
	}
	catch {
		// keep trimmed
	}
	if (!isInternalAppPath(decoded) || isLoginPath(decoded)) {
		return null;
	}
	return decoded;
}

function isLoginPath(path: string): boolean {
	return path === loginPath
		|| path.startsWith(`${loginPath}?`)
		|| path.startsWith(`${loginPath}/`);
}

function isInternalAppPath(path: string): boolean {
	if (!path.startsWith("/") || path.startsWith("//")) {
		return false;
	}
	// Reject absolute / protocol URLs that snuck past encoding
	if (/^[a-zA-Z][a-zA-Z\d+\-.]*:/.test(path)) {
		return false;
	}
	return path.length > 1;
}
