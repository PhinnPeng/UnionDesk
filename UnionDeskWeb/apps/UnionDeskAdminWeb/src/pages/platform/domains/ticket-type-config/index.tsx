import { Navigate, useParams } from "react-router";

export default function PlatformTicketTypeConfigRedirect() {
	const { domainId = "", typeId = "" } = useParams();

	if (!domainId.trim() || !typeId.trim()) {
		return <Navigate to="/platform/domains" replace />;
	}

	return (
		<Navigate
			to={`/platform/domains/ticket/form-design/${encodeURIComponent(domainId.trim())}/${encodeURIComponent(typeId.trim())}`}
			replace
		/>
	);
}
