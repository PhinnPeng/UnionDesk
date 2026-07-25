import type {
	CreateTicketAttributeBody,
	TicketAttribute,
	UpdateTicketAttributeBody,
} from "@uniondesk/shared";
import {
	createDomainTicketAttribute,
	createPlatformTicketAttribute,
	deleteDomainTicketAttribute,
	deletePlatformTicketAttribute,
	fetchDomainTicketAttributes,
	fetchPlatformTicketAttributes,
	reorderDomainTicketAttributes,
	reorderPlatformTicketAttributes,
	toErrorMessage,
	updateDomainTicketAttribute,
	updatePlatformTicketAttribute,
} from "@uniondesk/shared";

import { AuthGuarded } from "#src/components/auth-guarded";
import { TableSearchForm } from "#src/components/table-search-form";
import { useAuth } from "#src/hooks/use-auth";
import {
	PLATFORM_DOMAIN_CONTROL_TICKET_ATTRIBUTE_CREATE,
	PLATFORM_DOMAIN_CONTROL_TICKET_ATTRIBUTE_DELETE,
	PLATFORM_DOMAIN_CONTROL_TICKET_ATTRIBUTE_READ,
	PLATFORM_DOMAIN_CONTROL_TICKET_ATTRIBUTE_UPDATE,
	PLATFORM_TICKET_CONFIG_ATTR_CREATE,
	PLATFORM_TICKET_CONFIG_ATTR_DELETE,
	PLATFORM_TICKET_CONFIG_ATTR_READ,
	PLATFORM_TICKET_CONFIG_ATTR_UPDATE,
} from "#src/pages/platform/domains/platform-domain-permissions";

import { AttributeFormModal } from "./components/attribute-form-modal";
import { AttributeSortableTable } from "./components/attribute-sortable-table";

import { PlusOutlined, ReloadOutlined, SearchOutlined } from "@ant-design/icons";
import { App, Button, Card, Empty, Form, Input, Space, Typography } from "antd";
import { useCallback, useEffect, useState } from "react";

import "./ticket-attributes-panel.less";

export interface TicketAttributesPanelProps {
	scope: "platform" | "domain";
	domainId?: string;
}

interface AttributesSearchValues {
	keyword?: string;
}

export function TicketAttributesPanel({ scope, domainId }: TicketAttributesPanelProps) {
	const { message, modal } = App.useApp();
	const { hasPermission } = useAuth();
	const readPerm = scope === "platform"
		? PLATFORM_TICKET_CONFIG_ATTR_READ
		: PLATFORM_DOMAIN_CONTROL_TICKET_ATTRIBUTE_READ;
	const createPerm = scope === "platform"
		? PLATFORM_TICKET_CONFIG_ATTR_CREATE
		: PLATFORM_DOMAIN_CONTROL_TICKET_ATTRIBUTE_CREATE;
	const updatePerm = scope === "platform"
		? PLATFORM_TICKET_CONFIG_ATTR_UPDATE
		: PLATFORM_DOMAIN_CONTROL_TICKET_ATTRIBUTE_UPDATE;
	const deletePerm = scope === "platform"
		? PLATFORM_TICKET_CONFIG_ATTR_DELETE
		: PLATFORM_DOMAIN_CONTROL_TICKET_ATTRIBUTE_DELETE;

	const [loading, setLoading] = useState(false);
	const [submitting, setSubmitting] = useState(false);
	const [rows, setRows] = useState<TicketAttribute[]>([]);
	const [total, setTotal] = useState(0);
	const [page, setPage] = useState(1);
	const [pageSize, setPageSize] = useState(20);
	const [keyword, setKeyword] = useState("");
	const [modalOpen, setModalOpen] = useState(false);
	const [editing, setEditing] = useState<TicketAttribute | null>(null);

	const loadAttributes = useCallback(async (nextPage = page, nextPageSize = pageSize, nextKeyword = keyword) => {
		if (scope === "domain" && !domainId) {
			setRows([]);
			setTotal(0);
			return;
		}
		setLoading(true);
		try {
			const params = {
				keyword: nextKeyword.trim() || undefined,
				...(total > 100 || nextPage > 1 ? { page: nextPage, page_size: nextPageSize } : {}),
			};
			const result = scope === "platform"
				? await fetchPlatformTicketAttributes(params)
				: await fetchDomainTicketAttributes(domainId!, params);
			setRows(result.items);
			setTotal(result.total);
			setPage(nextPage);
			setPageSize(nextPageSize);
			setKeyword(nextKeyword);
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
		finally {
			setLoading(false);
		}
	}, [domainId, keyword, message, page, pageSize, scope, total]);

	useEffect(() => {
		void loadAttributes(1, 20, "");
	// eslint-disable-next-line react-hooks/exhaustive-deps -- scope/domain 变化时初始化
	}, [scope, domainId]);

	const handleSearch = useCallback((values: AttributesSearchValues) => {
		void loadAttributes(1, pageSize, values.keyword ?? "");
	}, [loadAttributes, pageSize]);

	const handleResetSearch = useCallback(() => {
		void loadAttributes(1, pageSize, "");
	}, [loadAttributes, pageSize]);

	const handleReorder = async (nextRows: TicketAttribute[]) => {
		setRows(nextRows);
		try {
			const orders = nextRows.map((item, index) => ({
				id: Number(item.id),
				sort_order: index,
			}));
			if (scope === "platform") {
				await reorderPlatformTicketAttributes(orders);
			}
			else {
				await reorderDomainTicketAttributes(domainId!, orders);
			}
			// 静默保存，不显示通知
		}
		catch (error) {
			message.error(toErrorMessage(error));
			void loadAttributes();
		}
	};

	const handleSubmit = async (body: CreateTicketAttributeBody | UpdateTicketAttributeBody) => {
		setSubmitting(true);
		try {
			if (editing) {
				if (scope === "platform") {
					await updatePlatformTicketAttribute(editing.id, body);
				}
				else {
					await updateDomainTicketAttribute(domainId!, editing.id, body);
				}
				message.success("属性已更新");
			}
			else {
				if (scope === "platform") {
					await createPlatformTicketAttribute(body as CreateTicketAttributeBody);
				}
				else {
					await createDomainTicketAttribute(domainId!, body as CreateTicketAttributeBody);
				}
				message.success("属性已创建");
			}
			setModalOpen(false);
			setEditing(null);
			await loadAttributes();
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
		finally {
			setSubmitting(false);
		}
	};

	const handleStatusToggle = async (record: TicketAttribute) => {
		const nextStatus = record.status === "active" ? "disabled" : "active";
		const doToggle = async () => {
			try {
				const body: UpdateTicketAttributeBody = { status: nextStatus };
				if (scope === "platform") {
					await updatePlatformTicketAttribute(record.id, body);
				}
				else {
					await updateDomainTicketAttribute(domainId!, record.id, body);
				}
				message.success(nextStatus === "active" ? "属性已启用" : "属性已停用");
				await loadAttributes();
			}
			catch (error) {
				message.error(toErrorMessage(error));
			}
		};
		if (nextStatus === "disabled") {
			modal.confirm({
				title: "确认停用该属性？",
				content: `属性「${record.name}」停用后将无法在事项中使用。`,
				okText: "确认停用",
				cancelText: "取消",
				onOk: doToggle,
			});
		}
		else {
			void doToggle();
		}
	};

	const handleDelete = async (record: TicketAttribute) => {
		try {
			if (scope === "platform") {
				await deletePlatformTicketAttribute(record.id);
			}
			else {
				await deleteDomainTicketAttribute(domainId!, record.id);
			}
			message.success("属性已删除");
			await loadAttributes();
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
	};

	return (
		<AuthGuarded auth={readPerm} fallback={<Empty description="无权限查看事项属性" className="py-16" />}>
			<div className="flex flex-col gap-4">
				<Card
					bordered={false}
					className="ticket-attributes-panel__search-card"
					styles={{ body: { paddingBlock: 12 } }}
					title={<><SearchOutlined className="mr-2" />筛选条件</>}
				>
					<TableSearchForm<AttributesSearchValues>
						loading={loading}
						collapseRender={false}
						span={{ xs: 24, sm: 12, md: 8, lg: 6, xl: 6, xxl: 4 }}
						initialValues={{ keyword: "" }}
						onFinish={handleSearch}
						onReset={handleResetSearch}
					>
						<Form.Item name="keyword" label="关键字">
							<Input allowClear placeholder="名称或描述" disabled={loading} />
						</Form.Item>
					</TableSearchForm>
				</Card>
				<Card
					bordered={false}
					title="事项属性列表"
					extra={(
						<Space>
							<Button icon={<ReloadOutlined />} onClick={() => void loadAttributes()}>刷新</Button>
							<AuthGuarded auth={createPerm} fallback={null}>
								<Button type="primary" icon={<PlusOutlined />} onClick={() => {
									setEditing(null);
									setModalOpen(true);
								}}
								>
									新建
								</Button>
							</AuthGuarded>
						</Space>
					)}
				>
					<AttributeSortableTable
						loading={loading}
						dataSource={rows}
						total={total}
						page={page}
						pageSize={pageSize}
					canUpdate={hasPermission(updatePerm)}
					canDelete={hasPermission(deletePerm)}
					onPageChange={(nextPage, nextPageSize) => void loadAttributes(nextPage, nextPageSize, keyword)}
					onReorder={orders => void handleReorder(orders)}
					onEdit={(record) => {
						setEditing(record);
						setModalOpen(true);
					}}
					onDelete={record => void handleDelete(record)}
					onStatusToggle={record => void handleStatusToggle(record)}
					/>
				</Card>
				<AttributeFormModal
					open={modalOpen}
					editing={editing}
					submitting={submitting}
					onCancel={() => {
						setModalOpen(false);
						setEditing(null);
					}}
					onSubmit={handleSubmit}
				/>
			</div>
		</AuthGuarded>
	);
}
