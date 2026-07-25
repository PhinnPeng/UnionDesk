import type {
	TicketStatusFlowState,
	TransitionRule,
} from "@uniondesk/shared";

import { Button, Modal, Table, Tag } from "antd";

interface RuleConfigModalProps {
	open: boolean;
	onCancel: () => void;
	rules: TransitionRule[];
	states: TicketStatusFlowState[];
	onEditRule: (rule: TransitionRule) => void;
}

export function RuleConfigModal({
	open,
	onCancel,
	rules,
	states,
	onEditRule,
}: RuleConfigModalProps) {
	const getStateName = (code: string) => {
		if (code === "*") return "任何状态";
		return states.find(s => s.code === code)?.name ?? code;
	};

	return (
		<Modal
			title="配置规则"
			open={open}
			onCancel={onCancel}
			footer={null}
			width={900}
		>
			<Table
				dataSource={rules}
				rowKey={r => `${r.from_state_code}-${r.to_state_code}`}
				pagination={false}
				size="small"
				columns={[
					{
						title: "步骤",
						render: (_, record: TransitionRule) => (
							<span>
								{getStateName(record.from_state_code)}
								<span style={{ margin: "0 8px" }}>→</span>
								{getStateName(record.to_state_code)}
							</span>
						),
					},
					{
						title: "步骤名称",
						dataIndex: "step_name",
					},
					{
						title: "权限",
						render: (_, record: TransitionRule) => (
							<span>
								{record.permission_mode === "none" ? "全部成员" : (
									<Tag color="blue">
										{record.member_ids.length + record.role_ids.length} 项限制
									</Tag>
								)}
							</span>
						),
					},
					{
						title: "必填属性",
						render: (_, record: TransitionRule) => (
							<span>
								{record.required_slot_ids.length === 0 ? "—" : (
									<Tag color="orange">{record.required_slot_ids.length} 项</Tag>
								)}
							</span>
						),
					},
					{
						title: "属性变更",
						render: (_, record: TransitionRule) => (
							<span>
								{record.attribute_updates.length === 0 ? "—" : (
									<Tag color="green">{record.attribute_updates.length} 项</Tag>
								)}
							</span>
						),
					},
					{
						title: "操作",
						render: (_, record: TransitionRule) => (
							<Button type="link" size="small" onClick={() => onEditRule(record)}>
								编辑
							</Button>
						),
					},
				]}
			/>
		</Modal>
	);
}
