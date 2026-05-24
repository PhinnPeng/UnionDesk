import type { PlatformComRegistryItem } from "./platform-com-registry";

export type PlatformComCascaderOption = {
	label: string
	value: string
	children?: PlatformComCascaderOption[]
};

type TreeNode = {
	label: string
	value: string
	children: Map<string, TreeNode>
};

function splitLabelSegments(label: string) {
	return label.split("/").map(segment => segment.trim()).filter(Boolean);
}

function splitValueSegments(value: string) {
	return value.split("/").filter(Boolean);
}

export function splitComponentKey(componentKey?: string | null): string[] {
	if (!componentKey) {
		return [];
	}
	return splitValueSegments(componentKey.trim().replace(/^\.\//, ""));
}

export function joinComponentKey(segments?: string[] | string | null): string | null {
	if (!segments) {
		return null;
	}
	if (typeof segments === "string") {
		return splitComponentKey(segments).join("/") || null;
	}
	if (!segments.length) {
		return null;
	}
	const last = segments[segments.length - 1];
	return last?.trim() || null;
}

/** 将 componentKey 转为级联表单值（每级为路径前缀，与 options.value 一致） */
export function componentKeyToCascaderValue(componentKey?: string | null): string[] {
	const key = splitComponentKey(componentKey).join("/");
	if (!key) {
		return [];
	}
	const segments = splitValueSegments(key);
	return segments.map((_, index) => segments.slice(0, index + 1).join("/"));
}

function buildSegmentLabelHints(registry: PlatformComRegistryItem[]) {
	const hints = new Map<string, string>();

	for (const item of registry) {
		const valueSegments = splitValueSegments(item.value);
		const labelSegments = splitLabelSegments(item.label);

		for (let index = 0; index < valueSegments.length; index += 1) {
			const prefix = valueSegments.slice(0, index + 1).join("/");
			const hintedLabel = labelSegments[index] ?? labelSegments[labelSegments.length - 1];
			if (hintedLabel && !hints.has(prefix)) {
				hints.set(prefix, hintedLabel);
			}
		}
	}

	return hints;
}

function mapTreeToOptions(nodes: Map<string, TreeNode>): PlatformComCascaderOption[] {
	return Array.from(nodes.values())
		.sort((left, right) => left.label.localeCompare(right.label, "zh-CN"))
		.map(node => ({
			label: node.label,
			value: node.value,
			children: node.children.size > 0 ? mapTreeToOptions(node.children) : undefined,
		}));
}

export function buildPlatformComCascaderOptions(registry: PlatformComRegistryItem[]): PlatformComCascaderOption[] {
	const segmentLabelHints = buildSegmentLabelHints(registry);
	const root = new Map<string, TreeNode>();

	for (const item of registry) {
		const valueSegments = splitValueSegments(item.value);
		const labelSegments = splitLabelSegments(item.label);
		let currentLevel = root;
		let pathPrefix = "";

		for (let index = 0; index < valueSegments.length; index += 1) {
			const segment = valueSegments[index];
			const isLeaf = index === valueSegments.length - 1;
			pathPrefix = pathPrefix ? `${pathPrefix}/${segment}` : segment;

			let node = currentLevel.get(segment);
			if (!node) {
				node = {
					label: isLeaf
						? (labelSegments[labelSegments.length - 1] ?? item.label)
						: (segmentLabelHints.get(pathPrefix) ?? segment),
					// 每级使用完整路径前缀作为 value，避免不同分支下同名 segment（如 home）冲突
					value: pathPrefix,
					children: new Map(),
				};
				currentLevel.set(segment, node);
			}
			else if (isLeaf) {
				node.label = labelSegments[labelSegments.length - 1] ?? item.label;
			}

			if (!isLeaf) {
				currentLevel = node.children;
			}
		}
	}

	return mapTreeToOptions(root);
}
