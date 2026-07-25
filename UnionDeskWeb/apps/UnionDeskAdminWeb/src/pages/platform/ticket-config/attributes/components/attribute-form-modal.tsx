import type {
	CreateTicketAttributeBody,
	TicketAttribute,
	UpdateTicketAttributeBody,
} from "@uniondesk/shared";

import { AttributeMenuOptionsEditor } from "./attribute-menu-options-editor";
import { AttributeTypePicker } from "./attribute-type-picker";
import type { AttributeTypeKey } from "./attribute-utils";
import {
	formatAttributeTypeLabel,
	getAttributeTypeLabel,
	isMenuAttributeType,
	isNumberAttributeType,
	isPriorityLevelsAttribute,
	resolveAttributeTypeKey,
	toFieldTypePayload,
} from "./attribute-utils";

import { Alert, Button, Form, Input, Modal, Typography } from "antd";
import { useEffect, useState } from "react";

import "./attribute-form-modal.less";

const STANDARD_PRIORITY_LEVELS = [
	{ code: "urgent", name: "紧急", color: "#f5222d" },
	{ code: "high", name: "高", color: "#fa8c16" },
	{ code: "normal", name: "中", color: "#1677ff" },
	{ code: "low", name: "低", color: "#8c8c8c" },
] as const;

export interface AttributeFormModalProps {
	open: boolean;
	editing: TicketAttribute | null;
	submitting: boolean;
	onCancel: () => void;
	onSubmit: (body: CreateTicketAttributeBody | UpdateTicketAttributeBody) => Promise<void>;
}

function isMemberAttribute(attribute: TicketAttribute | null): boolean {
	return attribute?.field_type === "member";
}

export function AttributeFormModal({
	open,
	editing,
	submitting,
	onCancel,
	onSubmit,
}: AttributeFormModalProps) {
	const [step, setStep] = useState<1 | 2>(1);
	const [selectedTypeKey, setSelectedTypeKey] = useState<AttributeTypeKey>("single_select");
	const [form] = Form.useForm<{
		name: string;
		description: string;
		options?: { label: string; value: string; color?: string }[];
		unit?: string;
		status?: string;
	}>();

	const editingPriorityLevels = isPriorityLevelsAttribute(editing);
	const editingMember = isMemberAttribute(editing);
	const preserveTypeConfig = editingPriorityLevels || editingMember;

	useEffect(() => {
		if (!open) {
			return;
		}
		if (editing) {
			const typeKey = resolveAttributeTypeKey(editing.field_type, editing.type_config);
			if (typeKey !== "switch" && typeKey !== "email_input" && typeKey !== "phone_input") {
				setSelectedTypeKey(typeKey);
			}
			setStep(2);
			form.setFieldsValue({
				name: editing.name,
				description: editing.description,
				options: editing.type_config?.options ?? [],
				unit: editing.type_config?.unit ?? "",
				status: editing.status,
			});
			return;
		}
		setStep(1);
		setSelectedTypeKey("single_select");
		form.resetFields();
		form.setFieldsValue({
			options: [],
			unit: "",
		});
	}, [editing, form, open]);

	const handleCancel = () => {
		setStep(1);
		onCancel();
	};

	const handleNext = () => {
		setStep(2);
	};

	const handleBack = () => {
		setStep(1);
	};

	const handleOk = async () => {
		if (!editing && step === 1) {
			handleNext();
			return;
		}
		const values = await form.validateFields();
		let fieldType: CreateTicketAttributeBody["field_type"];
		let typeConfig: CreateTicketAttributeBody["type_config"];
		if (editing && preserveTypeConfig) {
			// 系统优先级 / 成员类型：只改名称描述，禁止把 options_source 或 member 配置冲掉
			fieldType = editing.field_type;
			typeConfig = editing.type_config ?? {};
		}
		else if (editing) {
			const legacyKey = resolveAttributeTypeKey(editing.field_type, editing.type_config);
			if (legacyKey === "switch") {
				fieldType = editing.field_type;
				typeConfig = editing.type_config ?? {};
			}
			else if (legacyKey === "email_input" || legacyKey === "phone_input") {
				fieldType = editing.field_type;
				typeConfig = editing.type_config ?? {};
			}
			else {
				const payload = toFieldTypePayload(selectedTypeKey, {
					options: values.options,
					unit: values.unit,
				});
				fieldType = payload.field_type;
				typeConfig = payload.type_config;
			}
		}
		else {
			const payload = toFieldTypePayload(selectedTypeKey, {
				options: values.options,
				unit: values.unit,
			});
			fieldType = payload.field_type;
			typeConfig = payload.type_config;
		}
		const trimmedDescription = values.description?.trim();
		const body = {
			name: values.name.trim(),
			...(editing
				? { description: trimmedDescription ?? "" }
				: trimmedDescription
					? { description: trimmedDescription }
					: {}),
			field_type: fieldType,
			type_config: typeConfig,
			...(editing ? { status: values.status } : {}),
		};
		await onSubmit(body);
	};

	const modalTitle = editing
		? `编辑事项属性：${formatAttributeTypeLabel(editing)}`
		: step === 1
			? "请选择属性类型"
			: `配置属性：${getAttributeTypeLabel(selectedTypeKey)}`;

	const activeTypeKey = editing
		? resolveAttributeTypeKey(editing.field_type, editing.type_config)
		: selectedTypeKey;
	const showMenuOptions = editing
		? !preserveTypeConfig && (activeTypeKey === "single_select" || activeTypeKey === "multi_select")
		: isMenuAttributeType(selectedTypeKey);
	const showUnitField = editing
		? !preserveTypeConfig && (activeTypeKey === "integer" || activeTypeKey === "decimal")
		: isNumberAttributeType(selectedTypeKey);

	const footer = editing
		? undefined
		: step === 1
			? [
				<Button key="cancel" onClick={handleCancel}>取消</Button>,
				<Button key="next" type="primary" onClick={handleNext}>下一步</Button>,
			]
			: [
				<Button key="cancel" onClick={handleCancel}>取消</Button>,
				<Button key="back" onClick={handleBack}>上一步</Button>,
				<Button key="submit" type="primary" loading={submitting} onClick={() => void handleOk()}>创建</Button>,
			];

	return (
		<Modal
			className="attribute-form-modal"
			title={modalTitle}
			open={open}
			centered
			confirmLoading={submitting}
			okText={editing ? "保存" : undefined}
			cancelText={editing ? "取消" : undefined}
			destroyOnHidden
			width={640}
			classNames={{ body: "attribute-form-modal__body" }}
			footer={footer}
			onCancel={handleCancel}
			onOk={editing ? () => void handleOk() : undefined}
		>
			{!editing && step === 1 ? (
				<AttributeTypePicker value={selectedTypeKey} onChange={setSelectedTypeKey} />
			) : (
				<Form form={form} layout="vertical">
					{editing ? (
						<Form.Item label="属性类型">
							<Input value={formatAttributeTypeLabel(editing)} disabled />
						</Form.Item>
					) : null}
					<Form.Item name="name" label="属性名称" rules={[{ required: true, message: "请输入属性名称" }]}>
						<Input maxLength={128} placeholder="属性名称" disabled={!!(editing?.is_system && editing?.scope === "platform")} />
					</Form.Item>
					<Form.Item name="description" label="描述">
						<Input.TextArea maxLength={500} rows={2} showCount placeholder="对该属性的简要描述（可选）" />
					</Form.Item>
					{editingPriorityLevels ? (
						<>
							<Alert
								type="info"
								showIcon
								style={{ marginBottom: 12 }}
								message="选项来自业务域标准优先级档位"
								description="本属性不在字典内维护菜单选项；工单表单发布时按所属域的 ticket_priority_level 注入。默认标准四档如下（可在域配置中调整名称/颜色/图标）。"
							/>
							<div className="attribute-form-modal__priority-levels">
								{STANDARD_PRIORITY_LEVELS.map(level => (
									<div key={level.code} className="attribute-form-modal__priority-row">
										<span
											className="attribute-form-modal__priority-swatch"
											style={{ background: level.color }}
										/>
										<Typography.Text>
											{level.name}
											<Typography.Text type="secondary">（{level.code}）</Typography.Text>
										</Typography.Text>
									</div>
								))}
							</div>
						</>
					) : null}
					{editingMember && editing ? (
						<Alert
							type="info"
							showIcon
							style={{ marginBottom: 12 }}
							message={editing.type_config?.multiple ? "成员（多选）" : "成员（单选）"}
							description={`人选范围由 scope_mode=${editing.type_config?.scope_mode ?? "auto"} 决定：域类型走业务域成员，平台类型走平台员工池。此处不维护人员名单。`}
						/>
					) : null}
					{showMenuOptions ? (
						<Form.Item
							name="options"
							label="菜单选项"
							rules={[
								{
									validator: async (_, options: { label: string; value: string; color?: string }[] | undefined) => {
										if (!options?.length) {
											throw new Error("请至少添加一个菜单选项");
										}
									},
								},
							]}
						>
							<AttributeMenuOptionsEditor />
						</Form.Item>
					) : null}
					{showUnitField ? (
						<Form.Item name="unit" label="单位">
							<Input maxLength={32} placeholder="可选，如：元、kg" />
						</Form.Item>
					) : null}
				</Form>
			)}
		</Modal>
	);
}
