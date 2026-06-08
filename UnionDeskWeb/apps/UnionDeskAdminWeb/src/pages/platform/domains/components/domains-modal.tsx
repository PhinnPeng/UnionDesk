import type {
	CreateAdminDomainPayload,
	P0AccessPolicy,
	P0VisibilityPolicyCode,
} from "@uniondesk/shared";
import { createAdminDomain, toErrorMessage } from "@uniondesk/shared";

import { uploadAttachment } from "#src/api/platform/attachment";
import { AuthGuarded } from "#src/components/auth-guarded";

import { PLATFORM_DOMAIN_CREATE } from "../platform-domain-permissions";

import {
	AppstoreOutlined,
	CheckCircleFilled,
	CheckCircleOutlined,
	CloseOutlined,
	FolderOpenOutlined,
	GlobalOutlined,
	LockOutlined,
	PartitionOutlined,
	PlayCircleFilled,
	PlusOutlined,
	ReloadOutlined,
	StopFilled,
	TagOutlined,
	UploadOutlined,
	UserAddOutlined,
} from "@ant-design/icons";
import {
	App,
	Avatar,
	Button,
	Checkbox,
	Col,
	Form,
	Input,
	Modal,
	Row,
	Select,
	Space,
	Steps,
	Tag,
	Upload,
} from "antd";
import type { FormInstance, StepsProps, UploadProps } from "antd";
import { useEffect, useMemo, useRef, useState } from "react";

import { generateDomainCode } from "../domain-utils";

import styles from "./domains-modal.module.less";

interface DomainLogoFieldProps {
	form: FormInstance;
	name?: string;
	label?: string;
	previewName?: string;
	uploadDomainId?: number | null;
	required?: boolean;
	/** wizard：虚线框上传样式（创建向导 Step2） */
	variant?: "default" | "wizard";
}

interface VisibilityPolicyFieldProps {
	form: FormInstance;
	name?: string;
	label?: string;
}

type CreateMode = "blank" | "template";

interface ModeStepProps {
	createMode: CreateMode;
	onSelectMode: (mode: CreateMode) => void;
}

interface BasicInfoStepProps {
	form: FormInstance;
	onRandomCode: () => void;
}

export interface DomainBasicInfoFieldsProps {
	form: FormInstance;
	uploadDomainId?: number | null;
	previewName?: string;
	mode: "create" | "detail";
	onRandomCode?: () => void;
}

type VisibilityMode = "public" | "private";

interface PolicyStepProps {
	form: FormInstance;
	onDirty?: () => void;
}

interface PreviewStepProps {
	form: FormInstance;
	createMode: CreateMode;
}

export interface DomainsModalProps {
	open: boolean;
	onClose: () => void;
	onCreated: (result: { id: string; name: string }) => void;
}

type DomainCreateFormValues = CreateAdminDomainPayload & {
	/** 向导 UI 字段，不提交 API */
	portal_url?: string;
};

const DEFAULT_DOMAIN_LOGO = "/default-domain-logo.svg";
const DOMAIN_LOGO_ACCEPT = "image/png,image/jpeg,image/webp";
const DOMAIN_LOGO_MAX_BYTES = 2 * 1024 * 1024;
const DOMAIN_LOGO_UPLOAD_FALLBACK_DOMAIN_ID = 1;

const accessPolicyOptions: { value: P0AccessPolicy; label: string }[] = [
	{ value: "allowed", label: "允许" },
	{ value: "disallowed", label: "不允许" },
];

const visibilityOptions: { value: P0VisibilityPolicyCode; label: string }[] = [
	{ value: "public", label: "公开" },
	{ value: "domain_customer_only", label: "仅域内客户" },
	{ value: "channel_only", label: "仅渠道" },
];

const WIZARD_STEP_ITEMS: StepsProps["items"] = [
	{ title: "选择模式" },
	{ title: "基本信息" },
	{ title: "策略配置" },
	{ title: "预览创建" },
];

const STEP_FIELD_NAMES: string[][] = [
	[],
	["name", "code", "logo", "portal_url"],
	["visibility_policy_codes", "registration_enabled", "invitation_enabled"],
	[],
];

const MODE_LABELS: Record<CreateMode, string> = {
	blank: "全新空白独立域",
	template: "行业级公共预置模板",
};

function selectionCardClass(selected: boolean, extra?: string): string {
	return [
		styles.selectionCard,
		selected ? styles.selectionCardSelected : "",
		extra,
	].filter(Boolean).join(" ");
}

function derivePortalUrl(code?: string): string {
	const trimmed = code?.trim();
	return trimmed ? `${trimmed}.uniondesk.com` : "";
}

function normalizeVisibility(codes: P0VisibilityPolicyCode[]): P0VisibilityPolicyCode[] {
	if (codes.includes("public")) {
		return ["public"];
	}
	return codes.length > 0 ? codes : ["public"];
}

function isValidLogoUrl(value: string | undefined, options?: { required?: boolean }): boolean {
	const required = options?.required ?? false;
	if (!value?.trim()) {
		return !required;
	}
	try {
		const url = new URL(value.trim());
		return url.protocol === "http:" || url.protocol === "https:" || value.startsWith("/");
	}
	catch {
		return value.startsWith("/");
	}
}

function resolveDomainLogoUrl(logo?: string | null): string {
	const trimmed = logo?.trim();
	return trimmed && isValidLogoUrl(trimmed) ? trimmed : DEFAULT_DOMAIN_LOGO;
}

function applyVisibilityPolicyChange(
	previous: P0VisibilityPolicyCode[],
	next: P0VisibilityPolicyCode[],
): P0VisibilityPolicyCode[] {
	const added = next.filter(code => !previous.includes(code));
	if (added.includes("public")) {
		return ["public"];
	}
	if (previous.includes("public") && next.some(code => code !== "public")) {
		return next.filter(code => code !== "public");
	}
	if (next.includes("public") && next.length > 1) {
		return next.filter(code => code !== "public");
	}
	return next.length > 0 ? next : ["public"];
}

function resolveVisibilityMode(codes: P0VisibilityPolicyCode[] | undefined): VisibilityMode {
	return codes?.includes("public") ? "public" : "private";
}

function applyVisibilityMode(form: FormInstance, mode: VisibilityMode, onDirty?: () => void) {
	if (mode === "public") {
		form.setFieldsValue({
			visibility_policy_codes: ["public"] as P0VisibilityPolicyCode[],
		});
	}
	else {
		form.setFieldsValue({
			visibility_policy_codes: ["domain_customer_only"] as P0VisibilityPolicyCode[],
		});
	}
	onDirty?.();
}

function applyRegEnabled(form: FormInstance, enabled: boolean, onDirty?: () => void) {
	form.setFieldValue("registration_enabled", enabled ? ("allowed" satisfies P0AccessPolicy) : ("disallowed" satisfies P0AccessPolicy));
	onDirty?.();
}

function applyInviteEnabled(form: FormInstance, enabled: boolean, onDirty?: () => void) {
	form.setFieldValue("invitation_enabled", enabled ? ("allowed" satisfies P0AccessPolicy) : ("disallowed" satisfies P0AccessPolicy));
	onDirty?.();
}

function formatVisContract(
	visibility: P0VisibilityPolicyCode[],
	registration: P0AccessPolicy,
	invitation: P0AccessPolicy,
): string {
	const isPublic = visibility.includes("public");
	const visText = isPublic ? "全公开(Public)策略" : "私有可见性过滤拦截策略";
	let regText = "开放自注册";
	if (registration === "disallowed" && invitation === "allowed") {
		regText = "仅限特邀验证码";
	}
	else if (registration === "disallowed" && invitation === "disallowed") {
		regText = "禁止自主注册";
	}
	return `${visText} — ${regText}`;
}

/** Logo 上传与预览（存储为 URL） */
export function DomainLogoField({
	form,
	name = "logo",
	label = "Logo",
	previewName,
	uploadDomainId,
	required = true,
	variant = "default",
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
						extra={required
							? (variant === "wizard" ? "仅限图片格式，大小不超过 2MB" : "必填，支持 PNG/JPEG/WebP，最大 2MB")
							: undefined}
					>
						{variant === "wizard" ? (
							<Upload {...uploadProps}>
								<div className={`group ${styles.logoUploadBox}`}>
									{logoUrl?.trim() ? (
										<>
											<img
												src={previewSrc}
												alt="Logo 预览"
												className="size-full rounded object-cover p-0.5"
											/>
											<div className="absolute inset-0 flex items-center justify-center rounded bg-black/40 opacity-0 transition group-hover:opacity-100">
												<span className="text-[10px] text-white">更换</span>
											</div>
										</>
									) : (
										<>
											<PlusOutlined className="text-base" />
											<span className="mt-1 text-[10px]">上传图片</span>
										</>
									)}
								</div>
							</Upload>
						) : (
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
						)}
					</Form.Item>
				);
			}}
		</Form.Item>
	);
}

/** 可见策略多选（public 与其余互斥） */
export function VisibilityPolicyField({
	form,
	name = "visibility_policy_codes",
	label = "可见策略",
}: VisibilityPolicyFieldProps) {
	return (
		<Form.Item
			name={name}
			label={label}
			rules={[{ required: true, message: "请选择可见策略" }]}
		>
			<Form.Item noStyle shouldUpdate>
				{() => {
					const current = (form.getFieldValue(name) as P0VisibilityPolicyCode[] | undefined) ?? ["public"];
					return (
						<Checkbox.Group
							value={current}
							onChange={(values) => {
								const next = applyVisibilityPolicyChange(
									current,
									values as P0VisibilityPolicyCode[],
								);
								form.setFieldValue(name, next);
							}}
						>
							{visibilityOptions.map(option => (
								<Checkbox key={option.value} value={option.value}>
									{option.label}
								</Checkbox>
							))}
						</Checkbox.Group>
					);
				}}
			</Form.Item>
		</Form.Item>
	);
}

/** 业务域入域双配置表单项（注册 / 邀请），须在 Form + Row 内使用 */
export function DomainAccessPolicyFields() {
	return (
		<>
			<Col span={24}>
				<Form.Item
					name="registration_enabled"
					label="业务域注册配置"
					rules={[{ required: true, message: "请选择注册配置" }]}
				>
					<Select options={accessPolicyOptions} />
				</Form.Item>
			</Col>
			<Col span={24}>
				<Form.Item
					name="invitation_enabled"
					label="业务域邀请配置"
					rules={[{ required: true, message: "请选择邀请配置" }]}
				>
					<Select options={accessPolicyOptions} />
				</Form.Item>
			</Col>
		</>
	);
}

function ModeStep({ createMode, onSelectMode }: ModeStepProps) {
	const blankSelected = createMode === "blank";

	return (
		<div className="space-y-4 text-left">
			<span className="mb-2 block text-xs font-bold text-colorText">选择初始创建策略：</span>
			<Row gutter={16}>
				<Col xs={24} md={12}>
					<button
						type="button"
						className={selectionCardClass(blankSelected, "flex flex-col justify-between rounded-lg p-5")}
						onClick={() => onSelectMode("blank")}
					>
						<div className="space-y-3">
							<div className={styles.modeCardIcon}>
								<FolderOpenOutlined className="text-base" />
							</div>
							<h4 className="text-xs font-bold text-colorText">全新空白起航</h4>
							<p className="text-[10px] leading-relaxed text-colorTextSecondary">
								创建完全纯净的初始业务域容器。需要你在进入控制台后，从零手动建立各分类、工作班表及工单状态。
							</p>
						</div>
						<span className={`${styles.selectionCardCheck} ${blankSelected ? "" : styles.selectionCardCheckIdle}`}>
							{blankSelected ? <CheckCircleFilled /> : <CheckCircleOutlined />}
						</span>
					</button>
				</Col>
				<Col xs={24} md={12}>
					<div
						className={`${styles.selectionCard} ${styles.selectionCardDisabled} flex flex-col justify-between rounded-lg p-5`}
						aria-disabled
					>
						<Tag className="absolute top-3 right-3 z-10 m-0" color="default">
							即将推出
						</Tag>
						<div className="space-y-3">
							<div className={`${styles.modeCardIcon} ${styles.modeCardIconMuted}`}>
								<AppstoreOutlined className="text-base" />
							</div>
							<h4 className="text-xs font-bold text-colorText">套用高可用系统模板</h4>
							<p className="text-[10px] leading-relaxed text-colorTextSecondary">
								一键选择并装配系统内置行业沉淀的最佳流程方案，预置工单状态机、SLA 水位限制与默认安全角色。
							</p>
						</div>
					</div>
				</Col>
			</Row>
		</div>
	);
}

function BasicInfoStep({ form, onRandomCode }: BasicInfoStepProps) {
	return (
		<DomainBasicInfoFields
			form={form}
			mode="create"
			uploadDomainId={DOMAIN_LOGO_UPLOAD_FALLBACK_DOMAIN_ID}
			onRandomCode={onRandomCode}
		/>
	);
}

/** 创建向导 / 详情基础信息共用字段布局 */
export function DomainBasicInfoFields({
	form,
	uploadDomainId,
	previewName,
	mode,
	onRandomCode,
}: DomainBasicInfoFieldsProps) {
	const isDetail = mode === "detail";
	const resolvedUploadDomainId = uploadDomainId ?? DOMAIN_LOGO_UPLOAD_FALLBACK_DOMAIN_ID;

	return (
		<div className="space-y-4 text-left">
			<Row gutter={20} className="!mx-0">
				<Col xs={24} md={6} className={styles.logoColumn}>
					<DomainLogoField
						form={form}
						label="业务域标识 LOGO"
						previewName={previewName}
						uploadDomainId={resolvedUploadDomainId}
						variant="wizard"
					/>
				</Col>
				<Col xs={24} md={18}>
					<Row gutter={16}>
						<Col xs={24} sm={12}>
							<Form.Item name="name" label="业务域显示名称" rules={[{ required: true, message: "请输入名称" }]}>
								<Input placeholder="例如：新零售自建客服中心" />
							</Form.Item>
						</Col>
						<Col xs={24} sm={12}>
							<Form.Item
								name="code"
								label="唯一隔离识别短码"
								extra="仅小写字母、数字、下划线与连字符"
								rules={isDetail
									? undefined
									: [
											{ required: true, message: "请输入短码" },
											{ pattern: /^[a-z0-9_-]+$/, message: "仅支持小写字母、数字、下划线与连字符" },
										]}
							>
								{isDetail ? (
									<Input className="font-mono" disabled />
								) : (
									<Space.Compact className="!flex !w-full min-w-0">
										<Input className="!min-w-0 !flex-1 font-mono" placeholder="例如 acme-support" />
										<Button icon={<ReloadOutlined />} onClick={onRandomCode}>
											随机生成
										</Button>
									</Space.Compact>
								)}
							</Form.Item>
						</Col>
						<Col xs={24}>
							<Form.Item
								name="portal_url"
								label="绑定专属二级域名"
								rules={isDetail ? undefined : [{ required: true, message: "请输入域名" }]}
							>
								<Input className="font-mono" disabled={isDetail} placeholder="retail.uniondesk.com" />
							</Form.Item>
						</Col>
					</Row>
				</Col>
			</Row>
			<div className={styles.sectionDivider}>
				<Form.Item name="description" label="业务背景描述说明">
					<Input.TextArea
						rows={3}
						maxLength={512}
						showCount
						placeholder="简要描述该隔离业务域的运营范围和业务条线职责..."
					/>
				</Form.Item>
			</div>
		</div>
	);
}

function PolicyStep({ form, onDirty }: PolicyStepProps) {
	const visibilityCodes = Form.useWatch("visibility_policy_codes", form) as P0VisibilityPolicyCode[] | undefined;
	const registration = Form.useWatch("registration_enabled", form) as P0AccessPolicy | undefined;
	const invitation = Form.useWatch("invitation_enabled", form) as P0AccessPolicy | undefined;

	const visibilityMode = useMemo(() => resolveVisibilityMode(visibilityCodes), [visibilityCodes]);
	const regEnabled = registration === "allowed";
	const inviteEnabled = invitation === "allowed";

	return (
		<div className="space-y-4 text-left">
			<div className="space-y-2">
				<span className={styles.policySectionTitle}>
					<GlobalOutlined className="mr-1.5 text-primary" />
					1. 客户访问配置
				</span>
				<Row gutter={12}>
					<Col xs={24} sm={12}>
						<button
							type="button"
							className={selectionCardClass(visibilityMode === "public", "flex items-start gap-3 p-3")}
							onClick={() => applyVisibilityMode(form, "public", onDirty)}
						>
							<GlobalOutlined className="mt-0.5 shrink-0 text-sm text-primary" />
							<div className="space-y-0.5 pr-5">
								<h5 className="text-[11px] font-bold text-colorText">公开模式</h5>
								<p className="m-0 text-[9px] leading-normal text-colorTextSecondary">
									公开门户接入，任何访客均可自主访问公开知识库与入口。
								</p>
							</div>
							<span className={`${styles.selectionCardCheck} ${visibilityMode === "public" ? "" : styles.selectionCardCheckIdle}`}>
								{visibilityMode === "public" ? <CheckCircleFilled /> : <CheckCircleOutlined />}
							</span>
						</button>
					</Col>
					<Col xs={24} sm={12}>
						<button
							type="button"
							className={selectionCardClass(visibilityMode === "private", "flex items-start gap-3 p-3")}
							onClick={() => applyVisibilityMode(form, "private", onDirty)}
						>
							<LockOutlined className="mt-0.5 shrink-0 text-sm text-[#722ed1]" />
							<div className="space-y-0.5 pr-5">
								<h5 className="text-[11px] font-bold text-colorText">私域模式</h5>
								<p className="m-0 text-[9px] leading-normal text-colorTextSecondary">
									隔离验证准入，仅限白名单成员或受邀绑定客户进行登录访问。
								</p>
							</div>
							<span className={`${styles.selectionCardCheck} ${visibilityMode === "private" ? "" : styles.selectionCardCheckIdle}`}>
								{visibilityMode === "private" ? <CheckCircleFilled /> : <CheckCircleOutlined />}
							</span>
						</button>
					</Col>
				</Row>
			</div>

			<div className={`${styles.policySectionDivider} space-y-2`}>
				<span className={styles.policySectionTitle}>
					<UserAddOutlined className="mr-1.5 text-colorSuccess" />
					2. 客户注册配置
				</span>
				<Row gutter={12}>
					<Col xs={24} sm={12}>
						<button
							type="button"
							className={selectionCardClass(regEnabled, "flex items-center justify-between p-3")}
							onClick={() => applyRegEnabled(form, true, onDirty)}
						>
							<div className="flex items-center gap-2">
								<PlayCircleFilled className="text-xs text-colorSuccess" />
								<span className="text-[11px] font-bold text-colorText">开启客户注册</span>
							</div>
							<span className={`${styles.policyToggleCheck} ${regEnabled ? "" : styles.selectionCardCheckIdle}`}>
								{regEnabled ? <CheckCircleFilled /> : <CheckCircleOutlined />}
							</span>
						</button>
					</Col>
					<Col xs={24} sm={12}>
						<button
							type="button"
							className={selectionCardClass(!regEnabled, "flex items-center justify-between p-3")}
							onClick={() => applyRegEnabled(form, false, onDirty)}
						>
							<div className="flex items-center gap-2">
								<StopFilled className="text-xs text-colorTextQuaternary" />
								<span className="text-[11px] font-bold text-colorText">关闭客户注册</span>
							</div>
							<span className={`${styles.policyToggleCheck} ${!regEnabled ? "" : styles.selectionCardCheckIdle}`}>
								{!regEnabled ? <CheckCircleFilled /> : <CheckCircleOutlined />}
							</span>
						</button>
					</Col>
				</Row>
			</div>

			<div className={`${styles.policySectionDivider} space-y-2`}>
				<span className={styles.policySectionTitle}>
					<TagOutlined className="mr-1.5 text-orange-500" />
					3. 客户邀请配置
				</span>
				<Row gutter={12}>
					<Col xs={24} sm={12}>
						<button
							type="button"
							className={selectionCardClass(inviteEnabled, "flex items-center justify-between p-3")}
							onClick={() => applyInviteEnabled(form, true, onDirty)}
						>
							<div className="flex items-center gap-2">
								<PlayCircleFilled className="text-xs text-colorSuccess" />
								<span className="text-[11px] font-bold text-colorText">开启邀请激活</span>
							</div>
							<span className={`${styles.policyToggleCheck} ${inviteEnabled ? "" : styles.selectionCardCheckIdle}`}>
								{inviteEnabled ? <CheckCircleFilled /> : <CheckCircleOutlined />}
							</span>
						</button>
					</Col>
					<Col xs={24} sm={12}>
						<button
							type="button"
							className={selectionCardClass(!inviteEnabled, "flex items-center justify-between p-3")}
							onClick={() => applyInviteEnabled(form, false, onDirty)}
						>
							<div className="flex items-center gap-2">
								<StopFilled className="text-xs text-colorTextQuaternary" />
								<span className="text-[11px] font-bold text-colorText">关闭邀请激活</span>
							</div>
							<span className={`${styles.policyToggleCheck} ${!inviteEnabled ? "" : styles.selectionCardCheckIdle}`}>
								{!inviteEnabled ? <CheckCircleFilled /> : <CheckCircleOutlined />}
							</span>
						</button>
					</Col>
				</Row>
			</div>
		</div>
	);
}

function PreviewStep({ form, createMode }: PreviewStepProps) {
	const name = Form.useWatch("name", form) as string | undefined;
	const code = Form.useWatch("code", form) as string | undefined;
	const portalUrl = Form.useWatch("portal_url", form) as string | undefined;
	const logo = Form.useWatch("logo", form) as string | undefined;
	const description = Form.useWatch("description", form) as string | undefined;
	const visibility = (Form.useWatch("visibility_policy_codes", form) as P0VisibilityPolicyCode[] | undefined) ?? ["public"];
	const registration = (Form.useWatch("registration_enabled", form) as P0AccessPolicy | undefined) ?? "allowed";
	const invitation = (Form.useWatch("invitation_enabled", form) as P0AccessPolicy | undefined) ?? "allowed";

	const logoUrl = resolveDomainLogoUrl(logo);
	const displayName = name?.trim();

	return (
		<div className="space-y-4 text-left">
			<span className="mb-2 block text-xs font-bold text-colorText">配置清单总揽预览：</span>
			<div className={styles.previewCard}>
				<div className={styles.previewGrid}>
					<div>
						<span className="text-colorTextSecondary">创建模式：</span>
						<strong>{MODE_LABELS[createMode]}</strong>
					</div>
					<div className="flex items-center gap-1">
						<span className="text-colorTextSecondary">在册标识 Logo：</span>
						{displayName && logoUrl ? (
							<Avatar src={logoUrl} size={28} shape="square" />
						) : (
							<span className="inline-block rounded bg-colorPrimaryBg px-1.5 py-0.5 text-[9px] font-bold text-primary">
								{displayName?.slice(0, 2) ?? "新建"}
							</span>
						)}
					</div>
					<div>
						<span className="text-colorTextSecondary">业务域名称：</span>
						<strong>{displayName || "—"}</strong>
					</div>
					<div>
						<span className="text-colorTextSecondary">唯一短码：</span>
						<strong className="font-mono">{code?.trim() || "—"}</strong>
					</div>
					<div className="col-span-2">
						<span className="text-colorTextSecondary">域名入口：</span>
						<strong className="font-mono text-primary">{portalUrl?.trim() || "—"}</strong>
					</div>
					<div className="col-span-2">
						<span className="text-colorTextSecondary">可见与自注册契约：</span>
						<strong>{formatVisContract(visibility, registration, invitation)}</strong>
					</div>
				</div>
				<div className="pt-3 text-[10px] leading-relaxed text-colorTextSecondary">
					<span className="mb-1 block font-bold text-colorText">业务背景及运营实体描述说明</span>
					<p className="m-0">{description?.trim() || "未编写具体的业务背景及运营实体描述说明。"}</p>
				</div>
			</div>
			<div className={styles.previewHint}>
				确认后系统将极序生成独立业务域。
			</div>
		</div>
	);
}

/** 四步创建业务域 Modal 向导 */
export function DomainsModal({ open, onClose, onCreated }: DomainsModalProps) {
	const { message, modal } = App.useApp();
	const [form] = Form.useForm<DomainCreateFormValues>();
	const [currentStep, setCurrentStep] = useState(0);
	const [createMode, setCreateMode] = useState<CreateMode>("blank");
	const [submitting, setSubmitting] = useState(false);
	const [dirty, setDirty] = useState(false);
	const bodyScrollRef = useRef<HTMLDivElement>(null);
	const portalUrlTouched = useRef(false);

	useEffect(() => {
		if (!open) {
			form.resetFields();
			setCurrentStep(0);
			setCreateMode("blank");
			setDirty(false);
			portalUrlTouched.current = false;
		}
	}, [form, open]);

	const requestClose = () => {
		if (dirty) {
			modal.confirm({
				title: "放弃新建？",
				content: "当前表单尚未保存，关闭后将丢失已填写内容。",
				okText: "放弃",
				cancelText: "继续编辑",
				onOk: onClose,
			});
			return;
		}
		onClose();
	};

	const handleRandomCode = () => {
		const name = form.getFieldValue("name") as string | undefined;
		const code = generateDomainCode(name);
		form.setFieldValue("code", code);
		if (!portalUrlTouched.current) {
			form.setFieldValue("portal_url", derivePortalUrl(code));
		}
		setDirty(true);
	};

	const validateCurrentStep = async (): Promise<boolean> => {
		if (currentStep === 0) {
			if (createMode !== "blank") {
				message.warning("系统模板功能即将推出，请先选择「全新空白起航」");
				return false;
			}
			return true;
		}
		const fields = STEP_FIELD_NAMES[currentStep];
		if (fields.length === 0) {
			return true;
		}
		const result = await form.validateFields(fields).catch(() => null);
		if (!result && currentStep === 1) {
			message.error("请完整录入带有星号(*)的关键在册基础字段");
		}
		return result != null;
	};

	const scrollBodyToTop = () => {
		bodyScrollRef.current?.scrollTo({ top: 0, behavior: "smooth" });
	};

	const goToStep = (step: number) => {
		setCurrentStep(step);
		scrollBodyToTop();
	};

	const handleNext = async () => {
		const ok = await validateCurrentStep();
		if (!ok) {
			return;
		}
		goToStep(Math.min(currentStep + 1, 3));
	};

	const handlePrev = () => {
		if (currentStep <= 0) {
			return;
		}
		goToStep(currentStep - 1);
	};

	const handleStepChange = (step: number) => {
		if (step < currentStep) {
			goToStep(step);
		}
	};

	const handleSubmit = async () => {
		const values = await form.validateFields().catch(() => null);
		if (!values || createMode !== "blank") {
			return;
		}
		const payload: CreateAdminDomainPayload = {
			name: values.name.trim(),
			code: values.code.trim(),
			logo: values.logo?.trim() || DEFAULT_DOMAIN_LOGO,
			description: values.description?.trim() || undefined,
			visibility_policy_codes: normalizeVisibility(values.visibility_policy_codes ?? ["public"]),
			registration_enabled: values.registration_enabled,
			invitation_enabled: values.invitation_enabled,
		};
		setSubmitting(true);
		try {
			const result = await createAdminDomain(payload);
			message.success("已创建业务域");
			onCreated({ id: result.id, name: payload.name });
			onClose();
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
		finally {
			setSubmitting(false);
		}
	};

	const isLastStep = currentStep === 3;

	return (
		<Modal
			open={open}
			title={null}
			closable={false}
			centered
			onCancel={requestClose}
			width={800}
			maskClosable={false}
			destroyOnClose
			footer={null}
			styles={{
				container: { padding: 0, overflow: "hidden" },
				body: { padding: 0, background: "var(--ant-color-bg-container)" },
			}}
		>
			<div className={styles.wizardShell}>
				<div className={styles.wizardHeader}>
					<div className="space-y-0.5 text-left">
						<h3 className="m-0 flex items-center gap-2 text-sm font-bold">
							<PartitionOutlined className="text-primary" />
							创建业务域向导
						</h3>
						<p className="m-0 text-[10px] text-colorTextSecondary">
							请跟随以下步骤完成品牌业务线的敏捷创建与隔离配置。
						</p>
					</div>
					<button
						type="button"
						className="cursor-pointer border-0 bg-transparent p-1 text-colorTextSecondary transition hover:text-colorText"
						onClick={requestClose}
						aria-label="关闭"
					>
						<CloseOutlined className="text-base" />
					</button>
				</div>

				<div className={styles.wizardSteps}>
					<Steps
						current={currentStep}
						items={WIZARD_STEP_ITEMS}
						size="small"
						labelPlacement="vertical"
						onChange={handleStepChange}
					/>
				</div>

				<div ref={bodyScrollRef} className={styles.wizardBody}>
					<Form
						form={form}
						layout="vertical"
						requiredMark
						onValuesChange={(changed) => {
							setDirty(true);
							if ("portal_url" in changed) {
								portalUrlTouched.current = true;
							}
							if ("name" in changed && changed.name) {
								const name = String(changed.name).trim();
								if (name && !portalUrlTouched.current) {
									const code = generateDomainCode(name);
									form.setFieldsValue({
										code,
										portal_url: derivePortalUrl(code),
									});
								}
							}
						}}
						initialValues={{
							name: "",
							code: "",
							portal_url: "",
							logo: DEFAULT_DOMAIN_LOGO,
							description: "",
							visibility_policy_codes: ["public"] as P0VisibilityPolicyCode[],
							registration_enabled: "allowed",
							invitation_enabled: "allowed",
						}}
						preserve
					>
						<div className={currentStep === 0 ? undefined : "hidden"}>
							<ModeStep
								createMode={createMode}
								onSelectMode={(mode) => {
									if (mode === "template") {
										return;
									}
									setCreateMode(mode);
									setDirty(true);
								}}
							/>
						</div>
						<div className={currentStep === 1 ? undefined : "hidden"}>
							<BasicInfoStep form={form} onRandomCode={handleRandomCode} />
						</div>
						<div className={currentStep === 2 ? undefined : "hidden"}>
							<PolicyStep form={form} onDirty={() => setDirty(true)} />
						</div>
						<div className={currentStep === 3 ? undefined : "hidden"}>
							<PreviewStep form={form} createMode={createMode} />
						</div>
					</Form>
				</div>

				<div className={styles.wizardFooter}>
					<Button onClick={requestClose}>取消关闭</Button>
					<Space>
						{currentStep > 0 ? (
							<Button onClick={handlePrev}>上一步</Button>
						) : null}
						{!isLastStep ? (
							<Button type="primary" onClick={() => void handleNext()}>
								下一步
							</Button>
						) : (
							<AuthGuarded auth={PLATFORM_DOMAIN_CREATE}>
								<Button type="primary" loading={submitting} onClick={() => void handleSubmit()}>
									确认并立即创建
								</Button>
							</AuthGuarded>
						)}
					</Space>
				</div>
			</div>
		</Modal>
	);
}
