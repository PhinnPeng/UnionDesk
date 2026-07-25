import type { DomainTicketType } from "@uniondesk/shared";
import {
	fetchDomainTicketTypes,
	publishDomainTicketTypeFormSchema,
	saveDomainTicketTypeFormSchemaDraft,
	toErrorMessage,
} from "@uniondesk/shared";

import { AuthGuarded } from "#src/components/auth-guarded";
import { BasicContent } from "#src/components/basic-content";
import { FormilyFormDesigner, FormilyFormDesignerFallback } from "#src/components/formily-form-designer";
import { mergeSystemFormSchema } from "#src/components/formily-form-designer/form-schema-utils";
import { appScopes } from "#src/router/extra-info/app-scope";
import { useTabsStore } from "#src/store/tabs";

import { ArrowLeftOutlined, HistoryOutlined } from "@ant-design/icons";
import { App, Button, Empty, Modal, Spin, Typography } from "antd";
import { lazy, Suspense, useCallback, useEffect, useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router";

import {
	PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_READ,
	PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_UPDATE,
} from "#src/pages/platform/domains/platform-domain-permissions";

import { FormSchemaVersionDrawer } from "./components/form-schema-version-drawer";

const { Title } = Typography;

const LazyFormilyFormDesigner = lazy(() => import("#src/components/formily-form-designer").then(module => ({
	default: module.FormilyFormDesigner,
})));

function buildFormDesignPath(domainId: string, typeId: string) {
	return `/platform/domains/ticket/form-design/${encodeURIComponent(domainId)}/${encodeURIComponent(typeId)}`;
}

function applyTicketTypeSchema(ticketType: DomainTicketType): Record<string, unknown> {
	return mergeSystemFormSchema(ticketType.form_schema_draft ?? ticketType.form_schema);
}

export default function TicketFormDesignPage() {
	const { message, modal } = App.useApp();
	const navigate = useNavigate();
	const { domainId: domainIdParam, typeId: typeIdParam } = useParams();
	const { setTableTitle, resetTableTitle } = useTabsStore();

	const domainId = domainIdParam?.trim() ?? "";
	const typeId = typeIdParam?.trim() ?? "";

	const [loading, setLoading] = useState(false);
	const [saving, setSaving] = useState(false);
	const [publishing, setPublishing] = useState(false);
	const [historyOpen, setHistoryOpen] = useState(false);
	const [ticketType, setTicketType] = useState<DomainTicketType | null>(null);
	const [formSchema, setFormSchema] = useState<Record<string, unknown>>(mergeSystemFormSchema(null));

	const pagePath = useMemo(() => {
		if (!domainId || !typeId) {
			return "";
		}
		return buildFormDesignPath(domainId, typeId);
	}, [domainId, typeId]);

	const backToTickets = useCallback(() => {
		if (!domainId) {
			navigate("/platform/domains");
			return;
		}
		navigate(`/platform/domains/detail/${encodeURIComponent(domainId)}?tab=tickets`);
	}, [domainId, navigate]);

	const loadTicketType = useCallback(async () => {
		if (!domainId || !typeId) {
			setTicketType(null);
			return;
		}
		setLoading(true);
		try {
			const list = await fetchDomainTicketTypes(domainId);
			const found = list.find(item => item.id === typeId) ?? null;
			setTicketType(found);
			if (found) {
				setFormSchema(applyTicketTypeSchema(found));
			}
			else {
				message.error("未找到该事项类型");
			}
		}
		catch (error) {
			setTicketType(null);
			message.error(toErrorMessage(error));
		}
		finally {
			setLoading(false);
		}
	}, [domainId, message, typeId]);

	useEffect(() => {
		void loadTicketType();
	}, [loadTicketType]);

	useEffect(() => {
		if (!pagePath || !ticketType?.name) {
			return;
		}
		setTableTitle(appScopes.platform, pagePath, `表单设计 - ${ticketType.name}`);
		return () => {
			resetTableTitle(appScopes.platform, pagePath);
		};
	}, [pagePath, resetTableTitle, setTableTitle, ticketType?.name]);

	const handleSaveDraft = async (schema: Record<string, unknown>) => {
		if (!domainId || !typeId) {
			return;
		}
		setSaving(true);
		try {
			const saved = await saveDomainTicketTypeFormSchemaDraft(domainId, typeId, schema);
			setTicketType(saved);
			setFormSchema(applyTicketTypeSchema(saved));
			message.success("草稿已保存");
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
		finally {
			setSaving(false);
		}
	};

	const publishSchema = async (schema: Record<string, unknown>) => {
		if (!domainId || !typeId) {
			return;
		}
		setPublishing(true);
		try {
			const saved = await publishDomainTicketTypeFormSchema(domainId, typeId, schema);
			setTicketType(saved);
			setFormSchema(applyTicketTypeSchema(saved));
			message.success("已发布");
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
		finally {
			setPublishing(false);
		}
	};

	const handlePublish = (schema: Record<string, unknown>) => {
		modal.confirm({
			title: "确认发布表单？",
			content: "发布后将立即对用户侧生效，草稿将与当前发布版本同步。",
			okText: "发布",
			cancelText: "取消",
			onOk: () => publishSchema(schema),
		});
	};

	const handleRollbackSuccess = async () => {
		await loadTicketType();
	};

	return (
		<BasicContent className="h-full overflow-auto bg-colorBgLayout">
			<AuthGuarded auth={PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_READ}>
				<div className="flex h-full flex-col gap-4">
					<div className="flex items-center justify-between gap-3">
						<div className="flex items-center gap-3">
							<Button type="text" icon={<ArrowLeftOutlined />} onClick={backToTickets}>
								返回事项列表
							</Button>
							<Title level={5} className="!mb-0">
								{ticketType ? `表单设计：${ticketType.name}` : "表单设计"}
							</Title>
						</div>
						{ticketType ? (
							<Button icon={<HistoryOutlined />} onClick={() => setHistoryOpen(true)}>
								历史版本
							</Button>
						) : null}
					</div>

					{!domainId || !typeId ? (
						<Empty description="缺少业务域或事项类型 ID" />
					) : loading ? (
						<div className="flex flex-1 justify-center py-16">
							<Spin />
						</div>
					) : !ticketType ? (
						<Empty description="未找到事项类型">
							<Button type="primary" onClick={backToTickets}>
								返回事项列表
							</Button>
						</Empty>
					) : (
						<div className="min-h-0 flex-1">
							<AuthGuarded auth={PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_UPDATE} fallback={(
								<Empty description="无编辑权限" />
							)}
							>
								<Suspense fallback={<FormilyFormDesignerFallback />}>
									<LazyFormilyFormDesigner
										key={ticketType.id}
										value={formSchema}
										onChange={setFormSchema}
										onSaveDraft={handleSaveDraft}
										onPublish={handlePublish}
										saving={saving}
										publishing={publishing}
										hint="系统字段「标题」「详细描述」为必填且不可删除；保存写入草稿，发布后用户侧生效。"
									/>
								</Suspense>
							</AuthGuarded>
						</div>
					)}
				</div>

				{domainId && typeId ? (
					<FormSchemaVersionDrawer
						open={historyOpen}
						domainId={domainId}
						typeId={typeId}
						onClose={() => setHistoryOpen(false)}
						onRollbackSuccess={() => void handleRollbackSuccess()}
					/>
				) : null}
			</AuthGuarded>
		</BasicContent>
	);
}

export { buildFormDesignPath };
