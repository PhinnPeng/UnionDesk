import type { TransitionRule } from "@uniondesk/shared";

import { Form, Select } from "antd";

export type PermissionDraft = Pick<TransitionRule, "permission_mode" | "member_ids" | "role_ids">;

interface StepRulePermissionEditorProps {
	value: PermissionDraft;
	disabled?: boolean;
	onChange: (patch: Partial<PermissionDraft>) => void;
}

export function StepRulePermissionEditor({
	value,
	disabled = false,
	onChange,
}: StepRulePermissionEditorProps) {
	return (
		<div className="step-rule-editor">
			<Form.Item label="权限模式" required style={{ marginBottom: 12 }}>
				<Select
					value={value.permission_mode}
					disabled={disabled}
					onChange={(permission_mode) => {
						onChange({
							permission_mode,
							member_ids: permission_mode === "members" ? value.member_ids : [],
							role_ids: permission_mode === "roles" ? value.role_ids : [],
						});
					}}
					options={[
						{ value: "none", label: "全部成员" },
						{ value: "members", label: "指定成员" },
						{ value: "roles", label: "指定角色" },
					]}
				/>
			</Form.Item>
			{value.permission_mode === "members" && (
				<Form.Item label="指定成员" style={{ marginBottom: 0 }}>
					<Select
						mode="multiple"
						placeholder="选择成员"
						value={value.member_ids}
						options={[]}
						disabled={disabled}
						onChange={member_ids => onChange({ member_ids })}
					/>
				</Form.Item>
			)}
			{value.permission_mode === "roles" && (
				<Form.Item label="指定角色" style={{ marginBottom: 0 }}>
					<Select
						mode="multiple"
						placeholder="选择角色"
						value={value.role_ids}
						options={[]}
						disabled={disabled}
						onChange={role_ids => onChange({ role_ids })}
					/>
				</Form.Item>
			)}
		</div>
	);
}

export function summarizePermission(value: PermissionDraft): string {
	if (value.permission_mode === "members") {
		return value.member_ids.length > 0 ? `${value.member_ids.length} 位成员` : "指定成员（未选择）";
	}
	if (value.permission_mode === "roles") {
		return value.role_ids.length > 0 ? `${value.role_ids.length} 个角色` : "指定角色（未选择）";
	}
	return "全部成员";
}

export function hasPermissionRule(rule: Pick<TransitionRule, "permission_mode" | "member_ids" | "role_ids">): boolean {
	return rule.permission_mode !== "none"
		|| (rule.member_ids?.length ?? 0) > 0
		|| (rule.role_ids?.length ?? 0) > 0;
}
