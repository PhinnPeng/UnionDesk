import {
	AlignLeftOutlined,
	CalendarOutlined,
	CheckCircleFilled,
	FieldNumberOutlined,
	FieldTimeOutlined,
	FontSizeOutlined,
	UnorderedListOutlined,
} from "@ant-design/icons";

import type { AttributeTypeKey } from "./attribute-utils";
import { getAttributeTypeLabel } from "./attribute-utils";

import "./attribute-type-picker.less";

const ATTRIBUTE_TYPE_OPTIONS: {
	key: AttributeTypeKey;
	description: string;
	icon: React.ReactNode;
}[] = [
	{
		key: "single_select",
		description: "下拉菜单列表，只能选择一项",
		icon: <UnorderedListOutlined />,
	},
	{
		key: "multi_select",
		description: "下拉菜单列表，可以选择多项",
		icon: <UnorderedListOutlined />,
	},
	{
		key: "single_line_text",
		description: "单行文本输入框，适用于较短的文本需求",
		icon: <FontSizeOutlined />,
	},
	{
		key: "multi_line_text",
		description: "多行文本输入框，适合长文本录入",
		icon: <AlignLeftOutlined />,
	},
	{
		key: "date",
		description: "日期选择组件",
		icon: <CalendarOutlined />,
	},
	{
		key: "datetime",
		description: "时间和日期的选择组件",
		icon: <FieldTimeOutlined />,
	},
	{
		key: "integer",
		description: "整数输入框，可根据业务场景配置单位",
		icon: <FieldNumberOutlined />,
	},
	{
		key: "decimal",
		description: "小数输入框，可根据业务场景配置单位",
		icon: <FieldNumberOutlined />,
	},
];

interface AttributeTypePickerProps {
	value?: AttributeTypeKey;
	onChange?: (value: AttributeTypeKey) => void;
	disabled?: boolean;
}

export function AttributeTypePicker({ value, onChange, disabled }: AttributeTypePickerProps) {
	return (
		<div className="attribute-type-picker">
			{ATTRIBUTE_TYPE_OPTIONS.map((item) => {
				const selected = value === item.key;
				return (
					<div
						key={item.key}
						className={`attribute-type-picker__item${selected ? " attribute-type-picker__item--selected" : ""}`}
						role="button"
						tabIndex={disabled ? -1 : 0}
						onClick={() => {
							if (!disabled) {
								onChange?.(item.key);
							}
						}}
						onKeyDown={(event) => {
							if (disabled) {
								return;
							}
							if (event.key === "Enter" || event.key === " ") {
								event.preventDefault();
								onChange?.(item.key);
							}
						}}
					>
						<span className="attribute-type-picker__icon">{item.icon}</span>
						<div className="attribute-type-picker__body">
							<div className="attribute-type-picker__title">{getAttributeTypeLabel(item.key)}</div>
							<div className="attribute-type-picker__desc">{item.description}</div>
						</div>
						{selected ? <CheckCircleFilled className="attribute-type-picker__check" /> : null}
					</div>
				);
			})}
		</div>
	);
}
