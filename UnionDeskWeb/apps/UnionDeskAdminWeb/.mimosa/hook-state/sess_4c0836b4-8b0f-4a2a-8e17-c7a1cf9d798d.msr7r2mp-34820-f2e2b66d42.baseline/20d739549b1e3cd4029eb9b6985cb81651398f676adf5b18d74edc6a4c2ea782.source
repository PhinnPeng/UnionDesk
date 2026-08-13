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
	/** 传入时覆盖默认的事项配置一级侧栏（如团队模板配置副菜单） */
	sider?: ReactNode;
	/** 覆盖默认权限校验；默认要求属性或类型读权限 */
	auth?: string | string[];
}

export function TicketConfigShell({
	children,
	sider,
	auth = [PLATFORM_TICKET_CONFIG_ATTR_READ, PLATFORM_TICKET_CONFIG_TYPE_READ],
}: TicketConfigShellProps) {
	return (
		<BasicContent className="h-full overflow-auto bg-colorBgLayout">
			<AuthGuarded
				auth={auth}
				fallback={<div className="py-16 text-center text-colorTextSecondary">无权限查看事项配置</div>}
			>
				<div className={styles.detailShell}>
					<div className={styles.detailBody}>
						{sider ?? <TicketConfigSider />}
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
