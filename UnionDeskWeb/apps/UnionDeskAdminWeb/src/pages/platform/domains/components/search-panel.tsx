import { SearchOutlined } from "@ant-design/icons";
import { Button, Card, Col, DatePicker, Form, Input, Row, Space, Tag } from "antd";
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
	const [form] = Form.useForm<DomainSearchValues>();

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
			extra={<Tag color="blue">编码 / 名称 / 创建时间</Tag>}
			bordered={false}
			className="mb-4"
		>
			<Form<DomainSearchValues>
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
							<Input placeholder="请输入业务域编码或名称" allowClear disabled={loading} />
						</Form.Item>
					</Col>
					<Col xs={24} md={10}>
						<Form.Item name="createdRange" label="创建时间">
							<RangePicker className="w-full" showTime disabled={loading} />
						</Form.Item>
					</Col>
					<Col xs={24} md={4}>
						<Form.Item label=" ">
							<Space>
								<Button type="primary" htmlType="submit" loading={loading}>
									搜索
								</Button>
								<Button onClick={handleReset} disabled={loading}>
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
