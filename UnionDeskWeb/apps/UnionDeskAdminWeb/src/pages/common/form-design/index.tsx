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
import { appScopes } from "#src/router/extra-info/app-scope";
import { useTabsStore } from "#src/store/tabs";

import { ArrowLeftOutlined } from "@ant-design/icons";
import { App, Button, Empty, Spin, Typography } from "antd";
import { lazy, Suspense, useCallback, useEffect, useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router";

import { mergeSystemFormSchema } from "#src/pages/platform/domains/detail/components/ticket-type-form-defaults";
import {
	PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_READ,
	PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_UPDATE,
} from "#src/pages/platform/domains/platform-domain-permissions";

const { Title } = Typography;

const LazyFormilyFormDesigner = lazy(() => import("#src/components/formily-form-designer").then(module => ({
	default: module.FormilyFormDesigner,
})));

function buildFormDesignPath(domainId: string, typeId: string) {
	return `/platform/domains/ticket/form-design/${encodeURIComponent(domainId)}/${encodeURIComponent(typeId)}`;
}

export default function TicketFormDesignPage() {
	const { message } = App.useApp();
	const navigate = useNavigate();
	const { domainId: domainIdParam, typeId: typeIdParam } = useParams();
	const { setTableTitle, resetTableTitle } = useTabsStore();

	const domainId = domainIdParam?.trim() ?? "";
	const typeId = typeIdParam?.trim() ?? "";

	const [loading, setLoading] = useState(false);
	const [saving, setSaving] = useState(false);
	const [publishing, setPublishing] = useState(false);
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
				setFormSchema(mergeSystemFormSchema(found.form_schema_draft ?? found.form_schema));
			}
			else {
				message.error("未找到该工单类型");
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
			setFormSchema(mergeSystemFormSchema(saved.form_schema_draft ?? saved.form_schema));
			message.success("草稿已保存");
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
		finally {
			setSaving(false);
		}
	};

	const handlePublish = async (schema: Record<string, unknown>) => {
		if (!domainId || !typeId) {
			return;
		}
		setPublishing(true);
		try {
			await saveDomainTicketTypeFormSchemaDraft(domainId, typeId, schema);
			const saved = await publishDomainTicketTypeFormSchema(domainId, typeId);
			setTicketType(saved);
			setFormSchema(mergeSystemFormSchema(saved.form_schema_draft ?? saved.form_schema));
			message.success("已发布");
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
		finally {
			setPublishing(false);
		}
	};

	return (
		<BasicContent className="h-full overflow-auto bg-colorBgLayout">
			<AuthGuarded auth={PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_READ}>
				<div className="flex h-full flex-col gap-4">
					<div className="flex items-center gap-3">
						<Button type="text" icon={<ArrowLeftOutlined />} onClick={backToTickets}>
							返回工单列表
						</Button>
						<Title level={5} className="!mb-0">
							{ticketType ? `表单设计：${ticketType.name}` : "表单设计"}
						</Title>
					</div>

					{!domainId || !typeId ? (
						<Empty description="缺少业务域或工单类型 ID" />
					) : loading ? (
						<div className="flex flex-1 justify-center py-16">
							<Spin />
						</div>
					) : !ticketType ? (
						<Empty description="未找到工单类型">
							<Button type="primary" onClick={backToTickets}>
								返回工单列表
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
			</AuthGuarded>
		</BasicContent>
	);
}

export { buildFormDesignPath };
