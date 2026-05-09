import { menuIcons } from "#src/icons/menu-icons";

import { Select, Space, Tag } from "antd";
import type { SelectProps } from "antd";
import { createElement, useMemo } from "react";

type IconPickerProps = Omit<SelectProps<string>, "options" | "showSearch" | "filterOption">;

export function IconPicker(props: IconPickerProps) {
	const options = useMemo(() => {
		return Object.entries(menuIcons)
			.sort(([a], [b]) => a.localeCompare(b))
			.map(([name, IconComponent]) => ({
				value: name,
				label: (
					<Space size={8}>
						<Tag bordered={false} color="blue" className="m-0">
							{createElement(IconComponent, { style: { fontSize: 14 } })}
						</Tag>
						<span>{name}</span>
					</Space>
				),
				searchText: name.toLowerCase(),
			}));
	}, []);

	return (
		<Select<string>
			{...props}
			showSearch
			allowClear
			placeholder={props.placeholder ?? "请选择图标"}
			options={options}
			filterOption={(input, option) => {
				const searchText = String(option?.searchText ?? option?.value ?? "").toLowerCase();
				return searchText.includes(input.toLowerCase());
			}}
		/>
	);
}

