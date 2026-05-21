import type { MenuItemType } from "#src/api/system/menu";
import type { ActionType, ProColumns, ProCoreActionType } from "@ant-design/pro-components";
import type { Key } from "react";

import { fetchDeleteMenu, fetchMenuTree } from "#src/api/system/menu";
import { AuthGuarded } from "#src/components/auth-guarded";
import { BasicButton } from "#src/components/basic-button";
import { BasicContent } from "#src/components/basic-content";
import { BasicTable } from "#src/components/basic-table";
import { useAuth } from "#src/hooks/use-auth";
import { getAllExpandedKeys, getExpandedKeysToLevel } from "#src/utils/get-all-expanded-keys";

import { ConfirmPopover } from "#src/components/confirm-popover";
import { CompressOutlined, DownOutlined, ExpandOutlined, PlusCircleOutlined, ReloadOutlined, RightOutlined } from "@ant-design/icons";
import { Button, Card, Space, Tag } from "antd";
import { useEffect, useMemo, useRef, useState, type ReactNode } from "react";
import { useTranslation } from "react-i18next";

import { Detail } from "./components/detail";
import { MenuScopeFilter, type MenuScope } from "./components/menu-scope-filter";
import { getConstantColumns } from "./constants";

function flattenMenuTree(tree: MenuItemType[]): MenuItemType[] {
	const result: MenuItemType[] = [];
	const visit = (nodes: MenuItemType[]) => {
		for (const node of nodes) {
			const { children, ...rest } = node;
			result.push(rest);
			if (children?.length) {
				visit(children);
			}
		}
	};
	visit(tree);
	return result;
}

function getNodeScope(node: MenuItemType) {
	return node.scope === "platform" ? "platform" : "business";
}

function filterMenuTreeByScope(tree: MenuItemType[], scopeFilter: MenuScope): MenuItemType[] {
	const visit = (nodes: MenuItemType[]): MenuItemType[] => {
		return nodes.reduce<MenuItemType[]>((acc, node) => {
			const nextChildren = node.children?.length ? visit(node.children) : [];
			const matchesScope = getNodeScope(node) === scopeFilter;
			if (!matchesScope && nextChildren.length === 0) {
				return acc;
			}

			acc.push({
				...node,
				children: nextChildren.length ? nextChildren : undefined,
			});
			return acc;
		}, []);
	};

	return visit(tree);
}

function getScopeTagColor(scopeFilter: MenuScope) {
	return scopeFilter === "platform" ? "blue" : "green";
}

function getScopeTagLabel(t: (key: string) => string, scopeFilter: MenuScope) {
	return scopeFilter === "platform" ? t("system.menu.platformScope") : t("system.menu.businessScope");
}

/** 默认展开到二级节点（仅展开一级父节点） */
const MENU_DEFAULT_EXPAND_LEVEL = 2;

export default function Menu() {
	const { t } = useTranslation();
	const { routeScope } = useAuth();
	const [isOpen, setIsOpen] = useState(false);
	const [title, setTitle] = useState("");
	const [detailData, setDetailData] = useState<Partial<MenuItemType>>({});
	const [flatParentMenus, setFlatParentMenus] = useState<MenuItemType[]>([]);
	const [menuTreeData, setMenuTreeData] = useState<MenuItemType[]>([]);
	const [scopeFilter, setScopeFilter] = useState<MenuScope>(routeScope === "platform" ? "platform" : "business");
	const [expandedKeys, setExpandedKeys] = useState<Key[]>([]);
	const actionRef = useRef<ActionType>(null);
	const permissionScope = routeScope === "platform" ? "platform" : "domain";
	const createAuth = `${permissionScope}.menu.create`;
	const updateAuth = `${permissionScope}.menu.update`;
	const deleteAuth = `${permissionScope}.menu.delete`;
	const previousScopeRef = useRef(scopeFilter);

	const defaultScope = scopeFilter;
	const canExpandRow = (record: MenuItemType) => record.nodeType !== "button" && Array.isArray(record.children) && record.children.length > 0;

	const handleDeleteRow = async (id: number, action?: ProCoreActionType<object>) => {
		try {
			await fetchDeleteMenu(id);
			await action?.reload?.();
			window.$message?.success(t("common.deleteSuccess"));
		}
		catch (error) {
			window.$message?.error(error instanceof Error ? error.message : t("common.deleteFailed"));
		}
	};

	const columns: ProColumns<MenuItemType>[] = useMemo(() => {
		return [
			...getConstantColumns(t),
			{
				title: t("common.action"),
				valueType: "option",
				key: "option",
				width: 140,
				align: "center",
				fixed: "right",
				render: (_, record, __, action) => {
					if (record.required === true) {
						return (
							<div className="flex justify-center items-center gap-2">
								<Tag>系统</Tag>
								<AuthGuarded key="editable" auth={updateAuth}>
									<BasicButton
										type="link"
										size="small"
										onClick={() => {
											setIsOpen(true);
											setTitle(t("system.menu.editMenu"));
											setDetailData({ ...record });
										}}
									>
										{t("common.edit")}
									</BasicButton>
								</AuthGuarded>
								<AuthGuarded auth={deleteAuth}>
									<ConfirmPopover
										title={t("common.confirmDelete")}
										onConfirm={() => handleDeleteRow(record.id, action)}
										okText={t("common.confirm")}
										cancelText={t("common.cancel")}
									>
										<BasicButton type="link" size="small" danger>
											{t("common.delete")}
										</BasicButton>
									</ConfirmPopover>
								</AuthGuarded>
							</div>
						);
					}

					const btns: ReactNode[] = [];

					if (record.nodeType === "catalog") {
						btns.push(
							<AuthGuarded key="add-child" auth={createAuth}>
								<BasicButton
									type="link"
									size="small"
									onClick={() => {
										setIsOpen(true);
										setTitle(t("system.menu.addMenu"));
										setDetailData({ parentId: record.id, nodeType: "menu" });
									}}
								>
									{t("common.add")}
								</BasicButton>
							</AuthGuarded>,
						);
					}

					if (record.nodeType === "menu") {
						btns.push(
							<AuthGuarded key="add-button" auth={createAuth}>
								<BasicButton
									type="link"
									size="small"
									onClick={() => {
										setIsOpen(true);
										setTitle(t("system.menu.addButton"));
										setDetailData({ parentId: record.id, nodeType: "button" });
									}}
								>
									{t("system.menu.addButton")}
								</BasicButton>
							</AuthGuarded>,
						);
					}

					btns.push(
						<AuthGuarded key="editable" auth={updateAuth}>
							<BasicButton
								type="link"
								size="small"
								onClick={() => {
									setIsOpen(true);
									setTitle(t("system.menu.editMenu"));
									setDetailData({ ...record });
								}}
							>
								{t("common.edit")}
							</BasicButton>
						</AuthGuarded>,
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
						</AuthGuarded>,
					);

					return <div className="flex justify-center gap-2">{btns}</div>;
				},
			},
		];
	}, [createAuth, deleteAuth, t, updateAuth]);

	useEffect(() => {
		if (previousScopeRef.current === scopeFilter) {
			return;
		}
		previousScopeRef.current = scopeFilter;
		actionRef.current?.reload();
	}, [scopeFilter]);

	return (
		<BasicContent className="h-full bg-colorBgLayout">
			<div className="space-y-4">
				<Card className="mb-4">
					<MenuScopeFilter
						value={scopeFilter}
						onChange={setScopeFilter}
					/>
				</Card>

				<BasicTable<MenuItemType>
					adaptive
					pagination={false}
					columns={columns}
					className="menu-tree-table"
					expandable={{
						rowExpandable: canExpandRow,
						expandedRowKeys: expandedKeys,
						onExpandedRowsChange: keys => setExpandedKeys(keys as Key[]),
						expandIcon: ({ expanded, onExpand, record }) => {
							if (!canExpandRow(record)) {
								return null;
							}

							const Icon = expanded ? DownOutlined : RightOutlined;

							return (
								<Icon
									onClick={(event) => onExpand(record, event)}
								/>
							);
						},
					}}
					actionRef={actionRef}
					request={async () => {
						try {
							const treeData = await fetchMenuTree({ scope: scopeFilter });
							const visibleTreeData = filterMenuTreeByScope(treeData, scopeFilter);
							setMenuTreeData(visibleTreeData);
							setExpandedKeys(getExpandedKeysToLevel(visibleTreeData, MENU_DEFAULT_EXPAND_LEVEL, "id") as Key[]);
							setFlatParentMenus(flattenMenuTree(visibleTreeData).filter(item => item.nodeType !== "button"));
							return {
								data: visibleTreeData,
								success: true,
								total: visibleTreeData.length,
							};
						}
						catch (error) {
							window.$message?.error(error instanceof Error ? error.message : "加载菜单失败");
							return {
								data: [],
								success: false,
								total: 0,
							};
						}
					}}
					headerTitle={(
						<Space>
							<span>{t("system.menu.menuList")}</span>
							<Tag color={getScopeTagColor(scopeFilter)}>{getScopeTagLabel(t, scopeFilter)}</Tag>
						</Space>
					)}
					toolBarRender={() => [
						<AuthGuarded key="add-menu" auth={createAuth}>
							<Button
								icon={<PlusCircleOutlined />}
								type="primary"
								onClick={() => {
									setIsOpen(true);
									setTitle(t("system.menu.addMenu"));
									setDetailData({});
								}}
							>
								{t("common.add")}
							</Button>
						</AuthGuarded>,
						<Button
							key="expand-all"
							icon={<ExpandOutlined />}
							onClick={() => setExpandedKeys(getAllExpandedKeys(menuTreeData, "id") as Key[])}
						>
							展开全部
						</Button>,
						<Button
							key="collapse-all"
							icon={<CompressOutlined />}
							onClick={() => setExpandedKeys([])}
						>
							收起全部
						</Button>,
						<Button
							key="refresh"
							icon={<ReloadOutlined />}
							onClick={() => actionRef.current?.reload()}
						>
							刷新
						</Button>,
					]}
				/>
			</div>
			<Detail
				title={title}
				open={isOpen}
				flatParentMenus={flatParentMenus}
				defaultScope={defaultScope}
				onCloseChange={() => {
					setIsOpen(false);
					setDetailData({});
				}}
				detailData={detailData}
				refreshTable={() => {
					actionRef.current?.reload();
				}}
			/>
		</BasicContent>
	);
}
