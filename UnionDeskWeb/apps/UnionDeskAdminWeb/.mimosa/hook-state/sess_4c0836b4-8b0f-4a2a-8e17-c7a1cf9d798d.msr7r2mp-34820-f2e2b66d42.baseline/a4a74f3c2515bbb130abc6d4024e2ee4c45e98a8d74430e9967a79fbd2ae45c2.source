import { router } from "#src/router";
import { loginPath } from "#src/router/extra-info";
import { useAuthStore } from "#src/store/auth";
import { rememberRoute } from "#src/utils/remember-route";

let goLoginInFlight = false;

function isOnLoginPage(): boolean {
	const hashPath = window.location.hash.startsWith("#")
		? window.location.hash.slice(1).split("?")[0]
		: "";
	const pathname = hashPath || window.location.pathname;
	return pathname === loginPath;
}

/**
 * Clear session and navigate to login (single-flight). No-ops navigation when already on login.
 */
export function goLogin() {
	if (goLoginInFlight) {
		return;
	}
	goLoginInFlight = true;
	try {
		useAuthStore.getState().reset();
		if (isOnLoginPage()) {
			return;
		}
		void router.navigate(`${loginPath}${rememberRoute()}`, {
			replace: true,
		});
	}
	finally {
		queueMicrotask(() => {
			goLoginInFlight = false;
		});
	}
}
