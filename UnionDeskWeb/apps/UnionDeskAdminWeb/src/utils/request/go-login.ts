import { router } from "#src/router";
import { loginPath } from "#src/router/extra-info";
import { useAuthStore } from "#src/store/auth";
import { rememberRoute } from "#src/utils/remember-route";

export function goLogin() {
	useAuthStore.getState().reset();
	router.navigate(`${loginPath}${rememberRoute()}`, {
		replace: true,
	});
}
