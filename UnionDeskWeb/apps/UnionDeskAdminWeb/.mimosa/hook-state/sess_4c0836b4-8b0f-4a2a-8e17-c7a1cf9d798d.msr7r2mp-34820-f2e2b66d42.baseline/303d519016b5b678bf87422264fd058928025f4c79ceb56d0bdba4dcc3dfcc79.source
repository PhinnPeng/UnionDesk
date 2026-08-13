import type { TicketStatusFlowState } from "@uniondesk/shared";

import { App, Input, Modal, Select, Space } from "antd";
import { useEffect, useState } from "react";

interface AddStepModalProps {
	open: boolean;
	states: TicketStatusFlowState[];
	existingTransitions: { from: string; to: string }[];
	initialFromCode?: string;
	initialToCode?: string;
	onCancel: () => void;
	onOk: (fromCode: string, toCode: string, stepName: string) => void;
}

export function AddStepModal({
	open,
	states,
	existingTransitions,
	initialFromCode,
	initialToCode,
	onCancel,
	onOk,
}: AddStepModalProps) {
	const { message } = App.useApp();
	const [fromCode, setFromCode] = useState<string>();
	const [toCode, setToCode] = useState<string>();
	const [stepName, setStepName] = useState("");
	const [confirmLoading, setConfirmLoading] = useState(false);

	useEffect(() => {
		if (!open) return;
		setFromCode(initialFromCode);
		setToCode(initialToCode);
		if (initialToCode) {
			const targetState = states.find(s => s.code === initialToCode);
			setStepName(targetState?.name ?? "");
		} else {
			setStepName("");
		}
	}, [open, initialFromCode, initialToCode, states]);

	// 过滤已存在的转换
	const availableToCodes = states
		.filter(s => s.code !== fromCode)
		.filter(s => !existingTransitions.some(t => t.from === fromCode && t.to === s.code));

	const fromOptions = [
		{ value: "*", label: "任何状态" },
		...states.map(s => ({
			value: s.code,
			label: s.name,
		})),
	];

	const resetForm = () => {
		setFromCode(undefined);
		setToCode(undefined);
		setStepName("");
	};

	const handleOk = () => {
		if (!fromCode) {
			message.error("请选择开始状态");
			return;
		}
		if (!toCode) {
			message.error("请选择目标状态");
			return;
		}
		const trimmedStepName = stepName.trim();
		if (!trimmedStepName) {
			message.error("请输入步骤名称");
			return;
		}
		setConfirmLoading(true);
		onOk(fromCode, toCode, trimmedStepName);
		setConfirmLoading(false);
		resetForm();
	};

	const handleCancel = () => {
		resetForm();
		onCancel();
	};

	return (
		<Modal
			title="创建步骤"
			open={open}
			onOk={handleOk}
			onCancel={handleCancel}
			confirmLoading={confirmLoading}
			okButtonProps={{ disabled: !fromCode || !toCode || !stepName.trim() }}
		>
			<Space direction="vertical" className="w-full" size="large">
				<div>
					<div className="mb-2">开始状态</div>
					<Select
						placeholder="请选择开始状态"
						value={fromCode}
						onChange={(value) => {
							setFromCode(value);
							setToCode(undefined);
						}}
						options={fromOptions}
						className="w-full"
					/>
				</div>
				<div>
					<div className="mb-2">目标状态</div>
					<Select
						placeholder="请选择目标状态"
						value={toCode}
						onChange={setToCode}
						options={availableToCodes.map(s => ({
							value: s.code,
							label: s.name,
						}))}
						disabled={!fromCode}
						className="w-full"
					/>
					{fromCode && availableToCodes.length === 0 && (
						<div className="mt-2 text-sm text-gray-500">
							该状态已连接到所有其他状态
						</div>
					)}
				</div>
				<div>
					<div className="mb-2">
						<span className="text-red-500">*</span>
						{" "}
						步骤名称
					</div>
					<Input
						placeholder="请输入步骤名称"
						value={stepName}
						onChange={e => setStepName(e.target.value)}
						maxLength={50}
					/>
				</div>
			</Space>
		</Modal>
	);
}
