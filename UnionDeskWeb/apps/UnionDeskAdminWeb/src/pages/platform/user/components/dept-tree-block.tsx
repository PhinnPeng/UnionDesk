import type { PlatformOrganizationView } from "@uniondesk/shared";
import type { TreeDataNode } from "antd";
import type { Key } from "react";

import { buildOrganizationTree, type OrganizationTreeNode } from "#src/pages/platform/dept/utils";

import { ApartmentOutlined, CompressOutlined, DownOutlined, ExpandOutlined, SearchOutlined } from "@ant-design/icons";
import { Button, Input, Tree, Tooltip, Card, Space } from "antd";
import { useMemo, useState } from "react";

interface DeptTreeBlockProps {
	organizations: PlatformOrganizationView[];
	selectedDepartmentId?: number | null;
	onSelect?: (departmentId: number | null) => void;
}

function transformToTreeData(nodes: OrganizationTreeNode[], searchValue: string): TreeDataNode[] {
	return nodes.reduce<TreeDataNode[]>((acc, node) => {
		const children = transformToTreeData(node.children, searchValue);
		const isMatch = node.name.toLowerCase().includes(searchValue.toLowerCase());

		if (isMatch || children.length > 0) {
			acc.push({
				key: String(node.id),
				title: (
					<span className={isMatch ? "text-colorPrimary font-medium" : ""}>
						{node.name}
					</span>
				),
				children: children.length > 0 ? children : undefined,
			});
		}
		return acc;
	}, []);
}

function getAllKeys(nodes: TreeDataNode[]): Key[] {
	let keys: Key[] = [];
	for (const node of nodes) {
		keys.push(node.key);
		if (node.children) {
			keys = keys.concat(getAllKeys(node.children));
		}
	}
	return keys;
}

export function DeptTreeBlock({ organizations, selectedDepartmentId = null, onSelect }: DeptTreeBlockProps) {
	const [searchValue, setSearchValue] = useState("");
	const [expandedKeys, setExpandedKeys] = useState<Key[]>([]);
	const [autoExpandParent, setAutoExpandParent] = useState(true);

	const organizationTree = useMemo(() => buildOrganizationTree(organizations), [organizations]);

	const treeData = useMemo(() => {
		return transformToTreeData(organizationTree, searchValue);
	}, [organizationTree, searchValue]);

	const allKeys = useMemo(() => getAllKeys(treeData), [treeData]);

	const onSearchChange = (e: React.ChangeEvent<HTMLInputElement>) => {
		const { value } = e.target;
		setSearchValue(value);
		setAutoExpandParent(true);
		if (value) {
			setExpandedKeys(allKeys);
		}
	};

	const isAllExpanded = expandedKeys.length === allKeys.length && allKeys.length > 0;

	const handleToggleExpand = () => {
		if (isAllExpanded) {
			setExpandedKeys([]);
		} else {
			setExpandedKeys(allKeys);
			setAutoExpandParent(true);
		}
	};

	return (
		<Card
			className="flex h-full flex-col"
			styles={{ body: { flex: 1, overflow: "hidden", display: "flex", flexDirection: "column", padding: "16px" } }}
			title={(
				<Space>
					<ApartmentOutlined />
					<span>部门架构</span>
				</Space>
			)}
			extra={(
				<Space size={4}>
					<Tooltip title="展开所有节点">
						<Button
							type="text"
							size="small"
							icon={<ExpandOutlined />}
							onClick={handleToggleExpand}
						/>
					</Tooltip>
					<Tooltip title="收起所有节点">
						<Button
							type="text"
							size="small"
							icon={<CompressOutlined />}
							onClick={handleToggleExpand}
						/>
					</Tooltip>
				</Space>
			)}
		>
			<div className="flex flex-col gap-3 h-full">
				<Input
					placeholder="搜索部门..."
					prefix={<SearchOutlined className="text-colorTextPlaceholder" />}
					value={searchValue}
					onChange={onSearchChange}
					allowClear
					className="bg-colorBgLayout"
				/>

				<div className="min-h-0 flex-1 overflow-auto">
					{treeData.length > 0 ? (
						<Tree
							showIcon
							blockNode
							switcherIcon={<DownOutlined />}
							treeData={treeData}
							selectedKeys={selectedDepartmentId == null ? [] : [String(selectedDepartmentId)]}
							expandedKeys={expandedKeys}
							autoExpandParent={autoExpandParent}
							onExpand={(keys) => {
								setExpandedKeys(keys);
								setAutoExpandParent(false);
							}}
							onSelect={(keys) => {
								const id = keys.length > 0 ? Number(keys[0]) : null;
								onSelect?.(id != null && id === selectedDepartmentId ? null : id);
							}}
							className="bg-transparent"
						/>
					) : (
						<div className="flex h-32 items-center justify-center text-colorTextDescription">
							未找到匹配部门
						</div>
					)}
				</div>
			</div>
		</Card>
	);
}

