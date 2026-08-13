import type { CreatePlatformTicketTypeBody, PlatformTicketType, UpdatePlatformTicketTypeBody } from "@uniondesk/shared";
import {
	createPlatformTicketType,
	deletePlatformTicketType,
	fetchPlatformTicketTypes,
	reorderPlatformTicketTypes,
	toErrorMessage,
	updatePlatformTicketType,
} from "@uniondesk/shared";

import { AuthGuarded } from "#src/components/auth-guarded";
import { TableSearchForm } from "#src/components/table-search-form";
import { useAuth } from "#src/hooks/use-auth";
import {
	PLATFORM_TICKET_CONFIG_TYPE_CREATE,
	PLATFORM_TICKET_CONFIG_TYPE_DELETE,
	PLATFORM_TICKET_CONFIG_TYPE_READ,
	PLATFORM_TICKET_CONFIG_TYPE_UPDATE,
} from "#src/pages/platform/domains/platform-domain-permissions";

import { TicketTypeFormModal } from "./components/ticket-type-form-modal";
import { CreateTicketTypeDropdown } from "./components/create-ticket-type-dropdown";
import { TicketTypeSortableTable } from "./components/ticket-type-sortable-table";
import type { TicketTypeTemplateKey } from "./components/ticket-type-utils";
import { navigatePlatformTicketTypeConfig } from "./open-config-tab";

import { ReloadOutlined, SearchOutlined } from "@ant-design/icons";
import { App, Button, Card, Empty, Form, Input, Space, Typography } from "antd";
import { useCallback, useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router";

import "./ticket-types-panel.less";

interface TypesSearchValues {
	keyword?: string;
}

export interface TicketTypesPanelProps {
	onAttributeEdit?: (record: PlatformTicketType) => void;
	onWorkflowEdit?: (record: PlatformTicketType) => void;
}

export function TicketTypesPanel({ onAttributeEdit, onWorkflowEdit }: TicketTypesPanelProps = {}) {
	const { message, modal } = App.useApp();
	const navigate = useNavigate();
	const [searchParams, setSearchParams] = useSearchParams();
	const { hasPermission } = useAuth();

	const [loading, setLoading] = useState(false);
	const [submitting, setSubmitting] = useState(false);
	const [rows, setRows] = useState<PlatformTicketType[]>([]);
	const [total, setTotal] = useState(0);
	const [page, setPage] = useState(1);
	const [pageSize, setPageSize] = useState(20);
	const [keyword, setKeyword] = useState("");
	const [modalOpen, setModalOpen] = useState(false);
	const [editing, setEditing] = useState<PlatformTicketType | null>(null);
	const [templateKey, setTemplateKey] = useState<TicketTypeTemplateKey | null>(null);
	const [copyFrom, setCopyFrom] = useState<PlatformTicketType | null>(null);
	const [createDropdownOpen, setCreateDropdownOpen] = useState(false);

	/** 跨页「新建事项类型」：?action=create → 展开创建下拉后清除参数 */
	useEffect(() => {
		if (searchParams.get("action") !== "create") {
			return;
		}
		setCreateDropdownOpen(true);
		setSearchParams((prev) => {
			const next = new URLSearchParams(prev);
			next.delete("action");
			return next;
		}, { replace: true });
	}, [searchParams, setSearchParams]);

	const loadTypes = useCallback(async (nextPage = page, nextPageSize = pageSize, nextKeyword = keyword) => {
		setLoading(true);
		try {
			const params = {
				keyword: nextKeyword.trim() || undefined,
				...(total > 100 || nextPage > 1 ? { page: nextPage, page_size: nextPageSize } : {}),
			};
			const result = await fetchPlatformTicketTypes(params);
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
	}, [keyword, message, page, pageSize, total]);

	useEffect(() => {
		void loadTypes(1, 20, "");
	// eslint-disable-next-line react-hooks/exhaustive-deps -- 初始化加载
	}, []);

	const handleSearch = useCallback((values: TypesSearchValues) => {
		void loadTypes(1, pageSize, values.keyword ?? "");
	}, [loadTypes, pageSize]);

	const handleResetSearch = useCallback(() => {
		void loadTypes(1, pageSize, "");
	}, [loadTypes, pageSize]);

	const handleReorder = async (nextRows: PlatformTicketType[]) => {
		setRows(nextRows);
		try {
			const orders = nextRows.map((item, index) => ({
				id: Number(item.id),
				sort_order: index,
			}));
			await reorderPlatformTicketTypes(orders);
			message.success("排序已保存");
		}
		catch (error) {
			message.error(toErrorMessage(error));
			void loadTypes();
		}
	};

	const handleOpenCreate = (key: TicketTypeTemplateKey) => {
		setEditing(null);
		setTemplateKey(key);
		setCopyFrom(null);
		setModalOpen(true);
	};

	const handleOpenEdit = (record: PlatformTicketType) => {
		setEditing(record);
		setTemplateKey(null);
		setCopyFrom(null);
		setModalOpen(true);
	};

	const handleSubmit = async (values: { name: string; icon: string; description?: string }) => {
		setSubmitting(true);
		try {
			if (editing) {
				const body: UpdatePlatformTicketTypeBody = {
					name: values.name.trim(),
					icon: values.icon.trim(),
					description: values.description?.trim() ?? "",
				};
				await updatePlatformTicketType(editing.id, body);
				message.success("事项类型已更新");
			}
			else {
				const body: CreatePlatformTicketTypeBody = {
					name: values.name.trim(),
					icon: values.icon.trim(),
					description: values.description?.trim() || undefined,
					template_key: templateKey ?? undefined,
				};
				const created = await createPlatformTicketType(body);
				message.success("事项类型已创建");
				modal.confirm({
					title: "是否进入配置？",
					content: templateKey === "simple_ticket"
						? "创建成功。该类型已自动关联系统属性「描述」（必填），可继续配置工作流或添加自定义属性。"
						: templateKey === "standard_ticket"
							? "创建成功。该类型已自动关联系统属性「标题」「描述」（均必填），可继续配置工作流或添加自定义属性。"
							: "创建成功，可立即配置该类型的属性、工作流与描述模板。",
					okText: "进入配置",
					cancelText: "留在列表",
					onOk: () => navigatePlatformTicketTypeConfig(navigate, created, "attributes"),
				});
			}
			setModalOpen(false);
			setEditing(null);
			setTemplateKey(null);
			setCopyFrom(null);
			await loadTypes();
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
		finally {
			setSubmitting(false);
		}
	};

	const handleStatusToggle = async (record: PlatformTicketType) => {
		const nextStatus = record.status === "active" ? "disabled" : "active";
		const doToggle = async () => {
			try {
				await updatePlatformTicketType(record.id, { status: nextStatus });
				message.success(nextStatus === "active" ? "事项类型已启用" : "事项类型已停用");
				await loadTypes();
			}
			catch (error) {
				message.error(toErrorMessage(error));
			}
		};
		if (nextStatus === "disabled") {
			modal.confirm({
				title: "确认停用该事项类型？",
				content: `事项类型「${record.name}」停用后将无法在业务域中使用。`,
				okText: "确认停用",
				cancelText: "取消",
				onOk: doToggle,
			});
		}
		else {
			void doToggle();
		}
	};

	const handleCopy = (record: PlatformTicketType) => {
		setEditing(null);
		setTemplateKey(null);
		setCopyFrom(record);
		setModalOpen(true);
	};

	const handleDelete = (record: PlatformTicketType) => {
		modal.confirm({
			title: "确认删除该事项类型？",
			content: `事项类型「${record.name}」删除后不可恢复。`,
			okText: "确认删除",
			cancelText: "取消",
			okButtonProps: { danger: true },
			onOk: async () => {
				try {
					await deletePlatformTicketType(record.id);
					message.success("事项类型已删除");
					await loadTypes();
				}
				catch (error) {
					message.error(toErrorMessage(error));
				}
			},
		});
	};

	return (
		<AuthGuarded auth={PLATFORM_TICKET_CONFIG_TYPE_READ} fallback={<Empty description="无权限查看事项类型" className="py-16" />}>
			<div className="flex flex-col gap-4">
				<div className="ticket-types-panel__header">
					<Typography.Title level={4} className="ticket-types-panel__title">事项类型</Typography.Title>
					<Typography.Paragraph className="ticket-types-panel__desc">
						事项类型可被添加到业务域中；修改全局类型不影响已复制到域内的类型。
					</Typography.Paragraph>
				</div>
				<Card
					bordered={false}
					className="ticket-types-panel__search-card"
					styles={{ body: { paddingBlock: 12 } }}
					title={<><SearchOutlined className="mr-2" />筛选条件</>}
				>
					<TableSearchForm<TypesSearchValues>
						loading={loading}
						collapseRender={false}
						span={{ xs: 24, sm: 12, md: 8, lg: 6, xl: 6, xxl: 4 }}
						initialValues={{ keyword: "" }}
						onFinish={handleSearch}
						onReset={handleResetSearch}
					>
						<Form.Item name="keyword" label="关键字">
							<Input allowClear placeholder="名称、描述或编码" disabled={loading} />
						</Form.Item>
					</TableSearchForm>
				</Card>
				<Card
					bordered={false}
					title="事项类型列表"
					extra={(
						<Space>
							<Button icon={<ReloadOutlined />} onClick={() => void loadTypes()} loading={loading}>
								刷新
							</Button>
							<AuthGuarded auth={PLATFORM_TICKET_CONFIG_TYPE_CREATE}>
								<CreateTicketTypeDropdown
									open={createDropdownOpen}
									onOpenChange={setCreateDropdownOpen}
									onSelect={handleOpenCreate}
								/>
							</AuthGuarded>
						</Space>
					)}
				>
					<TicketTypeSortableTable
						loading={loading}
						dataSource={rows}
						total={total}
						page={page}
						pageSize={pageSize}
						canUpdate={hasPermission(PLATFORM_TICKET_CONFIG_TYPE_UPDATE)}
						canDelete={hasPermission(PLATFORM_TICKET_CONFIG_TYPE_DELETE)}
						onPageChange={(nextPage, nextPageSize) => void loadTypes(nextPage, nextPageSize, keyword)}
						onReorder={handleReorder}
						onEdit={handleOpenEdit}
						onDelete={handleDelete}
						onStatusToggle={record => void handleStatusToggle(record)}
						onAttributeEdit={onAttributeEdit ?? (() => {})}
						onWorkflowEdit={onWorkflowEdit ?? (() => {})}
						onCopy={record => void handleCopy(record)}
					/>
				</Card>
				<TicketTypeFormModal
					open={modalOpen}
					loading={submitting}
					editing={editing}
					templateKey={templateKey}
					copyFrom={copyFrom}
					onCancel={() => {
						setModalOpen(false);
						setEditing(null);
						setTemplateKey(null);
						setCopyFrom(null);
					}}
					onSubmit={handleSubmit}
				/>
			</div>
		</AuthGuarded>
	);
}
