import type { TicketAttributeSlot } from "@uniondesk/shared";

import { Form, Select } from "antd";

interface StepRuleRequiredEditorProps {
	value: string[];
	slots: Array<TicketAttributeSlot & { dragId: string }>;
	onChange: (required_slot_ids: string[]) => void;
}

export function StepRuleRequiredEditor({
	value,
	slots,
	onChange,
}: StepRuleRequiredEditorProps) {
	return (
		<div className="step-rule-editor">
			<Form.Item label="必填属性" style={{ marginBottom: 0 }}>
				<Select
					mode="multiple"
					placeholder="选择必填属性"
					value={value}
					onChange={onChange}
					options={slots.map(s => ({
						value: s.id,
						label: s.attribute?.name ?? s.id,
					}))}
				/>
			</Form.Item>
		</div>
	);
}

export function summarizeRequired(count: number): string {
	return count > 0 ? `${count} 项必填` : "未选择必填属性";
}

export function hasRequiredRule(required_slot_ids: string[] | undefined): boolean {
	return (required_slot_ids?.length ?? 0) > 0;
}
