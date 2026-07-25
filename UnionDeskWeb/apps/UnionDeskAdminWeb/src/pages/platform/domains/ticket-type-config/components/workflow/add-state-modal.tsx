import type { TicketStatusDefinition } from "@uniondesk/shared";

import { App, Modal, Select, Space, Switch } from "antd";
import { useState } from "react";

interface AddStateModalProps {
	open: boolean;
	platformStatuses: TicketStatusDefinition[];
	existingStateCodes: string[];
	onCancel: () => void;
	onOk: (status: TicketStatusDefinition, addGlobalTransitions: boolean) => void;
}

export function AddStateModal({
	open,
	platformStatuses,
	existingStateCodes,
	onCancel,
	onOk,
}: AddStateModalProps) {
	const { message } = App.useApp();
	const [selectedStatusId, setSelectedStatusId] = useState<string>();
	const [addGlobalTransitions, setAddGlobalTransitions] = useState(false);
	const [confirmLoading, setConfirmLoading] = useState(false);

	const availableStatuses = platformStatuses.filter(
		s => s.status === "active" && !existingStateCodes.includes(s.code)
	);

	const handleOk = () => {
		const status = platformStatuses.find(s => s.id === selectedStatusId);
		if (!status) {
			message.error("请选择状态");
			return;
		}
		setConfirmLoading(true);
		onOk(status, addGlobalTransitions);
		setConfirmLoading(false);
		setSelectedStatusId(undefined);
		setAddGlobalTransitions(false);
	};

	const handleCancel = () => {
		setSelectedStatusId(undefined);
		setAddGlobalTransitions(false);
		onCancel();
	};

	return (
		<Modal
			title="添加状态"
			open={open}
			onOk={handleOk}
			onCancel={handleCancel}
			confirmLoading={confirmLoading}
			okButtonProps={{ disabled: !selectedStatusId }}
		>
			<Space direction="vertical" className="w-full" size="large">
				<div>
					<div className="mb-2">事项状态</div>
					<Select
						placeholder="请选择状态"
						value={selectedStatusId}
						onChange={setSelectedStatusId}
						options={availableStatuses.map(s => ({
							value: s.id,
							label: s.name,
						}))}
						className="w-full"
					/>
					{availableStatuses.length === 0 && (
						<div className="mt-2 text-sm text-gray-500">
							没有可用的状态，
							<a href="/platform/ticket-config/statuses" target="_blank" rel="noreferrer">
								前往新建事项状态
							</a>
						</div>
					)}
				</div>
				<div>
					<Space>
						<Switch
							checked={addGlobalTransitions}
							onChange={setAddGlobalTransitions}
						/>
						<span>任何状态可转换到该状态</span>
					</Space>
					<div className="mt-1 text-xs text-gray-500">
						开启后，将自动为所有已有状态创建到该状态的转换
					</div>
				</div>
			</Space>
		</Modal>
	);
}
