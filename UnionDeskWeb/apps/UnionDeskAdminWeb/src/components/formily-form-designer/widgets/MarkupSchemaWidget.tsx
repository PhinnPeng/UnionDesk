import type { TreeNode } from "@designable/core";
import { MonacoInput } from "@designable/react-settings-form";

export interface MarkupSchemaWidgetProps {
	tree: TreeNode
}

function isPlainObject(value: unknown): value is Record<string, unknown> {
	return typeof value === "object" && value !== null && !Array.isArray(value);
}

function isEmptyObject(value: Record<string, unknown>): boolean {
	return Object.keys(value).length === 0;
}

function transformToMarkupSchemaCode(tree: TreeNode): string {
	const printAttribute = (node: TreeNode): string => {
		const props = { ...(node.props ?? {}) };
		if (node.depth !== 0) {
			props.name = node.props?.name ?? node.id;
		}
		return Object.keys(props)
			.map((key) => {
				if (
					key === "x-designable-id"
					|| key === "x-designable-source-name"
					|| key === "_isJSONSchemaObject"
					|| key === "version"
					|| key === "type"
				) {
					return "";
				}
				const value = props[key];
				if (isPlainObject(value) && isEmptyObject(value)) {
					return "";
				}
				if (typeof value === "string") {
					return `${key}="${value}"`;
				}
				return `${key}={${JSON.stringify(value)}}`;
			})
			.join(" ");
	};

	const printNode = (node: TreeNode): string => {
		const printTag = () => {
			const type = node.props?.type;
			if (type === "string") return "SchemaField.String";
			if (type === "number") return "SchemaField.Number";
			if (type === "boolean") return "SchemaField.Boolean";
			if (type === "date") return "SchemaField.Date";
			if (type === "datetime") return "SchemaField.DateTime";
			if (type === "array") return "SchemaField.Array";
			if (type === "object") return "SchemaField.Object";
			if (type === "void") return "SchemaField.Void";
			return "SchemaField.Markup";
		};
		const tag = printTag();
		const children = node.children.map(child => printNode(child)).join("");
		return children
			? `<${tag} ${printAttribute(node)}>${children}</${tag}>`
			: `<${tag} ${printAttribute(node)} />`;
	};

	const root = tree.find((child) => {
		return child.componentName === "Form" || child.componentName === "Root";
	});

	return `import React, { useMemo } from 'react'
import { createForm } from '@formily/core'
import { createSchemaField } from '@formily/react'
import {
  Form,
  FormItem,
  DatePicker,
  Checkbox,
  Cascader,
  Input,
  NumberPicker,
  Switch,
  Password,
  PreviewText,
  Radio,
  Reset,
  Select,
  Space,
  Submit,
  TimePicker,
  Transfer,
  TreeSelect,
  Upload,
  FormGrid,
  FormLayout,
  FormTab,
  FormCollapse,
  ArrayTable,
  ArrayCards,
} from '@formily/antd-v5'
import { Card, Slider, Rate } from 'antd'

const Text: React.FC<{
  value?: string
  content?: string
  mode?: 'normal' | 'h1' | 'h2' | 'h3' | 'p'
}> = ({ value, mode, content, ...props }) => {
  const tagName = mode === 'normal' || !mode ? 'div' : mode
  return React.createElement(tagName, props, value || content)
}

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
})

export default () => {
  const form = useMemo(() => createForm(), [])

  return <Form form={form} ${root ? printAttribute(root) : ""}>
    <SchemaField>
      ${root ? root.children.map(child => printNode(child)).join("") : ""}
    </SchemaField>
  </Form>
`;
}

export function MarkupSchemaWidget({ tree }: MarkupSchemaWidgetProps) {
	return (
		<MonacoInput
			options={{ readOnly: true }}
			value={transformToMarkupSchemaCode(tree)}
			language="typescript"
		/>
	);
}
