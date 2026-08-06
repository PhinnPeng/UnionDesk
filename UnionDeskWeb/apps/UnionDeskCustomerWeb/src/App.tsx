import {
	getCustomerPortalSnapshot,
	loadAuthSession,
	restoreCustomerPortalLive,
	setUnauthorizedHandler,
} from "@uniondesk/shared";
import { useEffect, useState } from "react";
import { Navigate, Outlet, Route, Routes, useLocation, useNavigate } from "react-router-dom";

import AppShell from "./components/AppShell";
import ChangePasswordPage from "./pages/change-password";
import ChatPage from "./pages/chat";
import DomainsPage from "./pages/domains";
import HomePage from "./pages/home";
import InboxPage from "./pages/inbox";
import LoginPage from "./pages/login";
import MePage from "./pages/me";
import RegisterPage from "./pages/register";
import TicketsPage from "./pages/tickets";
import TicketDetailPage from "./pages/tickets/detail";
import TicketCreatePage from "./pages/tickets/new";

function hasCustomerSession(): boolean {
	const session = loadAuthSession();
	return !!session && session.clientCode === "ud-customer-web" && !!session.accessToken;
}

function mustChangePassword(): boolean {
	const session = loadAuthSession();
	return Boolean(session?.mustChangePassword);
}

function SessionBootstrap({ children }: { children: React.ReactNode }) {
	const [ready, setReady] = useState(() => {
		const session = loadAuthSession();
		return !session?.accessToken || session.accessToken.startsWith("cust-at");
	});
	const navigate = useNavigate();

	useEffect(() => {
		setUnauthorizedHandler(() => {
			navigate("/login", { replace: true });
		});
		return () => setUnauthorizedHandler(null);
	}, [navigate]);

	useEffect(() => {
		let cancelled = false;
		(async () => {
			try {
				await restoreCustomerPortalLive();
			}
			catch {
				// keep mock/local snapshot if restore fails
			}
			finally {
				if (!cancelled) {
					setReady(true);
				}
			}
		})();
		return () => {
			cancelled = true;
		};
	}, []);

	if (!ready) {
		return (
			<div className="ud-stage" style={{ display: "grid", placeItems: "center", minHeight: "100vh" }}>
				<p className="ud-muted">正在恢复会话…</p>
			</div>
		);
	}
	return <>{children}</>;
}

function RequireSession() {
	const location = useLocation();
	if (!hasCustomerSession()) {
		return <Navigate to="/login" replace />;
	}
	if (mustChangePassword() && location.pathname !== "/change-password") {
		return <Navigate to="/change-password" replace />;
	}
	return <Outlet />;
}

function RequireDomain() {
	const snapshot = getCustomerPortalSnapshot();
	if (!snapshot.activeDomain) {
		return <Navigate to="/domains" replace />;
	}
	return <Outlet />;
}

function LandingRedirect() {
	const location = useLocation();
	if (!hasCustomerSession()) {
		return <Navigate to="/login" replace state={{ from: location.pathname }} />;
	}
	if (mustChangePassword()) {
		return <Navigate to="/change-password" replace />;
	}
	const snapshot = getCustomerPortalSnapshot();
	if (!snapshot.activeDomain) {
		return <Navigate to="/domains" replace />;
	}
	return <Navigate to="/home" replace />;
}

export default function App() {
	return (
		<SessionBootstrap>
			<Routes>
				<Route path="/login" element={<LoginPage />} />
				<Route path="/register" element={<RegisterPage />} />
				<Route path="/d/:domainCode/login" element={<LoginPage />} />
				<Route path="/d/:domainCode/register" element={<RegisterPage />} />

				<Route element={<RequireSession />}>
					<Route path="/change-password" element={<ChangePasswordPage />} />
					<Route element={<AppShell />}>
						<Route path="/domains" element={<DomainsPage />} />
						<Route path="/me" element={<MePage />} />
						<Route element={<RequireDomain />}>
							<Route path="/home" element={<HomePage />} />
							<Route path="/workspace" element={<Navigate to="/home" replace />} />
							<Route path="/tickets" element={<TicketsPage />} />
							<Route path="/tickets/new" element={<TicketCreatePage />} />
							<Route path="/tickets/:ticketId" element={<TicketDetailPage />} />
							<Route path="/chat" element={<ChatPage />} />
							<Route path="/inbox" element={<InboxPage />} />
						</Route>
					</Route>
				</Route>

				<Route path="/" element={<LandingRedirect />} />
				<Route path="*" element={<Navigate to="/" replace />} />
			</Routes>
		</SessionBootstrap>
	);
}
