import { buildLoginRedirectSearch } from "#src/utils/safe-redirect";

export function rememberRoute() {
	const currentHashPath = window.location.hash.startsWith("#")
		? window.location.hash.slice(1)
		: "";
	const { pathname, search } = window.location;
	if (currentHashPath) {
		const hashPathname = currentHashPath.split("?")[0] ?? "";
		const hashSearch = currentHashPath.includes("?")
			? `?${currentHashPath.slice(currentHashPath.indexOf("?") + 1)}`
			: "";
		return buildLoginRedirectSearch(hashPathname, hashSearch);
	}
	return buildLoginRedirectSearch(pathname, search);
}
