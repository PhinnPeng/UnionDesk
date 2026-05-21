import type { AdminDomain } from "@uniondesk/shared";

import { BasicContent } from "#src/components/basic-content";

import { fetchAdminDomainsPage, presignP0Attachment, toErrorMessage, uploadP0AttachmentLocal } from "@uniondesk/shared";
import { App, Alert, Button, Card, Select, Space, Typography, Upload } from "antd";
import { useCallback, useEffect, useState } from "react";

export default function PlatformAttachments() {
	const { message } = App.useApp();
	const [domains, setDomains] = useState<AdminDomain[]>([]);
	const [domainId, setDomainId] = useState<string | undefined>();

	const loadDomains = useCallback(async () => {
		try {
			const page = await fetchAdminDomainsPage({ page: 1, page_size: 100 });
			setDomains(page.list);
			setDomainId(prev => prev ?? page.list[0]?.id);
		} catch (e) {
			message.error(toErrorMessage(e));
		}
	}, [message]);

	useEffect(() => {
		void loadDomains();
	}, [loadDomains]);

	return (
		<BasicContent className="h-full bg-colorBgLayout">
			<Card title="附件（P0 契约对齐）">
				<Alert
					type="info"
					showIcon
					className="mb-4"
					message="对象存储 / 本地上传"
					description="预签名：`POST /api/v1/attachments/presign`；本地上传：`POST /api/v1/attachments/upload`（multipart）。后端未发布时操作会失败并提示错误信息。"
				/>
				<Space direction="vertical" size="middle" className="w-full">
					<div>
						<Typography.Text className="mr-2">目标业务域</Typography.Text>
						<Select
							className="min-w-48"
							value={domainId}
							placeholder="选择业务域"
							options={domains.map(d => ({ value: d.id, label: `${d.name}` }))}
							onChange={setDomainId}
						/>
					</div>
					<Upload
						maxCount={1}
						beforeUpload={async (file) => {
							if (!domainId) {
								message.warning("请先选择业务域");
								return Upload.LIST_IGNORE;
							}
							try {
								const fd = new FormData();
								fd.append("file", file);
								fd.append("target_type", "ticket");
								fd.append("domain_id", domainId);
								const res = await uploadP0AttachmentLocal(fd);
								message.success(`本地上传成功，attachment_id=${res.attachment_id}`);
							} catch {
								try {
									const pre = await presignP0Attachment({
										file_name: file.name,
										mime_type: file.type || "application/octet-stream",
										file_size: file.size,
										target_type: "ticket",
										domain_id: domainId,
									});
									message.info(`已获取预签名上传地址（请使用 PUT 上传至对象存储），attachment_id=${pre.attachment_id}`);
								} catch (e2) {
									message.error(toErrorMessage(e2));
								}
							}
							return Upload.LIST_IGNORE;
						}}
					>
						<Button type="primary">选择文件并尝试上传</Button>
					</Upload>
					<Typography.Paragraph type="secondary" className="!mb-0 !text-sm">
						先尝试本地上传；失败时自动再尝试预签名路径，便于联调 MinIO 未就绪场景。
					</Typography.Paragraph>
				</Space>
			</Card>
		</BasicContent>
	);
}
