import type { RoleItemType, RolePayload } from "#src/api/system/role";
import type { TreeDataNodeWithId } from "#src/components/basic-form";
import { fetchAddRole, fetchUpdateRole, fetchUpdateRolePermissions } from "#src/api/system/role";
import { FormTreeItem } from "#src/components/basic-form";

import {
	DrawerForm,
	ProFormText,
} from "@ant-design/pro-components";
import { App, Button, Form, Tag } from "antd";
import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";

interface RoleFormValues {
	name: string
	code: string
	menus: string[]
}

interface TreeNodeWithType extends TreeDataNodeWithId {
	nodeType?: string
}

interface DetailProps {
	treeData: TreeNodeWithType[]
	title: React.ReactNode
	open: boolean
	lockedScope: RoleItemType["scope"]
	detailData: Partial<RoleItemType> & { menuIds?: number[]; buttonIds?: number[] }
	onCloseChange: () => void
	refreshTable?: () => void | Promise<void>
}

function flattenTree(nodes: TreeNodeWithType[]): TreeNodeWithType[] {
	const result: TreeNodeWithType[] = [];
	for (const node of nodes) {
		result.push(node);
		if (node.children?.length) {
			result.push(...flattenTree(node.children as TreeNodeWithType[]));
		}
	}
	return result;
}

function getScopeLabel(scope: RoleItemType["scope"]) {
	return scope === "global" ? "平台角色" : "业务域角色";
}

export function Detail({ title, open, onCloseChange, detailData, treeData, refreshTable, lockedScope }: DetailProps) {
	const { t } = useTranslation();
	const { message } = App.useApp();
	const [form] = Form.useForm<RoleFormValues>();
	const [submitting, setSubmitting] = useState(false);
	const isEdit = !!detailData.id;

	const saveRole = async (values: RoleFormValues) => {
		const payload: RolePayload = {
			name: values.name,
			code: values.code,
			scope: lockedScope,
		};
		const flatNodes = flattenTree(treeData);
		const nodeTypeMap = new Map(flatNodes.map(n => [n.id, n.nodeType]));
		const selected = values.menus || [];
		const menuIds = selected.filter((id) => {
			const nodeType = nodeTypeMap.get(id);
			return nodeType === "menu" || nodeType === "catalog";
		}).map(Number);
		const buttonIds = selected.filter(id => nodeTypeMap.get(id) === "button").map(Number);
		if (isEdit) {
			await fetchUpdateRole(detailData.id!, payload);
			await fetchUpdateRolePermissions(detailData.id!, { menuIds, buttonIds });
			message.success(t("common.updateSuccess"));
		}
		else {
			const newRole = await fetchAddRole(payload);
			if (menuIds.length > 0 || buttonIds.length > 0) {
				await fetchUpdateRolePermissions(newRole.id, { menuIds, buttonIds });
			}
			message.success(t("common.addSuccess"));
		}
		onCloseChange();
		await refreshTable?.();
		return true;
	};

	const handleSubmit = async () => {
		if (submitting) {
			return;
		}
		const values = await form.validateFields(["name", "code"]);
		setSubmitting(true);
		try {
			await saveRole({
				...values,
				menus: form.getFieldValue("menus") ?? [],
			});
		}
		catch (error) {
			message.error(error instanceof Error ? error.message : "请求失败");
		}
		finally {
			setSubmitting(false);
		}
	};

	useEffect(() => {
		if (open) {
			const allSelected = [
				...(detailData.menuIds?.map(String) ?? []),
				...(detailData.buttonIds?.map(String) ?? []),
			];
			form.setFieldsValue({
				name: detailData.name ?? "",
				code: detailData.code ?? "",
				menus: allSelected,
			});
		}
	}, [open, detailData, form]);

	return (
		<DrawerForm<RoleFormValues>
			title={title}
			open={open}
			onOpenChange={(visible) => {
				if (visible === false) {
					onCloseChange();
				}
			}}
			resize={{
				onResize() {},
				maxWidth: window.innerWidth * 0.8,
				minWidth: 500,
			}}
			labelCol={{ span: 6 }}
			wrapperCol={{ span: 24 }}
			layout="horizontal"
			form={form}
			autoFocusFirstInput
			drawerProps={{
				destroyOnHidden: true,
			}}
			onFinish={saveRole}
			submitter={{
				render: () => [
					<Button key="cancel" onClick={onCloseChange}>
						{t("common.cancel")}
					</Button>,
					<Button key="submit" type="primary" loading={submitting} onClick={handleSubmit}>
						{t("common.confirm")}
					</Button>,
				],
			}}
			initialValues={{
				menus: [],
			}}
		>
			<ProFormText
				allowClear
				rules={[{ required: true }]}
				width="md"
				name="name"
				label={t("system.role.name")}
				tooltip={t("form.length", { length: 24 })}
			/>

			<ProFormText
				allowClear
				rules={[{ required: true }]}
				width="md"
				name="code"
				label={t("system.role.id")}
				disabled={isEdit}
			/>

			<Form.Item label={t("system.role.scope")}>
				<Tag color={lockedScope === "global" ? "blue" : "green"}>{getScopeLabel(lockedScope)}</Tag>
			</Form.Item>

			<Form.Item name="menus" label={t("system.role.assignMenu")} tooltip="按钮节点标注 [按钮] 前缀；勾选父节点将联动勾选所有子节点">
				<FormTreeItem treeData={treeData} checkStrictly />
			</Form.Item>
		</DrawerForm>
	);
}
