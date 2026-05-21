export function getAllExpandedKeys(data: any[], fieldName = "key"): string[] {
	return data.flatMap(item => [
		item[fieldName],
		...(item.children?.length ? getAllExpandedKeys(item.children, fieldName) : []),
	]);
}

/**
 * 返回使树展开到指定层级所需的节点 key。
 * @param maxVisibleLevel 可见的最大层级（1=仅根节点，2=展开到二级节点）
 */
export function getExpandedKeysToLevel(
	data: any[],
	maxVisibleLevel: number,
	fieldName = "key",
	currentLevel = 1,
): Array<string | number> {
	if (currentLevel >= maxVisibleLevel) {
		return [];
	}

	return data.flatMap((item) => {
		if (!item.children?.length) {
			return [];
		}

		return [
			item[fieldName],
			...getExpandedKeysToLevel(item.children, maxVisibleLevel, fieldName, currentLevel + 1),
		];
	});
}
