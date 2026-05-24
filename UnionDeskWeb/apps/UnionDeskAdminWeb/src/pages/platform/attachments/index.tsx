import type { AdminDomain } from "@uniondesk/shared";

import { BasicContent } from "#src/components/basic-content";

import { uploadAttachment } from "#src/api/platform/attachment";

import { fetchAdminDomainsPage, toErrorMessage } from "@uniondesk/shared";
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
					message="对象存储（服务端代理上传）"
					description="文件经 `POST /api/v1/attachments/upload` 提交到后端，由后端写入 MinIO。需本地 `docker compose` 启动 minio 服务。"
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
								const res = await uploadAttachment(Number(domainId), file, "ticket");
								message.success(`上传成功，attachment_id=${res.attachment_id}`);
							} catch (e) {
								message.error(toErrorMessage(e));
							}
							return Upload.LIST_IGNORE;
						}}
					>
						<Button type="primary">选择文件并尝试上传</Button>
					</Upload>
					<Typography.Paragraph type="secondary" className="!mb-0 !text-sm">
						上传完成后可在 MinIO 控制台 bucket `uniondesk-attachments` 中查看对象。
					</Typography.Paragraph>
				</Space>
			</Card>
		</BasicContent>
	);
}
