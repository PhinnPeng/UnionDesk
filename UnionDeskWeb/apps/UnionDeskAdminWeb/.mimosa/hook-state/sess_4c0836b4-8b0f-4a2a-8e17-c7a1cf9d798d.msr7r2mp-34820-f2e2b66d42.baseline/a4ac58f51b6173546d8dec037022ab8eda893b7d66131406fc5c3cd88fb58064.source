import type { PlatformUserSearchValues } from "../utils";

import { TableSearchForm } from "#src/components/table-search-form";

import { SearchOutlined } from "@ant-design/icons";
import { Form, Input } from "antd";

interface SearchPanelProps {
	onSearch: (values: PlatformUserSearchValues) => void;
	onReset: () => void;
}

export function SearchPanel({ onSearch, onReset }: SearchPanelProps) {
	return (
		<TableSearchForm<PlatformUserSearchValues>
			onFinish={onSearch}
			onReset={() => {
				onReset();
			}}
		>
			<Form.Item name="keyword" label="关键字">
				<Input allowClear placeholder="用户名、账号、手机号、邮箱" prefix={<SearchOutlined />} />
			</Form.Item>
		</TableSearchForm>
	);
}
