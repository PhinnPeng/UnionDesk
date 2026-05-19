import type { PlatformOrganizationView } from "@uniondesk/shared";

import { AuthGuarded } from "#src/components/auth-guarded";

import { ApartmentOutlined, DeleteOutlined } from "@ant-design/icons";
import { Button, Col, Form, Input, InputNumber, Modal, Row, Select, Space, Tag, TreeSelect, Typography } from "antd";
import { useEffect, useMemo } from "react";

import { buildOrganizationTree, generateDepartmentCode, type OrganizationTreeNode } from "../utils";

export type OrganizationFormValues = {
	code: string;
	name: string;
	parentId: number | null;
	leaderUserId?: number | null;
	orderNo?: number;
	status?: number;
	remark?: string | null;
};

interface DetailProps {
	open: boolean;
	mode: "create" | "edit";
	organization: PlatformOrganizationView | null;
	parentOrganization: PlatformOrganizationView | null;
	organizations: PlatformOrganizationView[];
	leaderOptions: Array<{ label: string; value: number }>;
	saving: boolean;
	onSubmit: (values: OrganizationFormValues) => Promise<void>;
	onClose: () => void;
	onDelete: () => Promise<void>;
}

type DepartmentTreeSelectNode = {
	title: string;
	value: number;
	disabled: boolean;
	children: DepartmentTreeSelectNode[];
};

function buildInitialValues(
	mode: "create" | "edit",
	organization: PlatformOrganizationView | null,
	parentOrganization: PlatformOrganizationView | null,
): OrganizationFormValues {
	if (mode === "create") {
		return {
			code: generateDepartmentCode(),
			name: "",
			parentId: parentOrganization?.id ?? null,
			leaderUserId: null,
			orderNo: 0,
			status: 1,
			remark: "",
		};
	}

	if (!organization) {
		return {
			code: "",
			name: "",
			parentId: null,
			leaderUserId: null,
			orderNo: 0,
			status: 1,
			remark: "",
		};
	}

	return {
		code: organization.code,
		name: organization.name,
		parentId: organization.parentId ?? null,
		leaderUserId: organization.leaderUserId ?? null,
		orderNo: organization.orderNo,
		status: organization.status,
		remark: organization.remark ?? "",
	};
}

export function Detail({
	open,
	mode,
	organization,
	parentOrganization,
	organizations,
	leaderOptions,
	saving,
	onSubmit,
	onClose,
	onDelete,
}: DetailProps) {
	const [form] = Form.useForm<OrganizationFormValues>();

	useEffect(() => {
		if (!open) {
			return;
		}
		form.setFieldsValue(buildInitialValues(mode, organization, parentOrganization));
	}, [form, mode, open, organization, parentOrganization]);

	const handleSubmit = async () => {
		const values = await form.validateFields().catch(() => null);
		if (!values) {
			return;
		}

		await onSubmit({
			code: values.code.trim(),
			name: values.name.trim(),
			parentId: values.parentId ?? null,
			leaderUserId: values.leaderUserId ?? null,
			orderNo: values.orderNo ?? 0,
			status: values.status ?? 1,
			remark: values.remark?.trim() || null,
		});
	};

	const treeData = useMemo(() => {
		const tree = buildOrganizationTree(organizations);

		const transform = (nodes: OrganizationTreeNode[]): DepartmentTreeSelectNode[] => {
			return nodes.map(node => {
				const isSelf = mode === "edit" && node.id === organization?.id;
				return {
					title: node.name,
					value: node.id,
					disabled: isSelf,
					children: isSelf ? [] : transform(node.children),
				};
			});
		};

		return transform(tree);
	}, [mode, organization?.id, organizations]);

	const contextText = mode === "create"
		? (parentOrganization ? `新增 ${parentOrganization.name} 的下级部门` : "新增顶级部门")
		: `编辑部门：${organization?.name ?? ""}`;

	return (
		<Modal
			open={open}
			title={(
				<Space>
					<ApartmentOutlined />
					<span>{mode === "create" ? "新增部门" : "编辑部门"}</span>
					<Tag color={mode === "create" ? "processing" : "blue"}>{mode === "create" ? "新增" : "编辑"}</Tag>
				</Space>
			)}
			onCancel={onClose}
			width={720}
			destroyOnHidden
			maskClosable={false}
			footer={(
				<div className="flex items-center justify-between gap-3">
					<div>
						{mode === "edit" ? (
							<AuthGuarded auth="platform.organization.delete">
								<Button danger icon={<DeleteOutlined />} onClick={() => void onDelete()}>
									删除部门
								</Button>
							</AuthGuarded>
						) : null}
					</div>
					<Space>
						<Button onClick={onClose}>
							取消
						</Button>
						<AuthGuarded auth={mode === "create" ? "platform.organization.create" : "platform.organization.update"}>
							<Button type="primary" loading={saving} onClick={() => void handleSubmit()}>
								保存
							</Button>
						</AuthGuarded>
					</Space>
				</div>
			)}
		>
			<Typography.Text className="mb-4 block" type="secondary">
				{contextText}
			</Typography.Text>
			<Form<OrganizationFormValues>
				form={form}
				layout="vertical"
				preserve={false}
			>
				<Row gutter={16}>
					<Col span={24}>
						<Form.Item
							name="code"
							label="部门编码"
							rules={[{ required: true, message: "请输入部门编码" }]}
						>
							<Input placeholder="系统已自动生成，可按需调整" />
						</Form.Item>
					</Col>
					<Col span={24}>
						<Form.Item
							name="name"
							label="部门名称"
							rules={[{ required: true, message: "请输入部门名称" }]}
						>
							<Input placeholder="请输入部门名称" />
						</Form.Item>
					</Col>
					<Col span={24}>
						<Form.Item
							name="parentId"
							label="上级部门"
						>
							<TreeSelect
								showSearch
								placeholder="请选择上级部门，留空表示顶级部门"
								allowClear
								treeDefaultExpandAll
								treeData={treeData}
								filterTreeNode={(input, treeNode) => String(treeNode.title ?? "").toLowerCase().includes(input.toLowerCase())}
							/>
						</Form.Item>
					</Col>
					<Col xs={24} lg={12}>
						<Form.Item name="leaderUserId" label="负责人">
							<Select
								allowClear
								showSearch
								placeholder="请选择负责人"
								options={leaderOptions}
								filterOption={(input, option) => String(option?.label ?? "").toLowerCase().includes(input.toLowerCase())}
							/>
						</Form.Item>
					</Col>
					<Col xs={24} lg={12}>
						<Form.Item name="orderNo" label="排序">
							<InputNumber className="w-full" min={0} placeholder="请输入排序值" />
						</Form.Item>
					</Col>
					<Col xs={24} lg={12}>
						<Form.Item name="status" label="状态">
							<Select
								options={[
									{ label: "启用", value: 1 },
									{ label: "停用", value: 0 },
								]}
							/>
						</Form.Item>
					</Col>
					<Col span={24}>
						<Form.Item name="remark" label="备注">
							<Input.TextArea rows={4} placeholder="请输入备注" />
						</Form.Item>
					</Col>
				</Row>
			</Form>
		</Modal>
	);
}
