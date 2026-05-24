import type { P0VisibilityPolicyCode } from "@uniondesk/shared";

import { Checkbox, Form } from "antd";
import type { FormInstance } from "antd";

import { visibilityOptions } from "../constants";
import { applyVisibilityPolicyChange } from "../domain-utils";

interface VisibilityPolicyFieldProps {
	form: FormInstance;
	name?: string;
	label?: string;
}

/** 可见策略多选（public 与其余互斥） */
export function VisibilityPolicyField({
	form,
	name = "visibility_policy_codes",
	label = "可见策略",
}: VisibilityPolicyFieldProps) {
	return (
		<Form.Item
			name={name}
			label={label}
			rules={[{ required: true, message: "请选择可见策略" }]}
		>
			<Form.Item noStyle shouldUpdate>
				{() => {
					const current = (form.getFieldValue(name) as P0VisibilityPolicyCode[] | undefined) ?? ["public"];
					return (
						<Checkbox.Group
							value={current}
							onChange={(values) => {
								const next = applyVisibilityPolicyChange(
									current,
									values as P0VisibilityPolicyCode[],
								);
								form.setFieldValue(name, next);
							}}
						>
							{visibilityOptions.map(option => (
								<Checkbox key={option.value} value={option.value}>
									{option.label}
								</Checkbox>
							))}
						</Checkbox.Group>
					);
				}}
			</Form.Item>
		</Form.Item>
	);
}
