import { uploadAttachment } from "#src/api/platform/attachment";

import { GlobalOutlined, UploadOutlined } from "@ant-design/icons";
import { App, Avatar, Button, Col, Form, Row, Upload } from "antd";
import type { FormInstance, UploadProps } from "antd";

import {
	DEFAULT_DOMAIN_LOGO,
	DOMAIN_LOGO_ACCEPT,
	DOMAIN_LOGO_MAX_BYTES,
	DOMAIN_LOGO_UPLOAD_FALLBACK_DOMAIN_ID,
	isValidLogoUrl,
	resolveDomainLogoUrl,
} from "../domain-utils";

interface DomainLogoFieldProps {
	form: FormInstance;
	name?: string;
	label?: string;
	previewName?: string;
	uploadDomainId?: number | null;
	required?: boolean;
}

/** Logo 上传与预览（存储为 URL） */
export function DomainLogoField({
	form,
	name = "logo",
	label = "Logo",
	previewName,
	uploadDomainId,
	required = true,
}: DomainLogoFieldProps) {
	const { message } = App.useApp();

	const validateLogo = async (_: unknown, value: string | undefined) => {
		if (!isValidLogoUrl(value, { required })) {
			if (required && !value?.trim()) {
				throw new Error("请上传 Logo");
			}
			throw new Error("请输入有效的图片地址");
		}
	};

	return (
		<Form.Item noStyle shouldUpdate={(prev, cur) => prev[name] !== cur[name] || prev.name !== cur.name}>
			{() => {
				const logoUrl = (form.getFieldValue(name) as string | undefined)?.trim();
				const displayName = (previewName ?? (form.getFieldValue("name") as string | undefined))?.trim();
				const fallbackLetter = displayName?.charAt(0)?.toUpperCase() ?? "?";
				const previewSrc = resolveDomainLogoUrl(logoUrl);

				const uploadProps: UploadProps = {
					accept: DOMAIN_LOGO_ACCEPT,
					maxCount: 1,
					showUploadList: false,
					beforeUpload: (file) => {
						if (!DOMAIN_LOGO_ACCEPT.split(",").includes(file.type)) {
							message.error("仅支持 PNG、JPEG、WebP 图片");
							return Upload.LIST_IGNORE;
						}
						if (file.size > DOMAIN_LOGO_MAX_BYTES) {
							message.error("Logo 大小不能超过 2MB");
							return Upload.LIST_IGNORE;
						}
						const domainId = uploadDomainId ?? DOMAIN_LOGO_UPLOAD_FALLBACK_DOMAIN_ID;
						void uploadAttachment(domainId, file, "business_domain_logo")
							.then((result) => {
								form.setFieldValue(name, result.download_url);
								message.success("Logo 上传成功");
							})
							.catch((error: unknown) => {
								message.error(error instanceof Error ? error.message : "Logo 上传失败");
							});
						return Upload.LIST_IGNORE;
					},
				};

				return (
					<Form.Item
						name={name}
						label={label}
						rules={[{ validator: validateLogo }]}
						extra={required ? "必填，支持 PNG/JPEG/WebP，最大 2MB" : undefined}
					>
						<Row gutter={12} align="middle">
							<Col flex="auto">
								<Upload {...uploadProps}>
									<Button icon={<UploadOutlined />}>上传 Logo</Button>
								</Upload>
								{logoUrl && logoUrl !== DEFAULT_DOMAIN_LOGO ? (
									<Button
										type="link"
										size="small"
										className="!px-0"
										onClick={() => form.setFieldValue(name, DEFAULT_DOMAIN_LOGO)}
									>
										恢复默认
									</Button>
								) : null}
							</Col>
							<Col flex="none">
								<Avatar
									size={48}
									src={previewSrc}
									icon={previewSrc === DEFAULT_DOMAIN_LOGO ? <GlobalOutlined /> : undefined}
									style={{ backgroundColor: "#1677ff", flexShrink: 0 }}
								>
									{fallbackLetter}
								</Avatar>
							</Col>
						</Row>
					</Form.Item>
				);
			}}
		</Form.Item>
	);
}
