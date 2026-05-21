import type { AdminDomain, P0RegistrationPolicy, P0VisibilityPolicyCode } from "@uniondesk/shared";
import { Avatar, Card, Space, Tag, Typography, Tooltip, Dropdown } from "antd";
import { EditOutlined, EllipsisOutlined, GlobalOutlined, SettingOutlined, UserAddOutlined, DeleteOutlined, PoweroffOutlined } from "@ant-design/icons";
import dayjs from "dayjs";

const { Text } = Typography;

interface DomainCardProps {
	domain: AdminDomain;
	onEdit: (domain: AdminDomain) => void;
	onManage: (domain: AdminDomain) => void;
	onToggleStatus: (domain: AdminDomain) => void;
	onDelete: (domain: AdminDomain) => void;
	registrationOptions: { value: P0RegistrationPolicy; label: string }[];
}

export function DomainCard({
	domain,
	onEdit,
	onManage,
	onToggleStatus,
	onDelete,
	registrationOptions,
}: DomainCardProps) {
	const registrationLabel = registrationOptions.find(o => o.value === domain.registration_policy)?.label ?? domain.registration_policy;
	const isEnabled = domain.status === "1" || domain.status === "active" || domain.status === "enabled";

	return (
		<Card
			hoverable
			actions={[
				<Tooltip title="编辑" key="edit">
					<EditOutlined onClick={() => onEdit(domain)} />
				</Tooltip>,
				<Tooltip title="管理" key="manage">
					<SettingOutlined onClick={() => onManage(domain)} />
				</Tooltip>,
				<Tooltip title={isEnabled ? "禁用" : "启用"} key="toggle">
					<PoweroffOutlined 
						style={{ color: isEnabled ? undefined : "#ff4d4f" }} 
						onClick={() => onToggleStatus(domain)} 
					/>
				</Tooltip>,
				<Dropdown
					key="more"
					menu={{
						items: [
							{
								key: "delete",
								label: "删除",
								danger: true,
								icon: <DeleteOutlined />,
								onClick: () => onDelete(domain),
							},
						],
					}}
				>
					<EllipsisOutlined />
				</Dropdown>,
			]}
		>
			<Card.Meta
				avatar={(
					<Avatar 
						size={48} 
						src={domain.logo} 
						icon={!domain.logo && <GlobalOutlined />}
						style={{ backgroundColor: domain.logo ? "transparent" : "#1677ff" }}
					>
						{domain.name.charAt(0).toUpperCase()}
					</Avatar>
				)}
				title={(
					<Space direction="vertical" size={0} className="w-full">
						<div className="flex items-center justify-between">
							<Text strong className="text-lg truncate max-w-[150px]">
								{domain.name}
							</Text>
							<Tag color={isEnabled ? "success" : "default"}>
								{isEnabled ? "已启用" : "已禁用"}
							</Tag>
						</div>
						<Text type="secondary" copyable className="text-xs">
							{domain.code}
						</Text>
					</Space>
				)}
				description={(
					<div className="mt-3 space-y-2">
						<Space size={4} wrap>
							{domain.visibility_policy_codes.map(c => (
								<Tag key={c}>{c}</Tag>
							))}
							<Tag color="cyan">{registrationLabel}</Tag>
						</Space>
						<div className="flex flex-col text-xs text-gray-400">
							<Text type="secondary" style={{ fontSize: "12px" }}>
								创建于: {domain.created_at ? dayjs(domain.created_at).format("YYYY-MM-DD") : "-"}
							</Text>
							<Text type="secondary" style={{ fontSize: "12px" }}>
								创建者: {domain.creator_name || "系统"}
							</Text>
						</div>
					</div>
				)}
			/>
		</Card>
	);
}
