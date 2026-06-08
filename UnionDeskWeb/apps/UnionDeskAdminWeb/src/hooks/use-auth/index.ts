import { useCurrentRoute } from "#src/hooks/use-current-route";
import { useUserStore } from "#src/store/user";

import { useLocation } from "react-router";

export function useAuth() {
	const actions = useUserStore(state => state.actions);
	const currentRoute = useCurrentRoute();
	const { pathname } = useLocation();
	const pathnameScope = pathname.startsWith("/platform")
		? "platform"
		: pathname.startsWith("/system")
			? "domain"
			: undefined;
	const routeScope = (currentRoute?.handle?.scope as string | undefined) ?? pathnameScope ?? "business";

	const hasPermission = (code?: string | string[] | null): boolean => {
		if (!code) {
			return true;
		}
		if (!actions?.length) {
			return false;
		}
		if (Array.isArray(code)) {
			return code.some(item => actions.includes(item));
		}
		return actions.includes(code);
	};

	return {
		hasPermission,
		routeScope,
	};
}
