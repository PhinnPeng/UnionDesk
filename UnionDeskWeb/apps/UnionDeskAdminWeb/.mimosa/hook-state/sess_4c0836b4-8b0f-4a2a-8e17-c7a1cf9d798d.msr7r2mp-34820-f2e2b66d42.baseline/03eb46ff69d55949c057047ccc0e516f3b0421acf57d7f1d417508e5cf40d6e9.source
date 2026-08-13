import type {
	AdditionalAttributeItem,
	TicketAttribute,
	TicketAttributeSlot,
} from "@uniondesk/shared";

import { Button, Space } from "antd";
import { DeleteOutlined, PlusOutlined } from "@ant-design/icons";
import { useState } from "react";

import {
	removeAdditionalAttribute,
	upsertAdditionalAttribute,
} from "./additional-attribute-utils";
import {
	StepRuleAdditionalAttributeForm,
	draftFromItem,
	itemFromDraft,
	type AdditionalAttributeDraft,
} from "./step-rule-additional-attribute-form";

interface StepRuleAdditionalAttributeListEditorProps {
	value: AdditionalAttributeItem[];
	slots: Array<TicketAttributeSlot & { dragId: string }>;
	availableAttributes: TicketAttribute[];
	onChange: (items: AdditionalAttributeItem[]) => void;
}

export function StepRuleAdditionalAttributeListEditor({
	value,
	slots,
	availableAttributes,
	onChange,
}: StepRuleAdditionalAttributeListEditorProps) {
	const [editing, setEditing] = useState<AdditionalAttributeDraft | null>(null);
	const [editingSlotId, setEditingSlotId] = useState<string | null>(null);

	const slotName = (slotId: string) => {
		const slot = slots.find(s => s.id === slotId);
		return slot?.attribute?.name ?? slot?.slot_config?.display_name ?? slotId;
	};

	const startAdd = () => {
		setEditingSlotId(null);
		setEditing(draftFromItem(null));
	};

	const startEdit = (item: AdditionalAttributeItem) => {
		setEditingSlotId(item.slot_id);
		setEditing(draftFromItem(item));
	};

	const commitEditing = () => {
		if (!editing) return;
		const item = itemFromDraft(editing);
		if (!item) return;
		const slot = slots.find(s => s.id === item.slot_id);
		const forced = slot?.slot_config?.required === true;
		const normalized = { ...item, required: forced ? true : item.required };
		let next = value;
		if (editingSlotId && editingSlotId !== normalized.slot_id) {
			next = removeAdditionalAttribute(next, editingSlotId);
		}
		onChange(upsertAdditionalAttribute(next, normalized));
		setEditing(null);
		setEditingSlotId(null);
	};

	const excludeSlotIds = value
		.map(item => item.slot_id)
		.filter(id => id !== editing?.slot_id);

	return (
		<div className="step-rule-editor">
			{value.length > 0 && (
				<div className="step-settings__rule-list" style={{ marginBottom: 12 }}>
					{value.map(item => (
						<div key={item.slot_id} className="step-settings__rule-card">
							<div className="step-settings__rule-card-header" onClick={() => startEdit(item)}>
								<div>
									<p className="step-settings__rule-card-title">{slotName(item.slot_id)}</p>
									<p className="step-settings__rule-card-summary">
										{item.required ? "必填" : "非必填"}
										{" · "}
										{item.default_mode === "set" ? "更改默认值" : "保持原有值"}
									</p>
								</div>
								<Space size={0} onClick={e => e.stopPropagation()}>
									<Button
										type="text"
										size="small"
										danger
										icon={<DeleteOutlined />}
										onClick={() => onChange(removeAdditionalAttribute(value, item.slot_id))}
									/>
								</Space>
							</div>
						</div>
					))}
				</div>
			)}

			{editing ? (
				<div className="step-settings__rule-card" style={{ padding: 12 }}>
					<StepRuleAdditionalAttributeForm
						value={editing}
						slots={slots}
						availableAttributes={availableAttributes}
						excludeSlotIds={excludeSlotIds}
						onChange={patch => setEditing(prev => (prev ? { ...prev, ...patch } : prev))}
					/>
					<Space style={{ marginTop: 12 }}>
						<Button type="primary" size="small" onClick={commitEditing} disabled={!editing.slot_id}>
							确定
						</Button>
						<Button
							size="small"
							onClick={() => {
								setEditing(null);
								setEditingSlotId(null);
							}}
						>
							取消
						</Button>
					</Space>
				</div>
			) : (
				<Button type="dashed" size="small" icon={<PlusOutlined />} block onClick={startAdd}>
					添加附加属性
				</Button>
			)}
		</div>
	);
}
