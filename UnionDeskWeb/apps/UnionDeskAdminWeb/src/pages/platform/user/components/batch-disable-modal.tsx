import type { AdminDomain, IamUser } from "@uniondesk/shared";
import { P0_STEP_UP_OPERATION, fetchAdminDomainsPage, toErrorMessage } from "@uniondesk/shared";

import type { DomainBatchStatusResult } from "#src/api/platform/iam";
import { batchDisableDomainMembers } from "#src/api/platform/iam";
import StepUpModal from "#src/components/step-up-modal";

import { Alert, App, Button, Checkbox, Empty, Modal, Spin, Tag, Typography } from "antd";
import { useCallback, useEffect, useMemo, useState } from "react";

interface BatchDisableModalProps {
	open: boolean;
	user: IamUser | null;
	onClose: () => void;
}

type ViewState = "select" | "submit" | "result";

function domainLabel(domains: AdminDomain[], domainId: number | string): string {
	const domain = domains.find(item => item.id === String(domainId));
	return domain ? `${domain.name}（${domain.code}）` : `业务域 #${domainId}`;
}

/**
 * P1-2 跨域批量停用（design §7 F4.7）：选员工 → 选域集 → step-up 二次认证 → 逐域部分成功摘要。
 */
export function BatchDisableModal(props: BatchDisableModalProps) {
	const { open, user, onClose } = props;
	const { message } = App.useApp();
	const [domains, setDomains] = useState<AdminDomain[]>([]);
	const [loading, setLoading] = useState(false);
	const [selectedDomainIds, setSelectedDomainIds] = useState<string[]>([]);
	const [stepUpOpen, setStepUpOpen] = useState(false);
	const [submitting, setSubmitting] = useState(false);
	const [result, setResult] = useState<DomainBatchStatusResult | null>(null);
	const [view, setView] = useState<ViewState>("select");

	const loadDomains = useCallback(async () => {
		setLoading(true);
		try {
			const page = await fetchAdminDomainsPage({ page: 1, page_size: 100 });
			setDomains(page.list);
		}
		catch (error) {
			message.error(`加载业务域失败：${toErrorMessage(error)}`);
		}
		finally {
			setLoading(false);
		}
	}, [message]);

	useEffect(() => {
		if (!open) {
			return;
		}
		setSelectedDomainIds([]);
		setResult(null);
		setView("select");
		void loadDomains();
	}, [loadDomains, open, user?.id]);

	const domainOptions = useMemo(
		() => domains.map(domain => ({ label: `${domain.name}（${domain.code}）`, value: domain.id })),
		[domains],
	);

	const handleNext = () => {
		if (selectedDomainIds.length === 0) {
			message.warning("请至少选择一个业务域");
			return;
		}
		setStepUpOpen(true);
	};

	const handleVerified = async (stepUpToken: string) => {
		if (!user) {
			return;
		}
		setStepUpOpen(false);
		setView("submit");
		setSubmitting(true);
		try {
			const res = await batchDisableDomainMembers(user.id, selectedDomainIds, stepUpToken);
			setResult(res);
			setView("result");
		}
		catch (error) {
			message.error(toErrorMessage(error));
			setView("select");
		}
		finally {
			setSubmitting(false);
		}
	};

	const handleClose = () => {
		setView("select");
		setResult(null);
		setSelectedDomainIds([]);
		onClose();
	};

	const failedDomains = result?.failed ?? [];

	return (
		<>
			<Modal
				title="跨域批量停用"
				open={open}
				width={560}
				destroyOnHidden
				okText={view === "result" ? "完成" : "下一步"}
				cancelText="取消"
				confirmLoading={submitting}
				onOk={() => {
					if (view === "result") {
						handleClose();
					}
					else {
						handleNext();
					}
				}}
				onCancel={handleClose}
			>
				{view === "select" && (
					<div>
						<Typography.Paragraph type="secondary" className="mb-3">
							停用员工
							{" "}
							<Typography.Text strong>{user?.username ?? ""}</Typography.Text>
							{" "}
							在以下业务域的成员权限（每域独立处理，部分失败不影响其他域）。
						</Typography.Paragraph>
						{loading
							? (
								<div style={{ display: "flex", justifyContent: "center", padding: "48px 0" }}>
									<Spin />
								</div>
							)
							: domains.length === 0
								? <Empty description="暂无业务域" />
								: (
									<div style={{ maxHeight: 360, overflow: "auto" }}>
										<Checkbox.Group
											value={selectedDomainIds}
											onChange={values => setSelectedDomainIds(values.map(String))}
											style={{ width: "100%" }}
										>
											<div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
												{domainOptions.map(option => (
													<Checkbox key={option.value} value={option.value}>
														{option.label}
													</Checkbox>
												))}
											</div>
										</Checkbox.Group>
									</div>
								)}
					</div>
				)}
				{view === "submit" && (
					<div style={{ display: "flex", justifyContent: "center", padding: "48px 0" }}>
						<Spin tip="正在逐域停用成员权限..." />
					</div>
				)}
				{view === "result" && result && (
					<div>
						<Alert
							type={failedDomains.length === 0 ? "success" : "warning"}
							showIcon
							message={failedDomains.length === 0
								? `已停用 ${result.success.length} 个业务域的成员权限`
								: `部分成功：成功 ${result.success.length} 个，失败 ${failedDomains.length} 个`}
						/>
						{failedDomains.length > 0 && (
							<div style={{ marginTop: 12, maxHeight: 240, overflow: "auto" }}>
								<Typography.Paragraph type="secondary" className="mb-2">
									失败域明细：
								</Typography.Paragraph>
								{failedDomains.map(item => (
									<div key={item.domain_id} style={{ marginBottom: 8 }}>
										<Tag color="error">{domainLabel(domains, item.domain_id)}</Tag>
										<span className="text-sm text-[var(--ant-color-text-secondary)]">{item.reason}</span>
									</div>
								))}
							</div>
						)}
						{result.success.length > 0 && failedDomains.length > 0 && (
							<Typography.Paragraph type="secondary" className="mt-3 mb-0">
								成功域：{result.success.map(domainId => domainLabel(domains, domainId)).join("、")}
							</Typography.Paragraph>
						)}
					</div>
				)}
			</Modal>
			<StepUpModal
				open={stepUpOpen}
				title="二次验证"
				description={`为符合 P0 安全要求，跨域批量停用「${user?.username ?? ""}」前请再次输入当前账号登录密码。`}
				operationCode={P0_STEP_UP_OPERATION.STAFF_DOMAIN_BATCH_STATUS}
				onCancel={() => setStepUpOpen(false)}
				onVerified={token => void handleVerified(token)}
			/>
		</>
	);
}
