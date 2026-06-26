import type { TreeNode } from "@designable/core";
import { transformToSchema } from "@designable/formily-transformer";
import {
	ArrayCards,
	ArrayTable,
	Cascader,
	Checkbox,
	DatePicker,
	Form,
	FormCollapse,
	FormGrid,
	FormItem,
	FormLayout,
	FormTab,
	Input,
	NumberPicker,
	Password,
	PreviewText,
	Radio,
	Reset,
	Select,
	Space,
	Submit,
	Switch,
	TimePicker,
	Transfer,
	TreeSelect,
	Upload,
} from "@formily/antd-v5";
import { createForm } from "@formily/core";
import { createSchemaField } from "@formily/react";
import { Card, Rate, Slider } from "antd";
import { createElement, useMemo } from "react";

const Text: React.FC<{
	value?: string
	content?: string
	mode?: "normal" | "h1" | "h2" | "h3" | "p"
}> = ({ value, mode, content, ...props }) => {
	const tagName = mode === "normal" || !mode ? "div" : mode;
	return createElement(tagName, props, value || content);
};

const SchemaField = createSchemaField({
	components: {
		Space,
		FormGrid,
		FormLayout,
		FormTab,
		FormCollapse,
		ArrayTable,
		ArrayCards,
		FormItem,
		DatePicker,
		Checkbox,
		Cascader,
		Input,
		Text,
		NumberPicker,
		Switch,
		Password,
		PreviewText,
		Radio,
		Reset,
		Select,
		Submit,
		TimePicker,
		Transfer,
		TreeSelect,
		Upload,
		Card,
		Slider,
		Rate,
	},
});

export interface PreviewWidgetProps {
	tree: TreeNode
}

export function PreviewWidget({ tree }: PreviewWidgetProps) {
	const form = useMemo(() => createForm(), []);
	const { form: formProps, schema } = transformToSchema(tree);

	return (
		<Form {...formProps} form={form}>
			<SchemaField schema={schema} />
		</Form>
	);
}
