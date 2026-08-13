interface ActiveTabNavigationState {
	activeFullPath: string;
	currentFullPath: string;
	previousActiveKey: string;
	activeKey: string;
}

export function shouldNavigateToActiveTab({
	activeFullPath,
	currentFullPath,
	previousActiveKey,
	activeKey,
}: ActiveTabNavigationState) {
	return activeKey.length > 0
		&& previousActiveKey !== activeKey
		&& activeFullPath !== currentFullPath;
}
