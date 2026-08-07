import type { PlatformTicketType, TeamTemplate, TeamTemplateItem, TeamTemplateItemBody } from "@uniondesk/shared";
import { fetchPlatformTicketTypes, toErrorMessage, updateTeamTemplate } from "@uniondesk/shared";

import { AuthGuarded } from "#src/components/auth-guarded";
import { PLATFORM_TICKET_CONFIG_TEMPLATE_UPDATE } from "#src/pages/platform/domains/platform-domain-permissions";
import { buildTicketConfigPath } from "#src/pages/platform/ticket-config/ticket-config-path";

import { DeleteOutlined, EllipsisOutlined, NodeIndexOutlined, PlusOutlined, SettingOutlined } from "@ant-design/icons";
import { App, Button, Dropdown, Space, Table, Tooltip, Typography } from "antd";
import type { MenuProps, TableColumnsType } from "antd";
import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router";

import { AddTicketTypeModal } from "./add-ticket-type-modal";

function toItemBodies(items: TeamTemplateItem[]): TeamTemplateItemBody[] {
	return items.map((item, index) => ({
		ticket_type_id: Number(item.ticket_type_id),
		sort_order: item.sort_order ?? index,
		include_form_schema: item.include_form_schema,
		include_workflow: item.include_workflow,
		include_description_template: item.include_description_template,
	}));
}

export interface CollaborationPanelProps {
	template: TeamTemplate;
	onUpdated: (next: TeamTemplate) => void;
}

export function CollaborationPanel({ template, onUpdated }: CollaborationPanelProps) {
	const { message, modal } = App.useApp();
	const navigate = useNavigate();
	const [addOpen, setAddOpen] = useState(false);
	const [submitting, setSubmitting] = useState(false);
	const [typeDescById, setTypeDescById] = useState<Map<string, string>>(new Map());

	useEffect(() => {
		void fetchPlatformTicketTypes()
			.then((result) => {
				const map = new Map<string, string>();
				for (const type of result.items) {
					map.set(type.id, type.description ?? "");
				}
				setTypeDescById(map);
			})
			.catch(() => {
				/* 描述列降级为 — */
			});
	}, [template.items]);

	const excludeTypeIds = useMemo(
		() => new Set((template.items ?? []).map(item => item.ticket_type_id)),
		[template.items],
	);

	const persistItems = async (items: TeamTemplateItemBody[]) => {
		const next = await updateTeamTemplate(template.id, {
			name: template.name,
			description: template.description ?? "",
			items,
		});
		onUpdated(next);
		return next;
	};

	const handleAdd = async (types: PlatformTicketType[]) => {
		setSubmitting(true);
		try {
			const existing = toItemBodies(template.items ?? []);
			const appended = types.map((type, index) => ({
				ticket_type_id: Number(type.id),
				sort_order: existing.length + index,
				include_form_schema: true,
				include_workflow: true,
				include_description_template: true,
			}));
			await persistItems([...existing, ...appended]);
			message.success("事项类型已添加");
			setAddOpen(false);
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
		finally {
			setSubmitting(false);
		}
	};

	const handleRemove = (record: TeamTemplateItem) => {
		modal.confirm({
			title: "确认移除此事项类型？",
			content: "仅从本模板解除关联，不会删除全局事项类型。",
			okText: "移除",
			okButtonProps: { danger: true },
			cancelText: "取消",
			onOk: async () => {
				try {
					const nextItems = toItemBodies(
						(template.items ?? []).filter(item => item.ticket_type_id !== record.ticket_type_id),
					);
					await persistItems(nextItems);
					message.success("已移除");
				}
				catch (error) {
					message.error(toErrorMessage(error));
					throw error;
				}
			},
		});
	};

	const openTypeConfig = (ticketTypeId: string, tab: "attributes" | "workflow") => {
		navigate(buildTicketConfigPath({ section: "types", typeId: ticketTypeId, tab }));
	};

	const columns = useMemo<TableColumnsType<TeamTemplateItem>>(() => [
		{
			title: "事项类型名称",
			dataIndex: "ticket_type_name",
			key: "ticket_type_name",
			align: "center",
			ellipsis: true,
			render: (value, record) => value || record.ticket_type_code || record.ticket_type_id,
		},
		{
			title: "描述",
			key: "description",
			align: "center",
			ellipsis: true,
			render: (_value, record) => typeDescById.get(record.ticket_type_id) || "—",
		},
		{
			title: "操作",
			key: "actions",
			width: 200,
			align: "center",
			render: (_value, record) => {
				const moreItems: MenuProps["items"] = [
					{
						key: "remove",
						danger: true,
						label: "移除",
						icon: <DeleteOutlined />,
						onClick: () => handleRemove(record),
					},
				];
				return (
					<div className="flex justify-center">
						<Space size={4}>
							<Tooltip title="属性">
								<Button
									type="text"
									size="small"
									icon={<SettingOutlined />}
									onClick={() => openTypeConfig(record.ticket_type_id, "attributes")}
								/>
							</Tooltip>
							<Tooltip title="工作流">
								<Button
									type="text"
									size="small"
									icon={<NodeIndexOutlined />}
									onClick={() => openTypeConfig(record.ticket_type_id, "workflow")}
								/>
							</Tooltip>
							<AuthGuarded auth={PLATFORM_TICKET_CONFIG_TEMPLATE_UPDATE} fallback={null}>
								<Dropdown menu={{ items: moreItems }} trigger={["click"]}>
									<Tooltip title="更多">
										<Button type="text" size="small" icon={<EllipsisOutlined />} aria-label="更多" />
									</Tooltip>
								</Dropdown>
							</AuthGuarded>
						</Space>
					</div>
				);
			},
		},
	], [navigate, typeDescById]);

	return (
		<div>
			<div className="team-template-config__collab-header">
				<Typography.Title level={4} className="team-template-config__collab-title">
					协作配置
				</Typography.Title>
				<Typography.Paragraph type="secondary" style={{ marginBottom: 0 }}>
					管理事项类型的相关配置
				</Typography.Paragraph>
			</div>

			<div className="team-template-config__type-bar">
				<h3 className="team-template-config__type-bar-title">事项类型</h3>
				<AuthGuarded auth={PLATFORM_TICKET_CONFIG_TEMPLATE_UPDATE} fallback={null}>
					<Button
						type="primary"
						shape="circle"
						icon={<PlusOutlined />}
						className="team-template-config__add-btn"
						aria-label="添加事项类型"
						onClick={() => setAddOpen(true)}
					/>
				</AuthGuarded>
			</div>

			<Table
				rowKey="id"
				columns={columns}
				dataSource={template.items ?? []}
				pagination={false}
				locale={{ emptyText: "暂无事项类型，请点击右上角添加" }}
			/>

			<AddTicketTypeModal
				open={addOpen}
				excludeTypeIds={excludeTypeIds}
				submitting={submitting}
				onCancel={() => setAddOpen(false)}
				onAdd={handleAdd}
			/>
		</div>
	);
}
