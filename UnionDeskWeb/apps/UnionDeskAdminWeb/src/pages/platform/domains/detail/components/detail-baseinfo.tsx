import type { AdminDomain } from "@uniondesk/shared";
import {
	deleteAdminDomain,
	P0_STEP_UP_OPERATION,
	toErrorMessage,
	updateAdminDomain,
} from "@uniondesk/shared";

import StepUpModal from "#src/components/step-up-modal";
import { useAuth } from "#src/hooks/use-auth";

import { CheckCircleOutlined, DeleteOutlined, StopOutlined } from "@ant-design/icons";
import { App, Button, Form, Input, Modal, Tooltip, Typography } from "antd";
import { useEffect, useMemo, useState } from "react";

import { DomainBasicInfoFields } from "../../components/domains-modal";
import modalStyles from "../../components/domains-modal.module.less";
import {
	DEFAULT_DOMAIN_LOGO,
	DOMAIN_CONTROL_GENERAL_DELETE_PERMISSION,
	DOMAIN_CONTROL_GENERAL_UPDATE_PERMISSION,
	DOMAIN_CONTROL_GENERAL_UPDATE_STATUS_PERMISSION,
	isDomainEnabled,
	resolveNumericDomainId,
} from "./detail-shared";

import styles from "./detail-baseinfo.module.less";

const { Title, Paragraph, Text } = Typography;

type BasicInfoFormValues = {
	name: string;
	logo: string;
	description: string;
	code: string;
	portal_url: string;
};

export interface DetailBaseinfoProps {
	domain: AdminDomain;
	onSaved: (domain: AdminDomain) => void;
	onDeleted?: () => void;
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

interface DangerZoneRowProps {
	title?: string;
	description: string;
	actions: React.ReactNode;
	deleteVariant?: boolean;
}

function DangerZoneRow({ title, description, actions, deleteVariant = false }: DangerZoneRowProps) {
	return (
		<div className={deleteVariant ? `${styles.dangerRow} ${styles.dangerRowDelete}` : styles.dangerRow}>
			<div className={styles.dangerRowMain}>
				{title ? <Text strong>{title}</Text> : null}
				<Text type="secondary" className={styles.dangerRowDesc}>
					{description}
				</Text>
			</div>
			<div className={styles.dangerRowActions}>
				{actions}
			</div>
		</div>
	);
}

export function DetailBaseinfo({ domain, onSaved, onDeleted }: DetailBaseinfoProps) {
	const { message, modal } = App.useApp();
	const { hasPermission } = useAuth();
	const [form] = Form.useForm<BasicInfoFormValues>();
	const [submitting, setSubmitting] = useState(false);
	const [codeConfirmOpen, setCodeConfirmOpen] = useState(false);
	const [codeInput, setCodeInput] = useState("");
	const [stepUpOpen, setStepUpOpen] = useState(false);
	const [deleting, setDeleting] = useState(false);
	const [statusUpdating, setStatusUpdating] = useState(false);

	useEffect(() => {
		form.setFieldsValue(buildFormValues(domain));
	}, [domain.id, domain.updated_at, form]);

	const canUpdate = hasPermission(DOMAIN_CONTROL_GENERAL_UPDATE_PERMISSION);
	const canUpdateStatus = hasPermission(DOMAIN_CONTROL_GENERAL_UPDATE_STATUS_PERMISSION);
	const canDelete = hasPermission(DOMAIN_CONTROL_GENERAL_DELETE_PERMISSION);
	const enabled = isDomainEnabled(domain);
	const formKey = `${domain.id}-${domain.updated_at ?? "0"}`;
	const statusDescription = enabled
		? "禁用后，该业务域将暂停对外服务，控制台仍可查看与恢复。"
		: "启用后，该业务域将恢复对外服务。";

	const handleSave = async () => {
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
			onSaved(updated);
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
			content: "确定要保存对业务域基础信息的修改吗？",
			okText: "确定更新",
			cancelText: "取消",
			onOk: () => handleSave(),
		});
	};

	const handleDeleteClick = () => {
		if (!canDelete) {
			return;
		}
		setCodeInput("");
		setCodeConfirmOpen(true);
	};

	const handleCodeConfirm = () => {
		if (codeInput.trim() !== domain.code) {
			message.error("业务域短码不一致，请重新输入");
			return;
		}
		setCodeConfirmOpen(false);
		setStepUpOpen(true);
	};

	const handleStepUpVerified = async (stepUpToken: string) => {
		setStepUpOpen(false);
		setDeleting(true);
		try {
			await deleteAdminDomain(domain.id, { stepUpToken });
			message.success("业务域已删除");
			onDeleted?.();
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
		finally {
			setDeleting(false);
		}
	};

	const handleStatusChange = async (checked: boolean) => {
		if (!canUpdateStatus) {
			return;
		}
		setStatusUpdating(true);
		try {
			const updated = await updateAdminDomain(domain.id, { status: checked ? 1 : 0 });
			message.success(checked ? "业务域已启用" : "业务域已禁用");
			onSaved(updated);
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
		finally {
			setStatusUpdating(false);
		}
	};

	const handleDisableClick = () => {
		if (!canUpdateStatus) {
			return;
		}
		modal.confirm({
			title: "确认禁用业务域",
			content: "禁用后，该业务域将暂停对外服务，控制台仍可查看与恢复。确定继续吗？",
			okText: "确定禁用",
			cancelText: "取消",
			onOk: () => handleStatusChange(false),
		});
	};

	const initialValues = useMemo(() => buildFormValues(domain), [
		domain.id,
		domain.updated_at,
		domain.name,
		domain.logo,
		domain.description,
		domain.code,
	]);

	return (
		<div>
			<Title level={5} className="!mb-4">
				基础信息
			</Title>
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
			<Title level={5} className="!mb-4 !mt-8">
				危险区
			</Title>
			<div className={styles.dangerSection}>
				<DangerZoneRow
					title="业务域状态"
					description={statusDescription}
					actions={(
						<Tooltip title={canUpdateStatus ? undefined : "无状态更新权限"}>
							{enabled ? (
								<Button
									color="orange"
									variant="outlined"
									icon={<StopOutlined />}
									disabled={!canUpdateStatus}
									loading={statusUpdating}
									onClick={handleDisableClick}
								>
									禁用
								</Button>
							) : (
								<Button
									color="green"
									variant="solid"
									icon={<CheckCircleOutlined />}
									disabled={!canUpdateStatus}
									loading={statusUpdating}
									onClick={() => void handleStatusChange(true)}
								>
									启用
								</Button>
							)}
						</Tooltip>
					)}
				/>
				<DangerZoneRow
					title="删除业务域"
					description="删除后，该业务域将从列表中移除且不可再通过控制台访问。此操作不可撤销，请谨慎操作。"
					deleteVariant
					actions={(
						<Tooltip title={canDelete ? undefined : "无删除权限"}>
							<Button
								danger
								icon={<DeleteOutlined />}
								disabled={!canDelete}
								loading={deleting}
								onClick={handleDeleteClick}
							>
								删除
							</Button>
						</Tooltip>
					)}
				/>
			</div>
			<Modal
				title="确认删除业务域"
				open={codeConfirmOpen}
				okText="确认删除"
				cancelText="取消"
				destroyOnClose
				onCancel={() => setCodeConfirmOpen(false)}
				onOk={handleCodeConfirm}
			>
				<Paragraph className="!mb-3">
					即将删除业务域「
					<Text strong>{domain.name}</Text>
					」。请输入业务域短码
					<Text code>{domain.code}</Text>
					以继续。
				</Paragraph>
				<Input
					value={codeInput}
					placeholder="请输入业务域短码"
					autoComplete="off"
					onChange={event => setCodeInput(event.target.value)}
					onPressEnter={handleCodeConfirm}
				/>
			</Modal>
			<StepUpModal
				open={stepUpOpen}
				title="安全验证"
				description="请输入登录密码以确认删除业务域。"
				operationCode={P0_STEP_UP_OPERATION.DELETE_BUSINESS_DOMAIN}
				onCancel={() => setStepUpOpen(false)}
				onVerified={token => void handleStepUpVerified(token)}
			/>
		</div>
	);
}
