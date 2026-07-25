import type { TicketTypeTemplateKey } from "./ticket-type-utils";
import { TICKET_TYPE_TEMPLATES } from "./ticket-type-utils";

import { FileTextOutlined, FormOutlined, PlusOutlined } from "@ant-design/icons";
import { Button, Dropdown, Typography } from "antd";
import type { ReactNode } from "react";
import { useState } from "react";

import "./create-ticket-type-dropdown.less";

const { Text, Paragraph } = Typography;

const TEMPLATE_ICONS: Record<TicketTypeTemplateKey, ReactNode> = {
	simple_ticket: <FileTextOutlined className="create-ticket-type-dropdown__icon create-ticket-type-dropdown__icon--simple" />,
	standard_ticket: <FormOutlined className="create-ticket-type-dropdown__icon create-ticket-type-dropdown__icon--standard" />,
};

interface CreateTicketTypeDropdownProps {
	onSelect: (key: TicketTypeTemplateKey) => void;
}

export function CreateTicketTypeDropdown({ onSelect }: CreateTicketTypeDropdownProps) {
	const [open, setOpen] = useState(false);

	const dropdownRender = () => (
		<div className="create-ticket-type-dropdown__panel">
			<Text type="secondary" className="create-ticket-type-dropdown__header">
				选择事项类型
			</Text>
			<div className="create-ticket-type-dropdown__options">
				{TICKET_TYPE_TEMPLATES.map(item => (
					<button
						key={item.key}
						type="button"
						className="create-ticket-type-dropdown__option"
						onClick={() => {
							setOpen(false);
							onSelect(item.key);
						}}
					>
						<div className="create-ticket-type-dropdown__option-icon">
							{TEMPLATE_ICONS[item.key]}
						</div>
						<div className="create-ticket-type-dropdown__option-body">
							<Text strong className="create-ticket-type-dropdown__option-title">
								{item.label}
							</Text>
							<Paragraph type="secondary" className="create-ticket-type-dropdown__option-desc">
								{item.helperText}
							</Paragraph>
						</div>
					</button>
				))}
			</div>
			<div className="create-ticket-type-dropdown__footer">
				<Text type="secondary">
					系统预置属性「标题」「描述」会在创建时自动关联，无需在属性字典中重复添加。
				</Text>
			</div>
		</div>
	);

	return (
		<Dropdown
			open={open}
			onOpenChange={setOpen}
			popupRender={dropdownRender}
			trigger={["click"]}
			placement="bottomRight"
		>
			<Button type="primary" icon={<PlusOutlined />}>
				创建事项类型
			</Button>
		</Dropdown>
	);
}
