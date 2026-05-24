import { TableSearchForm } from "#src/components/table-search-form";

import { SearchOutlined } from "@ant-design/icons";
import { Card, DatePicker, Form, Input, Space } from "antd";
import type { Dayjs } from "dayjs";

export interface DomainSearchValues {
	keyword?: string;
	createdRange?: [Dayjs | null, Dayjs | null] | null;
}

interface SearchPanelProps {
	onSearch: (values: DomainSearchValues) => void;
	onReset: () => void;
	loading?: boolean;
}

const { RangePicker } = DatePicker;

export function SearchPanel({
	onSearch,
	onReset,
	loading,
}: SearchPanelProps) {
	return (
		<Card
			title={(
				<Space>
					<SearchOutlined />
					<span>筛选条件</span>
				</Space>
			)}
			bordered={false}
			className="mb-4"
		>
			<TableSearchForm<DomainSearchValues>
				loading={loading}
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
					<Input placeholder="请输入业务域编码或名称" allowClear disabled={loading} />
				</Form.Item>
				<Form.Item name="createdRange" label="创建时间">
					<RangePicker className="w-full" showTime disabled={loading} />
				</Form.Item>
			</TableSearchForm>
		</Card>
	);
}
