import type { MenuItemType } from "#src/api/system/menu";
import type { AdminPermissionCodeView } from "#src/api/platform/iam";

import { fetchCreateMenu, fetchUpdateMenu } from "#src/api/system/menu";
import { fetchAdminPermissionCodes } from "#src/api/platform/iam";
import { handleTree } from "#src/utils/tree";

import { Alert, Form } from "antd";
import {
	ModalForm,
	ProFormCascader,
	ProFormDependency,
	ProFormDigit,
	ProForm,
	ProFormRadio,
	ProFormSelect,
	ProFormText,
} from "@ant-design/pro-components";
import { useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";

import { componentRegistry, registeredComponentKeys } from "./component-registry";
import { MenuIconPicker } from "./menu-icon-picker";

type MenuFormValues = Partial<MenuItemType> & {
	parentId?: number[] | number | null
	hidden?: boolean
};

interface DetailProps {
	title: React.ReactNode
	flatParentMenus: MenuItemType[]
	open: boolean
	detailData: Partial<MenuItemType>
	defaultScope?: "platform" | "business"
	onCloseChange: () => void
	refreshTable?: () => void
}

function groupPermissionOptions(items: AdminPermissionCodeView[], translate: (key: string) => string) {
	const groups = new Map<string, AdminPermissionCodeView[]>();
	for (const item of items) {
		const scope = item.permissionScope || "shared";
		const current = groups.get(scope) ?? [];
		current.push(item);
		groups.set(scope, current);
	}

	return Array.from(groups.entries()).map(([scope, options]) => ({
		label: scope === "platform" ? translate("system.menu.platformPermissions") : scope === "domain" ? translate("system.menu.domainPermissions") : translate("system.menu.sharedPermissions"),
		options: options.map(item => ({
			label: (
				<div className="flex flex-col gap-1">
					<div className="flex items-center gap-2">
						<span className="rounded bg-blue-50 px-2 py-0.5 text-xs text-blue-700">{item.code}</span>
						<span>{item.name}</span>
					</div>
					<span className="text-xs text-slate-500">{item.httpMethod} {item.pathPattern}</span>
				</div>
			),
			value: item.code,
			searchText: `${item.code} ${item.name} ${item.httpMethod} ${item.pathPattern}`,
		})),
	}));
}

function getParentPath(flatParentMenus: MenuItemType[], parentId?: number | null) {
	if (typeof parentId !== "number") {
		return [];
	}
	const menuMap = new Map(flatParentMenus.map(item => [item.id, item]));
	const path: number[] = [];
	const visited = new Set<number>();
	let currentId: number | null = parentId;

	while (typeof currentId === "number" && !visited.has(currentId)) {
		visited.add(currentId);
		const currentNode = menuMap.get(currentId);
		if (!currentNode) {
			path.unshift(currentId);
			break;
		}
		path.unshift(currentId);
		currentId = currentNode.parentId ?? null;
	}

	return path;
}

function normalizeParentId(parentId?: number[] | number | null) {
	if (Array.isArray(parentId)) {
		if (!parentId.length) {
			return null;
		}
		return parentId[parentId.length - 1] ?? null;
	}
	if (typeof parentId === "number") {
		return parentId;
	}
	return null;
}

function normalizeComponentKey(componentKey?: string | null) {
	return componentKey?.trim().replace(/^\.\//, "") || null;
}

export function Detail({
	title,
	open,
	flatParentMenus,
	onCloseChange,
	detailData,
	defaultScope = "business",
	refreshTable,
}: DetailProps) {
	const { t } = useTranslation();
	const [form] = Form.useForm<MenuFormValues>();
	const [permissionGroups, setPermissionGroups] = useState<ReturnType<typeof groupPermissionOptions>>([]);
	const componentKey = Form.useWatch("componentKey", form);
	const nodeType = Form.useWatch("nodeType", form);
	const normalizedComponentKey = normalizeComponentKey(componentKey);

	const resolveScopeValue = (parentId?: number | null, scope?: string) => {
		if (scope) {
			return scope;
		}
		const matchedParent = flatParentMenus.find(item => item.id === parentId);
		return matchedParent?.scope || defaultScope;
	};

	const componentOptions = useMemo(() => {
		return componentRegistry.map(item => ({
			label: item.label,
			value: item.value,
			searchText: `${item.label} ${item.value}`,
		}));
	}, []);

	useEffect(() => {
		if (!open) {
			return;
		}
		let ignore = false;
		void (async () => {
			try {
				const [platformCodes, domainCodes, sharedCodes] = await Promise.all([
					fetchAdminPermissionCodes("platform"),
					fetchAdminPermissionCodes("domain"),
					fetchAdminPermissionCodes("shared"),
				]);
				if (!ignore) {
					setPermissionGroups(groupPermissionOptions([...platformCodes, ...domainCodes, ...sharedCodes], t));
				}
			}
			catch (error) {
				window.$message?.error(error instanceof Error ? error.message : "加载权限码失败");
			}
		})();
		return () => {
			ignore = true;
		};
	}, [open, t]);

	useEffect(() => {
		if (open) {
			form.setFieldsValue({
				...detailData,
				nodeType: detailData.nodeType ?? "menu",
				parentId: getParentPath(flatParentMenus, detailData.parentId as number | null | undefined),
				scope: resolveScopeValue(detailData.parentId as number | null | undefined, detailData.scope),
				componentKey: normalizeComponentKey(detailData.componentKey),
				hidden: detailData.hidden ?? false,
				status: detailData.status ?? 1,
				orderNo: detailData.orderNo ?? 0,
			});
		}
	}, [open, detailData, form, flatParentMenus, defaultScope]);

	const onFinish = async (values: MenuFormValues) => {
		const nodeTypeValue = values.nodeType ?? "menu";
		const parentId = normalizeParentId(values.parentId);
		const payload = {
			name: values.name ?? "",
			nodeType: nodeTypeValue,
			routePath: nodeTypeValue === "button" ? null : (values.routePath?.trim() || null),
			componentKey: nodeTypeValue === "menu" ? normalizeComponentKey(values.componentKey) : null,
			permissionCode: values.permissionCode?.trim() || null,
			scope: values.scope ?? resolveScopeValue(parentId, detailData.scope),
			parentId,
			orderNo: Number(values.orderNo ?? 0),
			icon: nodeTypeValue === "button" ? null : (values.icon?.trim() || null),
			hidden: Boolean(values.hidden),
			status: Number(values.status ?? 1),
		};

		if (detailData.id) {
			await fetchUpdateMenu(detailData.id, payload);
			window.$message?.success(t("common.updateSuccess"));
		}
		else {
			await fetchCreateMenu(payload);
			window.$message?.success(t("common.addSuccess"));
		}
		refreshTable?.();
		return true;
	};

	const currentNodeType = String(nodeType ?? "menu");

	return (
		<ModalForm<MenuFormValues>
			title={title}
			open={open}
			labelAlign="left"
			onOpenChange={(visible) => {
				if (visible === false) {
					onCloseChange();
				}
			}}
			labelCol={{ span: 6 }}
			layout="horizontal"
			form={form}
			autoFocusFirstInput
			modalProps={{
				destroyOnHidden: true,
			}}
			grid
			width={{
				xl: 860,
				md: 560,
			}}
			onFinish={onFinish}
			initialValues={{
				nodeType: "menu",
				status: 1,
				hidden: false,
				scope: defaultScope,
			}}
		>
			<ProFormRadio.Group
				fieldProps={{
					buttonStyle: "solid",
				}}
				name="nodeType"
				label={t("system.menu.menuType")}
				tooltip={t("system.menu.menuTypeTooltip")}
				labelCol={{ span: 3 }}
				colProps={{ md: 24, xl: 24 }}
				radioType="button"
				required
				options={[
					{ label: t("system.menu.catalog"), value: "catalog" },
					{ label: t("system.menu.menu"), value: "menu" },
					{ label: t("system.menu.button"), value: "button" },
				]}
			/>

			<ProFormCascader
				name="parentId"
				label={t("system.menu.parentMenu")}
				tooltip={t("system.menu.parentMenuTooltip")}
				labelCol={{ span: 3 }}
				colProps={{ md: 24, xl: 24 }}
				fieldProps={{
					showSearch: true,
					autoClearSearchValue: true,
					fieldNames: {
						label: "name",
						value: "id",
						children: "children",
					},
				}}
				request={async () => {
					return handleTree(flatParentMenus);
				}}
			/>

			<ProFormRadio.Group
				name="scope"
				label={t("system.menu.scope")}
				tooltip={t("system.menu.scopeTooltip")}
				radioType="button"
				labelCol={{ span: 6 }}
				colProps={{ md: 24, xl: 12 }}
				fieldProps={{
					buttonStyle: "solid",
					disabled: Boolean(detailData.id),
				}}
				rules={[{ required: true }]}
				options={[
					{ label: t("system.menu.businessScope"), value: "business" },
					{ label: t("system.menu.platformScope"), value: "platform" },
				]}
			/>

			<ProFormDependency name={["nodeType"]}>
				{({ nodeType: dependencyNodeType }) => {
					const dependencyValue = String(dependencyNodeType ?? "menu");
					const dependencyShowRoutePath = dependencyValue !== "button";
					const dependencyShowIcon = dependencyValue !== "button";
					const dependencyShowComponentKey = dependencyValue === "menu";
					const dependencyShowPermissionCode = dependencyValue !== "catalog";

					return (
						<>
							<ProFormText
								allowClear
								rules={[{ required: true }]}
								labelCol={{ span: 6 }}
								colProps={{ md: 24, xl: 12 }}
								name="name"
								label={t("system.menu.name")}
								tooltip={t("system.menu.nameTooltip")}
							/>
							{dependencyShowRoutePath ? (
								<ProFormText
									allowClear
									labelCol={{ span: 6 }}
									colProps={{ md: 24, xl: 12 }}
									name="routePath"
									label={t("system.menu.routePath")}
									tooltip={t("system.menu.routePathTooltip")}
								/>
							) : null}
							<ProFormDigit
								allowClear
								rules={[{ required: true }]}
								labelCol={{ span: 6 }}
								colProps={{ md: 24, xl: 12 }}
								name="orderNo"
								label={t("system.menu.menuOrder")}
								tooltip={t("system.menu.menuOrderTooltip")}
							/>
							{dependencyShowIcon ? (
								<ProForm.Item
									name="icon"
									label={t("system.menu.menuIcon")}
									tooltip={t("system.menu.menuIconTooltip")}
									labelCol={{ span: 6 }}
									colProps={{ md: 24, xl: 12 }}
									rules={[{ required: true }]}
								>
									<MenuIconPicker placeholder={t("system.menu.menuIcon")} />
								</ProForm.Item>
							) : null}
							{dependencyShowComponentKey ? (
								<ProFormSelect
									name="componentKey"
									label={t("system.menu.componentUrl")}
									tooltip={t("system.menu.componentUrlTooltip")}
									labelCol={{ span: 6 }}
									colProps={{ md: 24, xl: 12 }}
									fieldProps={{
										showSearch: true,
										optionFilterProp: "searchText",
										options: componentOptions,
									}}
									placeholder={t("system.menu.componentUrl")}
								/>
							) : null}
							{dependencyShowPermissionCode ? (
								<ProFormSelect
									name="permissionCode"
									label={t("system.menu.permissionCode")}
									tooltip={t("system.menu.permissionCodeTooltip")}
									labelCol={{ span: 6 }}
									colProps={{ md: 24, xl: 12 }}
									fieldProps={{
										showSearch: true,
										optionFilterProp: "searchText",
										options: permissionGroups,
										placeholder: t("system.menu.permissionCode"),
										allowClear: true,
									}}
								/>
							) : null}
							<ProFormRadio.Group
								name="status"
								label={t("common.status")}
								tooltip={t("system.menu.statusTooltip")}
								radioType="button"
								labelCol={{ span: 6 }}
								colProps={{ md: 24, xl: 12 }}
								options={[
									{ label: t("common.enabled"), value: 1 },
									{ label: t("common.deactivated"), value: 0 },
								]}
							/>
							<ProFormRadio.Group
								name="hidden"
								label={t("system.menu.hideInMenu")}
								tooltip={t("system.menu.hideInMenuTooltip")}
								radioType="button"
								labelCol={{ span: 6 }}
								colProps={{ md: 24, xl: 12 }}
								options={[
									{ label: t("common.yes"), value: true },
									{ label: t("common.no"), value: false },
								]}
							/>
							{currentNodeType === "menu" && normalizedComponentKey && !registeredComponentKeys.has(normalizedComponentKey) ? (
								<Alert
									className="mt-2"
									type="warning"
									showIcon
									message={t("system.menu.unregisteredComponentHint")}
									description={`当前组件 ${componentKey} 尚未出现在注册表中，保存后菜单可能无法正确跳转。`}
								/>
							) : null}
						</>
					);
				}}
			</ProFormDependency>
		</ModalForm>
	);
}
