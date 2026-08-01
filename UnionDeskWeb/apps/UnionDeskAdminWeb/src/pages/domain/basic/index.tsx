import type { AdminDomain } from "@uniondesk/shared";
import { fetchAdminDomain, toErrorMessage, updateAdminDomain } from "@uniondesk/shared";

import { AuthGuarded } from "#src/components/auth-guarded";
import { BasicContent } from "#src/components/basic-content";
import { useAuth } from "#src/hooks/use-auth";
import {
	DOMAIN_GENERAL_READ,
	DOMAIN_GENERAL_UPDATE,
} from "#src/pages/domain/domain-permissions";
import { DomainBasicInfoFields } from "#src/pages/platform/domains/components/domains-modal";
import modalStyles from "#src/pages/platform/domains/components/domains-modal.module.less";
import { useAuthStore } from "#src/store/auth";

import { App, Button, Card, Empty, Form, Spin, Tooltip } from "antd";
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

	return (
		<BasicContent>
			<AuthGuarded
				auth={DOMAIN_GENERAL_READ}
				fallback={<Empty description="无权限查看通用设置" className="py-16" />}
			>
				<Card title="通用设置" bordered={false}>
					{!domainId ? (
						<Empty description="暂无可用业务域" className="py-16" />
					) : loading ? (
						<div className="flex justify-center py-16">
							<Spin />
						</div>
					) : !domain ? (
						<Empty description="业务域信息加载失败" className="py-16" />
					) : (
						<div className={modalStyles.basicInfoPanel}>
							<Form
								key={formKey}
								form={form}
								layout="vertical"
								initialValues={initialValues}
							>
								<DomainBasicInfoFields
									form={form}
									mode="detail"
									previewName={domain.name}
									uploadDomainId={resolveNumericDomainId(domain.id)}
								/>
								<div className={styles.formFooterActions}>
									<Tooltip title={canUpdate ? undefined : "无编辑权限"}>
										<Button
											type="primary"
											disabled={!canUpdate}
											loading={submitting}
											onClick={handleUpdateClick}
										>
											更新信息
										</Button>
									</Tooltip>
								</div>
							</Form>
						</div>
					)}
				</Card>
			</AuthGuarded>
		</BasicContent>
	);
}
