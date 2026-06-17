import type { RoleItemType } from "#src/api/system/role";
import type { MenuItemType } from "#src/api/system/menu";
import type { ActionType, ProColumns, ProCoreActionType } from "@ant-design/pro-components";

import { fetchDeleteRole, fetchRoleList, fetchRolePermissions } from "#src/api/system/role";
import { fetchMenuTree } from "#src/api/system/menu";
import { AuthGuarded } from "#src/components/auth-guarded";
import { BasicButton } from "#src/components/basic-button";
import { BasicContent } from "#src/components/basic-content";
import { BasicTable } from "#src/components/basic-table";
import { useAuth } from "#src/hooks/use-auth";

import { ConfirmPopover } from "#src/components/confirm-popover";
import { PlusCircleOutlined } from "@ant-design/icons";
import { useQuery } from "@tanstack/react-query";
import { Button } from "antd";
import { useRef, useState } from "react";
import { useTranslation } from "react-i18next";

import { Detail } from "./components/detail";
import { getConstantColumns } from "./constants";
import { filterRolesByAppScope, getRoleMenuTreeScope, getVisibleRoleScope } from "./utils";

type FormTreeNode = { id: string; title: string; key: string; nodeType: string; children: FormTreeNode[] };

/** 将后端菜单树转换为 FormTreeItem 需要的格式，保留所有节点并标注类型 */
function menuTreeToFormTree(nodes: MenuItemType[]): FormTreeNode[] {
	return nodes.map(node => ({
		id: String(node.id),
		title: node.nodeType === "button" ? `[按钮] ${node.name}` : node.name,
		key: String(node.id),
		nodeType: node.nodeType,
		children: node.children ? menuTreeToFormTree(node.children) : [],
	}));
}

export default function Role() {
	const { t } = useTranslation();
	const { routeScope } = useAuth();
	const [isOpen, setIsOpen] = useState(false);
	const [title, setTitle] = useState("");
	const [detailData, setDetailData] = useState<Partial<RoleItemType> & { menuIds?: number[]; buttonIds?: number[] }>({});
	const actionRef = useRef<ActionType>(null);
	const permissionScope = routeScope === "platform" ? "platform" : "domain";
	const createAuth = `${permissionScope}.role.create`;
	const updateAuth = `${permissionScope}.role.update`;
	const deleteAuth = `${permissionScope}.role.delete`;

	const { data: menuTreeData = [] } = useQuery({
		queryKey: ["menu-tree-for-role", routeScope],
		queryFn: async () => {
			const treeData = await fetchMenuTree({ scope: getRoleMenuTreeScope(routeScope) });
			return menuTreeToFormTree(treeData as MenuItemType[]);
		},
	});

	const handleDeleteRow = async (id: number, action?: ProCoreActionType<object>) => {
		await fetchDeleteRole(id);
		await action?.reload?.();
		window.$message?.success(t("common.deleteSuccess"));
	};

	const columns: ProColumns<RoleItemType>[] = [
		...getConstantColumns(t),
		{
			title: t("common.action"),
			valueType: "option",
			key: "option",
			width: 160,
			align: "center",
			fixed: "right",
			render: (_, record, __, action) => {
				return (
					<div className="flex justify-center gap-2">
						<AuthGuarded key="editable" auth={updateAuth}>
							<BasicButton
								type="link"
								size="small"
								onClick={async () => {
									const permissions = await fetchRolePermissions(record.id);
									setIsOpen(true);
									setTitle(t("system.role.editRole"));
									setDetailData({
										...record,
										menuIds: permissions.menuIds,
										buttonIds: permissions.buttonIds,
									});
								}}
							>
								{t("common.edit")}
							</BasicButton>
						</AuthGuarded>
						{!record.system ? (
							<AuthGuarded key="delete" auth={deleteAuth}>
								<ConfirmPopover
									title={t("common.confirmDelete")}
									onConfirm={() => handleDeleteRow(record.id, action)}
									okText={t("common.confirm")}
									cancelText={t("common.cancel")}
								>
									<BasicButton type="link" size="small">
										{t("common.delete")}
									</BasicButton>
								</ConfirmPopover>
							</AuthGuarded>
						) : null}
					</div>
				);
			},
		},
	];

	return (
		<BasicContent className="h-full bg-colorBgLayout">
			<BasicTable<RoleItemType>
				adaptive
				columns={columns}
				actionRef={actionRef}
				request={async () => {
					const data = await fetchRoleList();
					const filteredData = filterRolesByAppScope(data, routeScope);
					return {
						data: filteredData,
						success: true,
						total: filteredData.length,
					};
				}}
				headerTitle={t("system.role.name")}
				toolBarRender={() => [
					<AuthGuarded key="add-role" auth={createAuth}>
					<Button
							icon={<PlusCircleOutlined />}
							type="primary"
							onClick={() => {
								setIsOpen(true);
								setTitle(t("system.role.addRole"));
								setDetailData({
									scope: getVisibleRoleScope(routeScope),
								});
							}}
						>
							{t("common.add")}
						</Button>
					</AuthGuarded>,
				]}
			/>
			<Detail
				title={title}
				open={isOpen}
				lockedScope={getVisibleRoleScope(routeScope)}
				onCloseChange={() => {
					setIsOpen(false);
					setDetailData({});
				}}
				detailData={detailData}
				refreshTable={() => {
					return actionRef.current?.reload?.();
				}}
				treeData={menuTreeData}
			/>
		</BasicContent>
	);
}
