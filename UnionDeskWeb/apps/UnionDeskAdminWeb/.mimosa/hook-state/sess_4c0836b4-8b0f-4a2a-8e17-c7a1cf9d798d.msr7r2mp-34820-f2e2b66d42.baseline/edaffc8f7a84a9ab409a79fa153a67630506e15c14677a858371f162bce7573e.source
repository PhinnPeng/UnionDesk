export const ICON_PICKER_PAGE_SIZE = 40;

export function paginateIcons<T>(icons: T[], page: number, pageSize = ICON_PICKER_PAGE_SIZE) {
	const currentPage = Number.isFinite(page) && page > 0 ? Math.floor(page) : 1;
	const start = (currentPage - 1) * pageSize;
	return icons.slice(start, start + pageSize);
}
