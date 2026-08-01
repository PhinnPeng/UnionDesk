import type { AdminDomain } from "@uniondesk/shared";
import { fetchAdminDomain, toErrorMessage, updateAdminDomain } from "@uniondesk/shared";

import { AuthGuarded } from "#src/components/auth-guarded";
import { BasicContent } from "#src/components/basic-content";
import { useAuth } from "#src/hooks/use-auth";
import {
	DOMAIN_GENERAL_READ,
	DOMAIN_GENERAL_UPDATE,
} from "#src/pages/domain/domain-permissions";
import { DomainLogoField } from "#src/pages/platform/domains/components/domains-modal";
import { useAuthStore } from "#src/store/auth";

import { App, Button, Card, Empty, Form, Input, Spin, Tooltip } from "antd";
import { useEffect, useMemo, useState } from "react";

import styles from "./index.module.less";

const DEFAULT_DOMAIN_LOGO = "/default-domain-logo.svg";

type BasicInfoFormValues = {
	name: string;
	logo: string;
	description: string;
	code: string;
	portal_url: string;
};

function resolveBusinessDomainId(
	defaultBusinessDomainId: number,
	accessibleDomains: Array<{ id: number }>,
): string {
	if (defaultBusinessDomainId > 0) {
		return String(defaultBusinessDomainId);
	}
	const first = accessibleDomains[0];
	return first ? String(first.id) : "";
}

function resolveNumericDomainId(domainId: string): number | null {
	const trimmed = domainId.trim();
	if (!trimmed || !/^\d+$/.test(trimmed)) {
		return null;
	}
	const numeric = Number(trimmed);
	return Number.isSafeInteger(numeric) ? numeric : null;
}

function derivePortalHost(code: string): string {
	const trimmed = code.trim();
	return trimmed ? `${trimmed}.uniondesk.com` : "";
}

function buildFormValues(domain: AdminDomain): BasicInfoFormValues {
	return {
		name: domain.name,
		logo: domain.logo?.trim() || DEFAULT_DOMAIN_LOGO,
		description: domain.description ?? "",
		code: domain.code,
		portal_url: derivePortalHost(domain.code),
	};
}

export default function DomainBasicPage() {
	const { message, modal } = App.useApp();
	const { hasPermission } = useAuth();
	const [form] = Form.useForm<BasicInfoFormValues>();
	const defaultBusinessDomainId = useAuthStore(state => state.defaultBusinessDomainId);
	const accessibleDomains = useAuthStore(state => state.accessibleDomains);

	const domainId = useMemo(
		() => resolveBusinessDomainId(defaultBusinessDomainId, accessibleDomains ?? []),
		[accessibleDomains, defaultBusinessDomainId],
	);

	const [loading, setLoading] = useState(false);
	const [submitting, setSubmitting] = useState(false);
	const [domain, setDomain] = useState<AdminDomain | null>(null);

	const canUpdate = hasPermission(DOMAIN_GENERAL_UPDATE);

	useEffect(() => {
		if (!domainId) {
			setDomain(null);
			return;
		}
		let cancelled = false;
		setLoading(true);
		void (async () => {
			try {
				const data = await fetchAdminDomain(domainId);
				if (!cancelled) {
					setDomain(data);
					form.setFieldsValue(buildFormValues(data));
					// 表单读实时域详情，侧栏读登录快照；进入本页时对齐侧栏展示名，避免改名后仍显示旧名
					const numericId = resolveNumericDomainId(data.id);
					if (numericId != null) {
						useAuthStore.getState().patchAccessibleDomain({
							id: numericId,
							name: data.name,
							code: data.code,
						});
					}
				}
			}
			catch (error) {
				if (!cancelled) {
					setDomain(null);
					message.error(toErrorMessage(error));
				}
			}
			finally {
				if (!cancelled) {
					setLoading(false);
				}
			}
		})();
		return () => {
			cancelled = true;
		};
	}, [domainId, form, message]);

	const handleSave = async () => {
		if (!domain) {
			return;
		}
		const values = await form.validateFields().catch(() => null);
		if (!values) {
			return;
		}
		const logo = values.logo?.trim() || DEFAULT_DOMAIN_LOGO;
		setSubmitting(true);
		try {
			const updated = await updateAdminDomain(domain.id, {
				name: values.name.trim(),
				logo,
				description: values.description?.trim() || undefined,
			});
			message.success("已更新业务信息");
			setDomain(updated);
			form.setFieldsValue(buildFormValues(updated));
			const numericId = resolveNumericDomainId(updated.id);
			if (numericId != null) {
				useAuthStore.getState().patchAccessibleDomain({
					id: numericId,
					name: updated.name,
					code: updated.code,
				});
			}
		}
		catch (error) {
			message.error(toErrorMessage(error));
			throw error;
		}
		finally {
			setSubmitting(false);
		}
	};

	const handleUpdateClick = () => {
		if (!canUpdate) {
			return;
		}
		modal.confirm({
			title: "确认更新业务信息",
			content: "确定要保存对本域基础信息的修改吗？",
			okText: "确定更新",
			cancelText: "取消",
			onOk: () => handleSave(),
		});
	};

	const formKey = domain ? `${domain.id}-${domain.updated_at ?? "0"}` : "empty";
	const initialValues = useMemo(
		() => (domain ? buildFormValues(domain) : undefined),
		[domain],
	);

	const saveButton = (
		<Tooltip title={canUpdate ? undefined : "无编辑权限"}>
			<Button
				type="primary"
				disabled={!canUpdate}
				loading={submitting}
				onClick={handleUpdateClick}
			>
				保存更改
			</Button>
		</Tooltip>
	);

	return (
		<BasicContent>
			<AuthGuarded
				auth={DOMAIN_GENERAL_READ}
				fallback={<Empty description="无权限查看通用设置" className="py-16" />}
			>
				{!domainId ? (
					<Empty description="暂无可用业务域" className="py-16" />
				) : loading ? (
					<div className={styles.stateCenter}>
						<Spin />
					</div>
				) : !domain ? (
					<Empty description="业务域信息加载失败" className="py-16" />
				) : (
					<div className={styles.page}>
						<header className={styles.pageHeader}>
							<h1 className={styles.pageTitle}>通用设置</h1>
							<p className={styles.pageDesc}>
								这些信息将展示在门户与侧栏等公开位置。
							</p>
						</header>

						<Form
							key={formKey}
							form={form}
							layout="vertical"
							initialValues={initialValues}
							className={styles.formStack}
						>
							<Card className={styles.settingsCard} bordered>
								<div className={styles.cardTitle}>品牌标识</div>
								<p className={styles.cardDesc}>名称与 LOGO，用于侧栏与对外展示</p>
								<div className={styles.brandRow}>
									<div className={styles.brandLogo}>
										<DomainLogoField
											form={form}
											label="业务域标识 LOGO"
											previewName={domain.name}
											uploadDomainId={resolveNumericDomainId(domain.id)}
											variant="wizard"
										/>
									</div>
									<div className={styles.brandFields}>
										<Form.Item
											name="name"
											label="业务域显示名称"
											rules={[{ required: true, message: "请输入名称" }]}
										>
											<Input placeholder="例如：新零售自建客服中心" />
										</Form.Item>
									</div>
								</div>
							</Card>

							<Card className={styles.settingsCard} bordered>
								<div className={styles.cardTitle}>域标识（只读）</div>
								<p className={styles.cardDesc}>短码与二级域名创建后不可修改</p>
								<div className={styles.identityStack}>
									<Form.Item
										name="code"
										label="唯一隔离识别短码"
										extra="仅小写字母、数字、下划线与连字符"
									>
										<Input className="font-mono" disabled />
									</Form.Item>
									<Form.Item name="portal_url" label="绑定专属二级域名">
										<Input className="font-mono" disabled />
									</Form.Item>
								</div>
							</Card>

							<Card className={styles.settingsCard} bordered>
								<div className={styles.cardTitle}>业务背景</div>
								<p className={styles.cardDesc}>运营范围与职责说明</p>
								<Form.Item
									name="description"
									label="业务背景描述说明"
									style={{ marginBottom: 0, marginTop: 16 }}
								>
									<Input.TextArea
										rows={4}
										maxLength={512}
										showCount
										placeholder="简要描述该隔离业务域的运营范围和业务条线职责..."
									/>
								</Form.Item>
							</Card>

							<div className={styles.formActions}>
								{saveButton}
							</div>
						</Form>
					</div>
				)}
			</AuthGuarded>
		</BasicContent>
	);
}
