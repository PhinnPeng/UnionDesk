import { Navigate } from "react-router";

type LegacyDomainRedirectProps = {
	to: string
};

export default function LegacyDomainRedirect({ to }: LegacyDomainRedirectProps) {
	return <Navigate to={to} replace />;
}

export function createLegacyRedirect(to: string) {
	return function LegacyRedirect() {
		return <Navigate to={to} replace />;
	};
}
