import type { ReactNode } from "react";

import { AuthGuarded } from "#src/components/auth-guarded";
import { BasicContent } from "#src/components/basic-content";
import {
	PLATFORM_TICKET_CONFIG_ATTR_READ,
	PLATFORM_TICKET_CONFIG_TYPE_READ,
} from "#src/pages/platform/domains/platform-domain-permissions";

import { TicketConfigSider } from "./components/ticket-config-sider";

import styles from "./index.module.less";

interface TicketConfigShellProps {
	children: ReactNode;
}

export function TicketConfigShell({ children }: TicketConfigShellProps) {
	return (
		<BasicContent className="h-full overflow-auto bg-colorBgLayout">
			<AuthGuarded
				auth={[PLATFORM_TICKET_CONFIG_ATTR_READ, PLATFORM_TICKET_CONFIG_TYPE_READ]}
				fallback={<div className="py-16 text-center text-colorTextSecondary">无权限查看事项配置</div>}
			>
				<div className={styles.detailShell}>
					<div className={styles.detailBody}>
						<TicketConfigSider />
						<div className={styles.contentCard}>
							<div className={styles.contentScroll}>
								{children}
							</div>
						</div>
					</div>
				</div>
			</AuthGuarded>
		</BasicContent>
	);
}
