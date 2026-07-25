import type { DomainTicketFormSchemaVersionDetail, DomainTicketFormSchemaVersionSummary } from "@uniondesk/shared";
import {
	fetchDomainTicketTypeFormSchemaVersion,
	fetchDomainTicketTypeFormSchemaVersions,
	rollbackDomainTicketTypeFormSchemaVersion,
	toErrorMessage,
} from "@uniondesk/shared";

import { ConfirmPopover } from "#src/components/confirm-popover";
import { FormilyFormDesignerFallback } from "#src/components/formily-form-designer";
import { mergeSystemFormSchema } from "#src/components/formily-form-designer/form-schema-utils";

import { EyeOutlined, RollbackOutlined } from "@ant-design/icons";
import { App, Button, Drawer, Empty, Spin, Table, Typography } from "antd";
import type { TableColumnsType } from "antd";
import { lazy, Suspense, useCallback, useEffect, useMemo, useState } from "react";

const LazyFormilyFormDesigner = lazy(() => import("#src/components/formily-form-designer").then(module => ({
	default: module.FormilyFormDesigner,
})));

const { Text } = Typography;

interface FormSchemaVersionDrawerProps {
	open: boolean;
	domainId: string;
	typeId: string;
	onClose: () => void;
	onRollbackSuccess?: () => void;
}

export function FormSchemaVersionDrawer({
	open,
	domainId,
	typeId,
	onClose,
	onRollbackSuccess,
}: FormSchemaVersionDrawerProps) {
	const { message } = App.useApp();
	const [loading, setLoading] = useState(false);
	const [rollingBackVersion, setRollingBackVersion] = useState<number | null>(null);
	const [currentVersionNo, setCurrentVersionNo] = useState<number | null>(null);
	const [versions, setVersions] = useState<DomainTicketFormSchemaVersionSummary[]>([]);
	const [previewOpen, setPreviewOpen] = useState(false);
	const [previewLoading, setPreviewLoading] = useState(false);
	const [previewVersionNo, setPreviewVersionNo] = useState<number | null>(null);
	const [previewSchema, setPreviewSchema] = useState<Record<string, unknown>>(mergeSystemFormSchema(null));

	const loadVersions = useCallback(async () => {
		if (!domainId || !typeId) {
			return;
		}
		setLoading(true);
		try {
			const result = await fetchDomainTicketTypeFormSchemaVersions(domainId, typeId);
			setVersions(result.items ?? []);
			setCurrentVersionNo(result.current_version_no ?? null);
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
		finally {
			setLoading(false);
		}
	}, [domainId, message, typeId]);

	useEffect(() => {
		if (!open) {
			return;
		}
		void loadVersions();
	}, [loadVersions, open]);

	const handlePreview = async (versionNo: number) => {
		setPreviewOpen(true);
		setPreviewLoading(true);
		setPreviewVersionNo(versionNo);
		try {
			const detail: DomainTicketFormSchemaVersionDetail = await fetchDomainTicketTypeFormSchemaVersion(
				domainId,
				typeId,
				versionNo,
			);
			setPreviewSchema(mergeSystemFormSchema(detail.form_schema));
		}
		catch (error) {
			message.error(toErrorMessage(error));
			setPreviewOpen(false);
		}
		finally {
			setPreviewLoading(false);
		}
	};

	const handleRollback = async (versionNo: number) => {
		setRollingBackVersion(versionNo);
		try {
			await rollbackDomainTicketTypeFormSchemaVersion(domainId, typeId, versionNo);
			message.success(`已回退并发布为 v${versionNo} 内容的新版本`);
			await loadVersions();
			onRollbackSuccess?.();
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
		finally {
			setRollingBackVersion(null);
		}
	};

	const columns = useMemo<TableColumnsType<DomainTicketFormSchemaVersionSummary>>(() => [
		{
			title: "版本",
			width: 120,
			render: (_, record) => (
				<span>
					v{record.version_no}
					{record.is_current ? " *" : ""}
				</span>
			),
		},
		{
			title: "发布时间",
			dataIndex: "published_at",
			render: value => value ?? "-",
		},
		{
			title: "操作",
			width: 180,
			render: (_, record) => (
				<div className="flex gap-2">
					<Button
						type="link"
						size="small"
						icon={<EyeOutlined />}
						onClick={() => void handlePreview(record.version_no)}
					>
						预览
					</Button>
					<ConfirmPopover
						title={`确认回退到 v${record.version_no}？`}
						description="回退将立即发布为新版本，不会删除中间历史。"
						onConfirm={() => handleRollback(record.version_no)}
					>
						<Button
							type="link"
							size="small"
							icon={<RollbackOutlined />}
							loading={rollingBackVersion === record.version_no}
							disabled={record.is_current}
						>
							回退
						</Button>
					</ConfirmPopover>
				</div>
			),
		},
	], [rollingBackVersion]);

	return (
		<>
			<Drawer
				title="历史版本"
				width={640}
				open={open}
				onClose={onClose}
				destroyOnHidden
			>
				{loading ? (
					<div className="flex justify-center py-16">
						<Spin />
					</div>
				) : versions.length === 0 ? (
					<Empty description="暂无发布历史" />
				) : (
					<>
						{currentVersionNo ? (
							<Text type="secondary" className="mb-3 block">
								当前生效版本：v{currentVersionNo}
							</Text>
						) : null}
						<Table
							rowKey="version_no"
							size="small"
							pagination={false}
							columns={columns}
							dataSource={versions}
						/>
					</>
				)}
			</Drawer>

			<Drawer
				title={previewVersionNo ? `预览 v${previewVersionNo}` : "预览"}
				width={960}
				open={previewOpen}
				onClose={() => setPreviewOpen(false)}
				destroyOnHidden
			>
				{previewLoading ? (
					<div className="flex justify-center py-16">
						<Spin />
					</div>
				) : (
					<Suspense fallback={<FormilyFormDesignerFallback />}>
						<LazyFormilyFormDesigner
							key={`preview-${previewVersionNo ?? "none"}`}
							value={previewSchema}
							onChange={() => {}}
							disabled
							hint="只读预览，不可编辑。"
						/>
					</Suspense>
				)}
			</Drawer>
		</>
	);
}
