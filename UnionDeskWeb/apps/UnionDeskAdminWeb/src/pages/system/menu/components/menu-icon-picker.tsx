import { DownOutlined } from "@ant-design/icons";
import type { IconUnit } from "@ant-design/pro-editor";
import PickerPanel from "@ant-design/pro-editor/es/IconPicker/features/PickerPanel";
import StoreUpdater from "@ant-design/pro-editor/es/IconPicker/container/StoreUpdater";
import { Provider, createStore } from "@ant-design/pro-editor/es/IconPicker/store";
import { Input, Popover, Tag } from "antd";
import { createElement, useCallback, useMemo, useState } from "react";

import { menuIcons } from "#src/icons/menu-icons";

interface MenuIconPickerProps {
	disabled?: boolean
	onChange?: (value: string) => void
	placeholder?: string
	value?: string
}

function toIconUnit(value?: string): IconUnit | undefined {
	if (!value) {
		return undefined;
	}
	return {
		type: "antd",
		componentName: value,
	};
}

export function MenuIconPicker({
	disabled,
	onChange,
	placeholder = "请选择图标",
	value,
}: MenuIconPickerProps) {
	const [open, setOpen] = useState(false);
	const iconUnit = useMemo(() => toIconUnit(value), [value]);
	const previewIcon = value ? menuIcons[value] : undefined;

	const handleIconChange = useCallback((icon: IconUnit) => {
		onChange?.(icon.componentName);
		setOpen(false);
	}, [onChange]);

	const handleOpenChange = useCallback((visible: boolean) => {
		if (!disabled) {
			setOpen(visible);
		}
	}, [disabled]);

	const pickerPanel = (
		<Provider createStore={createStore}>
			<StoreUpdater icon={iconUnit} onIconChange={handleIconChange} />
			<PickerPanel />
		</Provider>
	);

	return (
		<Popover
			open={disabled ? false : open}
			onOpenChange={handleOpenChange}
			trigger="click"
			placement="bottomLeft"
			destroyOnHidden
			overlayStyle={{ width: 480 }}
			content={pickerPanel}
		>
			<Input
				readOnly
				disabled={disabled}
				placeholder={placeholder}
				value={value ?? ""}
				prefix={previewIcon ? (
					<Tag variant="filled" color="blue" className="m-0">
						{createElement(previewIcon, { style: { fontSize: 14 } })}
					</Tag>
				) : null}
				suffix={(
					<DownOutlined
						style={{
							transform: open ? "rotate(180deg)" : undefined,
							transition: "transform 0.2s ease",
						}}
					/>
				)}
				style={{
					cursor: disabled ? "not-allowed" : "pointer",
					width: "100%",
				}}
			/>
		</Popover>
	);
}
