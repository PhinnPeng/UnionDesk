import type { ReactNode } from "react";

import { useAuth } from "#src/hooks/use-auth";

interface AuthGuardedProps {
	auth?: string | null
	fallback?: ReactNode
	children: ReactNode
}

export function AuthGuarded({
	auth,
	fallback = null,
	children,
}: AuthGuardedProps) {
	const { hasPermission } = useAuth();

	if (!hasPermission(auth)) {
		return fallback;
	}

	return children;
}
