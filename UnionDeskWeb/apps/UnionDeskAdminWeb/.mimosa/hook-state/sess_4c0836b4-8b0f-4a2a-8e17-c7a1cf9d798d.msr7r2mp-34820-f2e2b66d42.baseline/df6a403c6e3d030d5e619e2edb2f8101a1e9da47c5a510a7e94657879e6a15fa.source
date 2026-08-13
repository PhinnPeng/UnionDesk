import type { OrganizationSearchValues } from "../utils";

import { TableSearchForm } from "#src/components/table-search-form";

import { SearchOutlined } from "@ant-design/icons";
import { Card, DatePicker, Form, Input, Space, Tag } from "antd";

interface SearchPanelProps {
	onSearch: (values: OrganizationSearchValues) => void;
	onReset: () => void;
}

const { RangePicker } = DatePicker;

export function SearchPanel({
	onSearch,
	onReset,
}: SearchPanelProps) {
	return (
		<Card
			title={(
				<Space>
					<SearchOutlined />
					<span>筛选条件</span>
				</Space>
			)}
			extra={<Tag color="blue">名称 / 编码 / 创建时间</Tag>}
			bordered={false}
		>
			<TableSearchForm<OrganizationSearchValues>
				initialValues={{
					keyword: "",
					createdRange: null,
				}}
				onFinish={onSearch}
				onReset={() => {
					onReset();
				}}
			>
				<Form.Item name="keyword" label="关键词">
					<Input placeholder="请输入部门名称或编码" allowClear />
				</Form.Item>
				<Form.Item name="createdRange" label="创建时间">
					<RangePicker className="w-full" showTime />
				</Form.Item>
			</TableSearchForm>
		</Card>
	);
}
