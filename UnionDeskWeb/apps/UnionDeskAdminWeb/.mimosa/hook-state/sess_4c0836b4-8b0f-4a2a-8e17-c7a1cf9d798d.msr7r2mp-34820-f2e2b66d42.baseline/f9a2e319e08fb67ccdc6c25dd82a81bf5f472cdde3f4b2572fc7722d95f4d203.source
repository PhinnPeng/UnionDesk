import type { TreeProps } from "antd";

import { Checkbox, Input, Tree } from "antd";
import { useEffect, useMemo, useRef, useState } from "react";
import { useTranslation } from "react-i18next";

export interface TreeDataNodeWithId {
	id: string
	title: string
	children?: TreeDataNodeWithId[]
}

interface FormTreeItemProps {
	treeData: TreeDataNodeWithId[]
	value?: React.Key[]
	onChange?: (value: React.Key[]) => void
	/** 父子勾选独立存储，展示半选；点击父节点时联动勾选/取消整棵子树 */
	checkStrictly?: boolean
}

function normalizeCheckedKeys(checkedKeys: Parameters<NonNullable<TreeProps["onCheck"]>>[0]): React.Key[] {
	if (Array.isArray(checkedKeys)) {
		return checkedKeys;
	}
	return checkedKeys.checked;
}

function sameTreeKey(a: React.Key, b: React.Key): boolean {
	return String(a) === String(b);
}

/** checkStrictly 模式下，根据已选节点推导父级半选（indeterminate）展示 */
export function computeStrictTreeCheckedKeys(
	treeData: TreeDataNodeWithId[],
	selectedKeys: React.Key[],
): { checked: React.Key[]; halfChecked: React.Key[] } {
	const selectedSet = new Set(selectedKeys.map(String));
	const checked: React.Key[] = [];
	const halfChecked: React.Key[] = [];

	function walk(node: TreeDataNodeWithId): { total: number; selected: number } {
		if (!node.children?.length) {
			if (selectedSet.has(String(node.id))) {
				checked.push(node.id);
			}
			return { total: 1, selected: selectedSet.has(String(node.id)) ? 1 : 0 };
		}

		let total = 1;
		let selectedCount = selectedSet.has(String(node.id)) ? 1 : 0;
		for (const child of node.children) {
			const sub = walk(child);
			total += sub.total;
			selectedCount += sub.selected;
		}

		if (selectedCount > 0) {
			if (selectedCount === total) {
				checked.push(node.id);
			}
			else {
				halfChecked.push(node.id);
			}
		}
		return { total, selected: selectedCount };
	}

	for (const node of treeData) {
		walk(node);
	}
	return { checked, halfChecked };
}

function findTreeNodeById(treeData: TreeDataNodeWithId[], id: React.Key): TreeDataNodeWithId | undefined {
	for (const node of treeData) {
		if (sameTreeKey(node.id, id)) {
			return node;
		}
		if (node.children?.length) {
			const found = findTreeNodeById(node.children, id);
			if (found) {
				return found;
			}
		}
	}
	return undefined;
}

function collectSubtreeIds(node: TreeDataNodeWithId): React.Key[] {
	const ids: React.Key[] = [node.id];
	if (node.children?.length) {
		for (const child of node.children) {
			ids.push(...collectSubtreeIds(child));
		}
	}
	return ids;
}

function isDescendantsFullySelected(node: TreeDataNodeWithId, selectedSet: Set<string>): boolean {
	const descendantIds = collectSubtreeIds(node).filter(id => !sameTreeKey(id, node.id));
	if (!descendantIds.length) {
		return selectedSet.has(String(node.id));
	}
	return descendantIds.every(id => selectedSet.has(String(id)));
}

/** 自底向上同步所有父节点：子树全部选中则自动勾选，否则移除 */
function syncAllAncestorSelection(treeData: TreeDataNodeWithId[], selectedSet: Set<string>): void {
	function walk(nodes: TreeDataNodeWithId[]): void {
		for (const node of nodes) {
			if (node.children?.length) {
				walk(node.children);
				if (isDescendantsFullySelected(node, selectedSet)) {
					selectedSet.add(String(node.id));
				}
				else {
					selectedSet.delete(String(node.id));
				}
			}
		}
	}
	walk(treeData);
}

/** checkStrictly 模式下，按被点击节点联动整棵子树勾选/取消，并向上同步祖先 */
export function applyStrictCascadeToggle(
	treeData: TreeDataNodeWithId[],
	previousKeys: React.Key[],
	nodeKey: React.Key,
	checked: boolean,
): React.Key[] {
	const node = findTreeNodeById(treeData, nodeKey);
	if (!node) {
		return previousKeys;
	}

	const next = new Set(previousKeys.map(String));
	const subtreeIds = collectSubtreeIds(node);

	if (checked) {
		for (const id of subtreeIds) {
			next.add(String(id));
		}
	}
	else {
		for (const id of subtreeIds) {
			next.delete(String(id));
		}
	}

	syncAllAncestorSelection(treeData, next);

	return [...next];
}

const { Search } = Input;

function getParentKey(key: React.Key, tree: TreeDataNodeWithId[]): React.Key {
	let parentKey: React.Key;
	for (let i = 0; i < tree.length; i++) {
		const node = tree[i];
		if (node.children) {
			if (node.children.some(item => item.id === key)) {
				parentKey = node.id;
			}
			else if (getParentKey(key, node.children)) {
				parentKey = getParentKey(key, node.children);
			}
		}
	}
	return parentKey!;
}

export function FormTreeItem({ treeData, value, onChange, checkStrictly = false }: FormTreeItemProps) {
	const [expandedKeys, setExpandedKeys] = useState<React.Key[]>([]);
	const [searchValue, setSearchValue] = useState("");
	const [checkedOptions, setCheckedOptions] = useState<string[]>([]);
	const [autoExpandParent, setAutoExpandParent] = useState(true);
	const previousCheckedOptionsRef = useRef<string[]>([]);
	const { t } = useTranslation();

	// const onSelect: TreeProps["onSelect"] = (selectedKeys) => {
	// 	onChange?.(selectedKeys);
	// };

	const strictCheckedKeys = useMemo(
		() => (checkStrictly ? computeStrictTreeCheckedKeys(treeData, value ?? []) : undefined),
		[checkStrictly, treeData, value],
	);

	const onCheck: TreeProps["onCheck"] = (checkedKeys, info) => {
		if (checkStrictly) {
			onChange?.(applyStrictCascadeToggle(treeData, value ?? [], info.node.key, info.checked));
			return;
		}
		onChange?.(normalizeCheckedKeys(checkedKeys));
	};

	const onExpand = (newExpandedKeys: React.Key[]) => {
		setExpandedKeys(newExpandedKeys);
		setAutoExpandParent(false);
	};

	const flattenTreeData = useMemo(() => {
		const dataList: { id: React.Key, title: string }[] = [];
		const generateList = (data: TreeDataNodeWithId[]) => {
			for (let i = 0; i < data.length; i++) {
				const node = data[i];
				dataList.push({ id: node.id, title: node.title as string });
				if (node.children) {
					generateList(node.children);
				}
			}
		};
		generateList(treeData);

		return dataList;
	}, [treeData]);

	const handleSearchChange = (e: React.ChangeEvent<HTMLInputElement>) => {
		const { value } = e.target;
		const newExpandedKeys = flattenTreeData
			.map((item) => {
				if (t(item.title).includes(value)) {
					return getParentKey(item.id, treeData);
				}
				return null;
			})
			.filter((item, i, self): item is React.Key => !!(item && self.indexOf(item) === i));
		setExpandedKeys(newExpandedKeys);
		setSearchValue(value);
		setAutoExpandParent(true);
	};

	const onCheckboxChange = (checkedValues: string[]) => {
		setCheckedOptions(checkedValues);
	};

	useEffect(() => {
		const previousCheckedOptions = previousCheckedOptionsRef.current;
		const expandAll = checkedOptions.includes("expandAll");
		const previousExpandAll = previousCheckedOptions.includes("expandAll");
		if (expandAll) {
			setExpandedKeys(flattenTreeData.map(item => item.id));
		}
		else if (previousExpandAll) {
			setExpandedKeys([]);
		}

		const checkAll = checkedOptions.includes("checkAll");
		const previousCheckAll = previousCheckedOptions.includes("checkAll");
		if (checkAll) {
			onChange?.(flattenTreeData.map(item => item.id));
		}
		else if (previousCheckAll) {
			onChange?.([]);
		}

		previousCheckedOptionsRef.current = checkedOptions;
	}, [checkedOptions, flattenTreeData, onChange]);
	return (
		<>
			<Search
				className="mb-3"
				placeholder={t("common.keywordSearch")}
				allowClear
				value={searchValue}
				onChange={handleSearchChange}
			/>
			<Checkbox.Group
				options={[
					{ label: checkedOptions.includes("expandAll") ? t("common.collapseAll") : t("common.expandAll"), value: "expandAll" },
					{ label: checkedOptions.includes("checkAll") ? t("common.cancelAll") : t("common.checkAll"), value: "checkAll" },
				]}
				value={checkedOptions}
				rootClassName="flex justify-between items-center mb-3"
				onChange={onCheckboxChange}
			/>

			<Tree
				checkable
				blockNode
				defaultExpandAll
				checkStrictly={checkStrictly}
				titleRender={node => t(node.title as string)}
				onExpand={onExpand}
				expandedKeys={expandedKeys}
				autoExpandParent={autoExpandParent}
				fieldNames={{
					key: "id",
				}}
				checkedKeys={checkStrictly ? strictCheckedKeys : value}
				treeData={treeData as any}
				onCheck={onCheck}
			/>
		</>
	);
}
