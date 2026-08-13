import type { TicketStatusFlowState } from "@uniondesk/shared";

/** 状态类型样式：走 Ant Design Token，适配深/浅色 */
const STATE_TYPE_STYLE: Record<string, { color: string; background: string }> = {
	in_progress: {
		color: "var(--ant-color-primary)",
		background: "var(--ant-color-primary-bg)",
	},
	paused: {
		color: "var(--ant-color-warning)",
		background: "var(--ant-color-warning-bg)",
	},
	terminal: {
		color: "var(--ant-color-success)",
		background: "var(--ant-color-success-bg)",
	},
};

interface StatusLabelProps {
	state: TicketStatusFlowState;
	size?: "small" | "default";
}

/** 状态标签，颜色体系对齐主题 Token */
export function StatusLabel({ state, size = "default" }: StatusLabelProps) {
	const style = STATE_TYPE_STYLE[state.state_type] ?? STATE_TYPE_STYLE.in_progress;
	const padding = size === "small" ? "2px 6px" : "3px 8px";
	const fontSize = size === "small" ? "12px" : "13px";

	return (
		<span
			style={{
				display: "inline-block",
				color: style.color,
				background: style.background,
				borderRadius: "3px",
				padding,
				fontSize,
				lineHeight: "18px",
				whiteSpace: "nowrap",
			}}
		>
			{state.name}
		</span>
	);
}

/** 通过 code 查找状态后渲染标签 */
export function StatusLabelByCode({
	code,
	states,
	size,
}: {
	code: string;
	states: TicketStatusFlowState[];
	size?: "small" | "default";
}) {
	const state = states.find(s => s.code === code);
	if (!state) return <span>{code}</span>;
	return <StatusLabel state={state} size={size} />;
}
