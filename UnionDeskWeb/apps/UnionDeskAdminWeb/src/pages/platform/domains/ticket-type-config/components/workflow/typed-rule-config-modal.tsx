import type {
	AdditionalAttributeItem,
	AttributeUpdateItem,
	TicketAttribute,
	TicketAttributeSlot,
	TicketStatusFlowState,
	TransitionRule,
} from "@uniondesk/shared";

import { Alert, Form, Modal, Select } from "antd";
import { useEffect, useMemo, useState } from "react";

import { MemberPicker } from "#src/pages/platform/components/member-picker";

import {
	normalizeAdditionalAttributes,
	syncRequiredSlotIds,
	upsertAdditionalAttribute,
} from "./additional-attribute-utils";
import {
	StepRuleAttributeUpdateEditor,
} from "./step-rule-attribute-update-editor";
import {
	StepRuleAdditionalAttributeForm,
	draftFromItem,
	itemFromDraft,
	type AdditionalAttributeDraft,
} from "./step-rule-additional-attribute-form";
import {
	StepRulePermissionEditor,
	type PermissionDraft,
} from "./step-rule-permission-editor";
import { StatusLabelByCode } from "./status-label";
import {
	WORKFLOW_RULE_KIND_META,
	type WorkflowRuleKind,
} from "./workflow-rule-kinds";
import "./typed-rule-config-modal.less";

export type StepKey = {
	from_state_code: string;
	to_state_code: string;
};

interface TypedRuleConfigModalProps {
	open: boolean;
	ruleKind: WorkflowRuleKind | null;
	/** 工具栏入口为 true，需选择步骤；点步骤入口为 false */
	requireStepSelect: boolean;
	/** 点步骤时预置；工具栏时可为 null */
	initialStepKey: StepKey | null;
	rules: TransitionRule[];
	states: TicketStatusFlowState[];
	slots: Array<TicketAttributeSlot & { dragId: string }>;
	availableAttributes: TicketAttribute[];
	isPlatformLevel: boolean;
	domainId?: string;
	onCancel: () => void;
	onUpdate: (fromCode: string, toCode: string, patch: Partial<TransitionRule>) => void;
}

function findAssigneeSlot(
	slots: Array<TicketAttributeSlot & { dragId: string }>,
	availableAttributes: TicketAttribute[],
): TicketAttributeSlot | null {
	for (const slot of slots) {
		const attr = availableAttributes.find(a => a.id === slot.attribute_id)
			?? slot.attribute;
		if (attr?.system_key === "assignee" || attr?.name === "处理人") {
			return slot;
		}
	}
	return null;
}

function stepOptionLabel(
	rule: TransitionRule,
	states: TicketStatusFlowState[],
): string {
	const fromName = rule.from_state_code === "*"
		? "任何状态"
		: (states.find(s => s.code === rule.from_state_code)?.name ?? rule.from_state_code);
	const toName = states.find(s => s.code === rule.to_state_code)?.name ?? rule.to_state_code;
	return `${rule.step_name}（${fromName} → ${toName}）`;
}

function stepKeyOf(rule: TransitionRule): string {
	return `${rule.from_state_code}→${rule.to_state_code}`;
}

function parseStepKey(value: string): StepKey {
	const [from_state_code, to_state_code] = value.split("→");
	return { from_state_code, to_state_code };
}

export function TypedRuleConfigModal({
	open,
	ruleKind,
	requireStepSelect,
	initialStepKey,
	rules,
	states,
	slots,
	availableAttributes,
	isPlatformLevel,
	domainId,
	onCancel,
	onUpdate,
}: TypedRuleConfigModalProps) {
	const [stepValue, setStepValue] = useState<string | null>(null);
	const [stepError, setStepError] = useState<string | null>(null);
	const [slotError, setSlotError] = useState<string | null>(null);
	const [permission, setPermission] = useState<PermissionDraft>({
		permission_mode: "none",
		member_ids: [],
		role_ids: [],
	});
	const [additionalDraft, setAdditionalDraft] = useState<AdditionalAttributeDraft>(draftFromItem(null));
	const [attributeUpdates, setAttributeUpdates] = useState<AttributeUpdateItem[]>([]);
	const [assigneeStaffId, setAssigneeStaffId] = useState<number | null>(null);

	const selectedRule = useMemo(() => {
		if (!stepValue) return null;
		const key = parseStepKey(stepValue);
		return rules.find(
			r => r.from_state_code === key.from_state_code && r.to_state_code === key.to_state_code,
		) ?? null;
	}, [rules, stepValue]);

	const assigneeSlot = useMemo(
		() => findAssigneeSlot(slots, availableAttributes),
		[slots, availableAttributes],
	);

	const loadRuleFields = (rule: TransitionRule | null) => {
		setPermission({
			permission_mode: rule?.permission_mode === "none" || !rule
				? "members"
				: rule.permission_mode,
			member_ids: [...(rule?.member_ids ?? [])],
			role_ids: [...(rule?.role_ids ?? [])],
		});
		setAttributeUpdates([...(rule?.attribute_updates ?? [])]);
		// 附加属性：每次打开 Modal 用于「添加/编辑一条」；默认空表，若仅一项则预填便于编辑
		const items = normalizeAdditionalAttributes(rule);
		setAdditionalDraft(draftFromItem(items.length === 1 ? items[0] : null));
		const assigneeUpdate = assigneeSlot
			? rule?.attribute_updates?.find(item => item.slot_id === assigneeSlot.id)
			: null;
		const raw = assigneeUpdate?.value;
		setAssigneeStaffId(
			typeof raw === "number"
				? raw
				: typeof raw === "string" && raw.trim()
					? Number(raw)
					: null,
		);
		setSlotError(null);
	};

	useEffect(() => {
		if (!open || !ruleKind) return;

		const key = initialStepKey
			? `${initialStepKey.from_state_code}→${initialStepKey.to_state_code}`
			: null;
		setStepValue(key);
		setStepError(null);

		const rule = key
			? rules.find(r => `${r.from_state_code}→${r.to_state_code}` === key) ?? null
			: null;
		loadRuleFields(rule);
	}, [open, ruleKind, initialStepKey, rules]);

	useEffect(() => {
		if (!open || !requireStepSelect || !stepValue) return;
		const rule = rules.find(r => stepKeyOf(r) === stepValue) ?? null;
		loadRuleFields(rule);
	}, [stepValue]); // eslint-disable-line react-hooks/exhaustive-deps

	if (!ruleKind) return null;

	const meta = WORKFLOW_RULE_KIND_META[ruleKind];
	const permissionBlocked = ruleKind === "permission" && isPlatformLevel;
	const isAdditional = ruleKind === "required";
	const isAssignee = ruleKind === "assignee";

	const existingAdditional = normalizeAdditionalAttributes(selectedRule);
	const excludeSlotIds = existingAdditional
		.map(item => item.slot_id)
		.filter(id => id !== additionalDraft.slot_id);

	const handleOk = () => {
		if (permissionBlocked) return;

		let key = initialStepKey;
		if (requireStepSelect) {
			if (!stepValue) {
				setStepError("请选择当前步骤");
				return;
			}
			key = parseStepKey(stepValue);
		}
		if (!key) {
			setStepError("请选择当前步骤");
			return;
		}

		const patch: Partial<TransitionRule> = {};
		if (ruleKind === "permission") {
			patch.permission_mode = permission.permission_mode;
			patch.member_ids = permission.member_ids;
			patch.role_ids = permission.role_ids;
		}
		else if (ruleKind === "required") {
			const item = itemFromDraft(additionalDraft);
			if (!item) {
				setSlotError("请选择附加属性");
				return;
			}
			const slot = slots.find(s => s.id === item.slot_id);
			const forced = slot?.slot_config?.required === true;
			const normalized: AdditionalAttributeItem = {
				...item,
				required: forced ? true : item.required,
			};
			const base = normalizeAdditionalAttributes(
				rules.find(
					r => r.from_state_code === key!.from_state_code
						&& r.to_state_code === key!.to_state_code,
				),
			);
			const next = upsertAdditionalAttribute(base, normalized);
			patch.additional_attributes = next;
			patch.required_slot_ids = syncRequiredSlotIds(next);
		}
		else if (ruleKind === "attribute_update") {
			patch.attribute_updates = attributeUpdates;
		}
		else if (ruleKind === "assignee") {
			if (!assigneeSlot) {
				setSlotError("当前类型未挂载「处理人」系统属性插槽");
				return;
			}
			if (assigneeStaffId == null || !Number.isFinite(assigneeStaffId)) {
				setSlotError("请选择处理人");
				return;
			}
			const others = (selectedRule?.attribute_updates ?? []).filter(
				item => item.slot_id !== assigneeSlot.id,
			);
			patch.attribute_updates = [
				...others,
				{
					slot_id: assigneeSlot.id,
					value: assigneeStaffId,
					value_type: "number",
				},
			];
		}

		onUpdate(key.from_state_code, key.to_state_code, patch);
		onCancel();
	};

	const renderStepReadonly = () => {
		if (!selectedRule) return null;
		const fromCode = selectedRule.from_state_code;
		return (
			<div className="typed-rule-modal__step-readonly">
				<span style={{ marginRight: 8 }}>当前步骤</span>
				<span style={{ marginRight: 8, fontWeight: 500 }}>{selectedRule.step_name}</span>
				{fromCode === "*"
					? <span className="step-settings__any-state">任何状态</span>
					: <StatusLabelByCode code={fromCode} states={states} size="small" />}
				<span style={{ margin: "0 6px", color: "var(--ant-color-text-secondary)" }}>→</span>
				<StatusLabelByCode code={selectedRule.to_state_code} states={states} size="small" />
			</div>
		);
	};

	return (
		<Modal
			title={isAdditional ? "添加规则" : meta.label}
			open={open}
			onCancel={onCancel}
			onOk={handleOk}
			okText="确定"
			cancelText="取消"
			okButtonProps={{ disabled: permissionBlocked }}
			width={480}
			destroyOnHidden
		>
			{isAdditional ? (
				<>
					<p className="typed-rule-modal__kind-title">{meta.label}</p>
					<p className="typed-rule-modal__hint">{meta.description}</p>
				</>
			) : (
				<p className="typed-rule-modal__hint">{meta.description}</p>
			)}

			{permissionBlocked && (
				<Alert
					type="info"
					showIcon
					style={{ marginBottom: 16 }}
					message="平台级类型不支持配置步骤权限"
				/>
			)}

			{requireStepSelect ? (
				<Form.Item
					label="当前步骤"
					required
					validateStatus={stepError ? "error" : undefined}
					help={stepError ?? undefined}
				>
					<Select
						placeholder="请选择步骤"
						value={stepValue}
						onChange={(value) => {
							setStepError(null);
							setStepValue(value);
						}}
						options={rules.map(r => ({
							value: stepKeyOf(r),
							label: stepOptionLabel(r, states),
						}))}
					/>
				</Form.Item>
			) : (
				renderStepReadonly()
			)}

			{ruleKind === "permission" && !permissionBlocked && (
				<StepRulePermissionEditor
					value={permission}
					disabled={isPlatformLevel}
					onChange={patch => setPermission(prev => ({ ...prev, ...patch }))}
				/>
			)}
			{ruleKind === "required" && (
				<>
					<StepRuleAdditionalAttributeForm
						value={additionalDraft}
						slots={slots}
						availableAttributes={availableAttributes}
						excludeSlotIds={excludeSlotIds}
						onChange={(patch) => {
							setSlotError(null);
							setAdditionalDraft(prev => ({ ...prev, ...patch }));
						}}
					/>
					{slotError && (
						<div style={{ marginTop: 8, color: "var(--ant-color-error)", fontSize: 12 }}>{slotError}</div>
					)}
				</>
			)}
			{ruleKind === "attribute_update" && (
				<StepRuleAttributeUpdateEditor
					value={attributeUpdates}
					slots={slots}
					availableAttributes={availableAttributes}
					domainId={domainId}
					onChange={setAttributeUpdates}
				/>
			)}
			{isAssignee && (
				<>
					{!assigneeSlot && (
						<Alert
							type="warning"
							showIcon
							style={{ marginBottom: 12 }}
							message="请先在类型表单中挂载「处理人」系统属性"
						/>
					)}
					<Form.Item
						label="目标处理人"
						required
						validateStatus={slotError ? "error" : undefined}
						help={slotError ?? undefined}
					>
						<MemberPicker
							domainId={domainId}
							value={assigneeStaffId}
							onChange={value => {
								setSlotError(null);
								setAssigneeStaffId(typeof value === "number" ? value : null);
							}}
						/>
					</Form.Item>
				</>
			)}
		</Modal>
	);
}
