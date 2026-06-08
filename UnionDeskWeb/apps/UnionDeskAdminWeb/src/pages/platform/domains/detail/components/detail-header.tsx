import type { AdminDomain } from "@uniondesk/shared";

import { ArrowLeftOutlined, CopyOutlined, DownOutlined, FileTextOutlined, LinkOutlined } from "@ant-design/icons";
import { App, Avatar, Button, Dropdown, Tag, Typography } from "antd";
import dayjs from "dayjs";
import { useNavigate } from "react-router";

import { derivePortalUrl, isDomainEnabled, resolveDomainLogoUrl } from "./detail-shared";

import styles from "../index.module.less";

const { Title, Link } = Typography;

export interface DetailHeaderProps {
	domain: AdminDomain;
}

export function DetailHeader({ domain }: DetailHeaderProps) {
	const { message } = App.useApp();
	const navigate = useNavigate();

	const enabled = isDomainEnabled(domain);
	const portalUrl = derivePortalUrl(domain.code);

	const menuItems = [
		{
			key: "copy",
			label: "复制当前配置",
			icon: <CopyOutlined />,
			onClick: () => message.info("功能开发中"),
		},
		{
			key: "template",
			label: "提炼为公共模板",
			icon: <FileTextOutlined />,
			onClick: () => message.info("功能开发中"),
		},
	];

	return (
		<div className={styles.headerMeta}>
			<div className={styles.headerMain}>
				<Avatar size={64} src={resolveDomainLogoUrl(domain.logo)} />
				<div className={styles.headerInfo}>
					<div className={styles.headerTitleRow}>
						<Title level={4} className="!mb-0">
							{domain.name}
						</Title>
						<Tag color={enabled ? "success" : "default"}>
							{enabled ? "正常" : "禁用"}
						</Tag>
						<Tag>{domain.code}</Tag>
					</div>
					<div className={styles.headerMetaRow}>
						<span>
							<LinkOutlined className="mr-1" />
							<Link href={portalUrl} target="_blank" rel="noreferrer">
								{portalUrl}
							</Link>
						</span>
						<span>
							创建：
							{domain.created_at ? dayjs(domain.created_at).format("YYYY-MM-DD") : "—"}
						</span>
						<span>域管理员：{domain.creator_name ?? "—"}</span>
					</div>
					{domain.description?.trim() ? (
						<div className={styles.descriptionBoard}>{domain.description.trim()}</div>
					) : null}
				</div>
			</div>
			<div className={styles.headerActions}>
				<Button icon={<ArrowLeftOutlined />} onClick={() => navigate("/platform/domains")}>
					返回列表
				</Button>
				<Dropdown menu={{ items: menuItems }} trigger={["click"]}>
					<Button>
						操作选项
						<DownOutlined />
					</Button>
				</Dropdown>
			</div>
		</div>
	);
}
