import type { PlatformUserSearchValues } from "../utils";

import { SearchOutlined } from "@ant-design/icons";
import { Button, Col, Form, Input, Row, Space } from "antd";

interface SearchPanelProps {
	onSearch: (values: PlatformUserSearchValues) => void;
	onReset: () => void;
}

export function SearchPanel({ onSearch, onReset }: SearchPanelProps) {
	const [form] = Form.useForm<PlatformUserSearchValues>();

	const handleReset = () => {
		form.resetFields();
		onReset();
	};

	return (
		<Form<PlatformUserSearchValues>
			form={form}
			layout="vertical"
			onFinish={onSearch}
		>
			<Row gutter={16} align="bottom">
				<Col xs={24} lg={10}>
					<Form.Item name="keyword" label="关键字">
						<Input allowClear placeholder="用户名、账号、手机号、邮箱" prefix={<SearchOutlined />} />
					</Form.Item>
				</Col>
				<Col xs={24} lg={14}>
					<Form.Item>
						<Space>
							<Button type="primary" htmlType="submit">
								搜索
							</Button>
							<Button onClick={handleReset}>
								重置
							</Button>
						</Space>
					</Form.Item>
				</Col>
			</Row>
		</Form>
	);
}
