import type { OrganizationSearchValues } from "../utils";

import { SearchOutlined } from "@ant-design/icons";
import { Button, Card, Col, DatePicker, Form, Input, Row, Space, Tag } from "antd";

interface SearchPanelProps {
	onSearch: (values: OrganizationSearchValues) => void;
	onReset: () => void;
}

const { RangePicker } = DatePicker;

export function SearchPanel({
	onSearch,
	onReset,
}: SearchPanelProps) {
	const [form] = Form.useForm<OrganizationSearchValues>();

	const handleReset = () => {
		form.resetFields();
		onReset();
	};

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
			<Form<OrganizationSearchValues>
				form={form}
				layout="vertical"
				onFinish={values => onSearch(values)}
				initialValues={{
					keyword: "",
					createdRange: null,
				}}
			>
				<Row gutter={16} align="bottom">
					<Col xs={24} md={10}>
						<Form.Item name="keyword" label="关键词">
							<Input placeholder="请输入部门名称或编码" allowClear />
						</Form.Item>
					</Col>
					<Col xs={24} md={10}>
						<Form.Item name="createdRange" label="创建时间">
							<RangePicker className="w-full" showTime />
						</Form.Item>
					</Col>
					<Col xs={24} md={4}>
						<Form.Item label=" ">
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
		</Card>
	);
}
