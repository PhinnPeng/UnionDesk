import type { IamUser, PlatformOrganizationView } from "@uniondesk/shared";

import type { RoleItemType } from "#src/api/system/role";
import { fetchCreatePlatformUser, fetchUpdatePlatformUser } from "#src/api/platform/iam";
import { buildOrganizationTree, type OrganizationTreeNode } from "#src/pages/platform/dept/utils";
import { toErrorMessage } from "@uniondesk/shared";
import {
	buildCreatePlatformUserPayload,
	buildUpdatePlatformUserPayload,
	generateResetPassword,
	type PlatformUserFormValues,
} from "#src/pages/platform/user/utils";

import { App, Button, Form, Input, Modal, Select, TreeSelect } from "antd";
import { useEffect, useMemo, useState } from "react";

interface DetailProps {
	open: boolean;
	mode: "create" | "edit";
	user: IamUser | null;
	roles: RoleItemType[];
	organizations: PlatformOrganizationView[];
	onClose: () => void;
	onSuccess: (user: IamUser) => void | Promise<void>;
}

type DepartmentTreeSelectNode = {
	title: string;
	value: number;
	children: DepartmentTreeSelectNode[];
};

function buildDepartmentTreeSelectData(nodes: OrganizationTreeNode[]): DepartmentTreeSelectNode[] {
	return nodes.map(node => ({
		title: node.name,
		value: node.id,
		children: buildDepartmentTreeSelectData(node.children),
	}));
}

function buildRoleOptions(roles: RoleItemType[]) {
	return roles.map(role => ({
		label: `${role.name} / ${role.code}`,
		value: role.code,
	}));
}

function buildDepartmentInitialTree(organizations: PlatformOrganizationView[]) {
	return buildDepartmentTreeSelectData(buildOrganizationTree(organizations));
}

export function Detail({
	open,
	mode,
	user,
	roles,
	organizations,
	onClose,
	onSuccess,
}: DetailProps) {
	const { message } = App.useApp();
	const [form] = Form.useForm<PlatformUserFormValues>();
	const [saving, setSaving] = useState(false);
	const isCreate = mode === "create";

	const departmentOptions = useMemo(() => buildDepartmentInitialTree(organizations), [organizations]);
	const roleOptions = useMemo(() => buildRoleOptions(roles), [roles]);
	const title = isCreate ? "新增用户" : "编辑用户";

	useEffect(() => {
		if (!open) {
			form.resetFields();
			return;
		}

		form.setFieldsValue({
			username: user?.username ?? "",
			mobile: user?.mobile ?? "",
			email: user?.email ?? "",
			roleCodes: user?.roleCodes ?? [],
			organizationId: user?.organizationIds?.[0] ?? null,
			remark: user?.remark ?? "",
		});
	}, [form, open, user]);

	const handleGeneratePassword = () => {
		form.setFieldsValue({
			password: generateResetPassword(),
		});
	};

	const handleSave = async () => {
		const values = await form.validateFields().catch(() => null);
		if (!values) {
			return;
		}

		setSaving(true);
		try {
			let savedUser: IamUser;
			if (isCreate) {
				savedUser = await fetchCreatePlatformUser(buildCreatePlatformUserPayload(values));
			}
			else {
				if (!user) {
					throw new Error("缺少要编辑的用户");
				}
				savedUser = await fetchUpdatePlatformUser(
					user.id,
					buildUpdatePlatformUserPayload(values, user.accountType),
				);
			}
			message.success(isCreate ? "新增用户成功" : "用户保存成功");
			await onSuccess(savedUser);
			onClose();
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
		finally {
			setSaving(false);
		}
	};

	return (
		<Modal
			open={open}
			title={title}
			width={720}
			okText="保存"
			cancelText="取消"
			confirmLoading={saving}
			onOk={() => void handleSave()}
			onCancel={onClose}
			destroyOnHidden
		>
			<Form<PlatformUserFormValues>
				form={form}
				layout="vertical"
				preserve={false}
			>
				<Form.Item
					name="username"
					label="用户名"
					rules={[{ required: true, message: "请输入用户名" }]}
				>
					<Input placeholder="请输入用户名" />
				</Form.Item>

				<Form.Item
					name="mobile"
					label="手机号"
					rules={[{ required: true, message: "请输入手机号" }]}
				>
					<Input placeholder="请输入手机号" />
				</Form.Item>

				<Form.Item
					name="email"
					label="邮箱"
				>
					<Input placeholder="请输入邮箱" />
				</Form.Item>

				<Form.Item
					name="organizationId"
					label="主部门"
				>
					<TreeSelect
						treeDefaultExpandAll
						showSearch
						allowClear
						placeholder="请选择主部门"
						treeData={departmentOptions}
						filterTreeNode={(input, treeNode) => String(treeNode.title ?? "").toLowerCase().includes(input.toLowerCase())}
					/>
				</Form.Item>

				{isCreate ? (
					<Form.Item
						label="登录密码"
						required
					>
						<div className="flex gap-2">
							<Form.Item
								name="password"
								noStyle
								rules={[{ required: true, message: "请输入登录密码" }]}
							>
								<Input.Password
									className="flex-1"
									placeholder="请输入登录密码"
									autoComplete="new-password"
								/>
							</Form.Item>
							<Button htmlType="button" onClick={handleGeneratePassword}>
								随机生成
							</Button>
						</div>
					</Form.Item>
				) : null}

				<Form.Item
					name="roleCodes"
					label="角色"
					rules={[{ required: true, message: "请选择角色" }]}
				>
					<Select
						mode="multiple"
						placeholder="请选择角色"
						options={roleOptions}
						showSearch
						filterOption={(input, option) => String(option?.label ?? "").toLowerCase().includes(input.toLowerCase())}
					/>
				</Form.Item>

				<Form.Item
					name="remark"
					label="备注"
				>
					<Input.TextArea rows={4} placeholder="请输入备注" />
				</Form.Item>
			</Form>
		</Modal>
	);
}
