import type { TicketRow } from "#src/api/platform/ticket";

import { Drawer, Modal } from "antd";
import { useCallback, useState } from "react";

import { TicketDetailContent } from "./ticket-detail-content";

/** 详情形态记忆 key：drawer | modal（本地记住用户操作） */
const DETAIL_MODE_STORAGE_KEY = "uniondesk.ticketDetail.mode";

interface TicketDetailDrawerProps {
	domainId: string
	ticketId: string | null
	open: boolean
	onClose: () => void
	/** 当前列表行（上一条/下一条导航） */
	tickets: TicketRow[]
	/** 分配（复用父级分配弹窗） */
	onAssign?: (row: TicketRow) => void
	/** 操作成功后父级刷新列表 */
	onChanged?: () => void
}

/**
 * 工单详情容器：抽屉（默认）与全屏 Modal 双形态，形态选择本地记忆；
 * 内容统一由 TicketDetailContent 承载（头部/操作栏/描述/活动日志/底部输入）。
 */
export function TicketDetailDrawer({
	domainId,
	ticketId,
	open,
	onClose,
	tickets,
	onAssign,
	onChanged,
}: TicketDetailDrawerProps) {
	const [fullscreen, setFullscreen] = useState(
		() => localStorage.getItem(DETAIL_MODE_STORAGE_KEY) === "modal",
	);

	const handleToggleFullscreen = useCallback(() => {
		setFullscreen(prev => {
			const next = !prev;
			localStorage.setItem(DETAIL_MODE_STORAGE_KEY, next ? "modal" : "drawer");
			return next;
		});
	}, []);

	const content = (
		<TicketDetailContent
			domainId={domainId}
			ticketId={ticketId}
			tickets={tickets}
			onClose={onClose}
			onChanged={onChanged}
			onAssign={onAssign}
			onToggleFullscreen={handleToggleFullscreen}
			isFullscreen={fullscreen}
		/>
	);

	if (fullscreen) {
		return (
			<Modal
				open={open}
				onCancel={onClose}
				width="100vw"
				style={{ top: 0, maxWidth: "100vw", padding: 0, margin: 0 }}
				styles={{
					body: { padding: 0, height: "100vh" },
					container: { height: "100vh", padding: 0 },
					root: { padding: 0, maxWidth: "100vw" },
				}}
				footer={null}
				title={null}
				closable={false}
			>
				{content}
			</Modal>
		);
	}

	return (
		<Drawer
			open={open}
			onClose={onClose}
			width={900}
			title={null}
			// closable 默认 true 会在 title=null 时仍渲染空头部（带底边框线），关闭按钮不需要（遮罩/ESC/更多-关闭可关）
			closable={false}
			styles={{ body: { padding: 0 } }}
		>
			{content}
		</Drawer>
	);
}
