import { parseIconValue } from "#src/icons/render-icon";
import { Icon } from "@iconify/react/offline";
import { Input, Pagination, Popover, Select, Tabs, Tooltip } from "antd";
import type { MouseEvent } from "react";
import { useMemo, useState } from "react";

import { PRESET_ICON_COLLECTION_OPTIONS, getPresetCollectionIcons, searchPresetIcons } from "#src/icons/iconify-offline-catalog";

import { ICON_PICKER_PAGE_SIZE, paginateIcons } from "./icon-picker-utils";

interface IconPickerProps {
	value?: string
	onChange?: (value: string) => void
}

function IconGrid({ icons, selected, onSelect }: { icons: string[]; selected?: string; onSelect: (icon: string) => void }) {
	if (icons.length === 0) return null;
	return (
		<div className="grid gap-1" style={{ gridTemplateColumns: "repeat(8, 1fr)" }}>
			{icons.map(icon => (
				<Tooltip key={icon} title={icon} mouseEnterDelay={0.5}>
					<button
						type="button"
						onClick={() => onSelect(icon)}
						className="flex items-center justify-center rounded cursor-pointer border transition-colors"
						style={{
							width: 36,
							height: 36,
							backgroundColor: selected === icon ? "var(--ant-color-primary-bg)" : "var(--ant-color-bg-container)",
							borderColor: selected === icon ? "var(--ant-color-primary)" : "var(--ant-color-border)",
							color: "var(--ant-color-text)",
						}}
						onMouseEnter={e => {
							if (selected !== icon) {
								e.currentTarget.style.backgroundColor = "var(--ant-color-primary-bg-hover)";
								e.currentTarget.style.borderColor = "var(--ant-color-primary)";
							}
						}}
						onMouseLeave={e => {
							if (selected !== icon) {
								e.currentTarget.style.backgroundColor = "var(--ant-color-bg-container)";
								e.currentTarget.style.borderColor = "var(--ant-color-border)";
							}
						}}
					>
						<Icon icon={icon} style={{ fontSize: 20, color: "var(--ant-color-text)" }} />
					</button>
				</Tooltip>
			))}
		</div>
	);
}

function IconResults({
	icons,
	total,
	page,
	emptyText,
	selected,
	onSelect,
	onPageChange,
}: {
	icons: string[]
	total: number
	page: number
	emptyText: string
	selected?: string
	onSelect: (icon: string) => void
	onPageChange: (page: number) => void
}) {
	const showPagination = total > ICON_PICKER_PAGE_SIZE;

	return (
		<div style={{ height: 260, display: "flex", flexDirection: "column" }}>
			{icons.length === 0
				? (
					<div className="flex flex-1 items-center justify-center text-sm" style={{ color: "var(--ant-color-text-quaternary)" }}>
						{emptyText}
					</div>
				)
				: (
					<>
						<div style={{ flex: 1, overflowY: "auto" }}>
							<IconGrid icons={icons} selected={selected} onSelect={onSelect} />
						</div>
						{showPagination ? (
							<div className="flex justify-center" style={{ marginTop: 4 }}>
								<Pagination
									size="small"
									hideOnSinglePage
									showLessItems
									showSizeChanger={false}
									current={page}
									pageSize={ICON_PICKER_PAGE_SIZE}
									total={total}
									onChange={onPageChange}
								/>
							</div>
						) : null}
					</>
				)}
		</div>
	);
}

export function IconPicker({ value, onChange }: IconPickerProps) {
	const [open, setOpen] = useState(false);
	const [tab, setTab] = useState<"search" | "browse">("search");
	const [keyword, setKeyword] = useState("");
	const [searchPage, setSearchPage] = useState(1);
	const [collection, setCollection] = useState(PRESET_ICON_COLLECTION_OPTIONS[0]?.value ?? "ant-design");
	const [browsePage, setBrowsePage] = useState(1);

	const searchIcons = useMemo(() => searchPresetIcons(keyword), [keyword]);
	const browseIcons = useMemo(() => getPresetCollectionIcons(collection), [collection]);

	const searchPageIcons = useMemo(
		() => paginateIcons(searchIcons, searchPage, ICON_PICKER_PAGE_SIZE),
		[searchIcons, searchPage],
	);
	const browsePageIcons = useMemo(
		() => paginateIcons(browseIcons, browsePage, ICON_PICKER_PAGE_SIZE),
		[browseIcons, browsePage],
	);

	const handleTabChange = (key: string) => {
		setTab(key as "search" | "browse");
	};

	const handleCollectionChange = (prefix: string) => {
		setCollection(prefix);
		setBrowsePage(1);
	};

	const handleSearchChange = (kw: string) => {
		setKeyword(kw);
		setSearchPage(1);
	};

	const handleSearchPageChange = (page: number) => {
		setSearchPage(page);
	};

	const handleBrowsePageChange = (page: number) => {
		setBrowsePage(page);
	};

	const handleSelect = (icon: string) => {
		onChange?.(icon);
		setOpen(false);
	};

	const handleClear = (e: MouseEvent) => {
		e.stopPropagation();
		onChange?.("");
	};

	const searchEmptyText = keyword.trim().length < 2 ? "请输入至少 2 个字符搜索图标" : "未找到匹配图标";

	const popoverContent = (
		<div style={{ width: 380 }}>
			<Tabs
				size="small"
				activeKey={tab}
				onChange={handleTabChange}
				items={[
					{
						key: "search",
						label: "搜索",
						children: (
							<>
								<Input.Search
									autoFocus
									placeholder="搜索图标，例如：home、user、setting"
									value={keyword}
									onChange={e => handleSearchChange(e.target.value)}
									allowClear
									style={{ marginBottom: 8 }}
								/>
								<IconResults
									icons={searchPageIcons}
									total={searchIcons.length}
									page={searchPage}
									emptyText={searchEmptyText}
									selected={value}
									onSelect={handleSelect}
									onPageChange={handleSearchPageChange}
								/>
							</>
						),
					},
					{
						key: "browse",
						label: "分类浏览",
						children: (
							<>
								<Select
									size="small"
									style={{ width: "100%", marginBottom: 8 }}
									value={collection}
									options={PRESET_ICON_COLLECTION_OPTIONS}
									onChange={handleCollectionChange}
								/>
								<IconResults
									icons={browsePageIcons}
									total={browseIcons.length}
									page={browsePage}
									emptyText="暂无图标"
									selected={value}
									onSelect={handleSelect}
									onPageChange={handleBrowsePageChange}
								/>
							</>
						),
					},
				]}
			/>
		</div>
	);

	return (
		<Popover content={popoverContent} trigger="click" open={open} onOpenChange={setOpen} placement="bottomLeft">
			<div
				className="flex items-center gap-2 px-3 rounded-md cursor-pointer transition-colors"
				style={{
					height: 32,
					width: "100%",
					backgroundColor: "var(--ant-color-bg-container)",
					border: "1px solid var(--ant-color-border)",
					color: "var(--ant-color-text)",
				}}
				onMouseEnter={e => (e.currentTarget.style.borderColor = "var(--ant-color-primary)")}
				onMouseLeave={e => (e.currentTarget.style.borderColor = "var(--ant-color-border)")}
			>
				{value && parseIconValue(value)
					? (
						<>
							<Icon icon={value} style={{ fontSize: 18, flexShrink: 0 }} />
							<span className="flex-1 text-sm truncate" style={{ color: "var(--ant-color-text)" }}>{value}</span>
							<span
								className="text-xs flex-shrink-0"
								style={{ color: "var(--ant-color-text-quaternary)" }}
								onClick={handleClear}
							>
								清除
							</span>
						</>
					)
					: <span className="text-sm" style={{ color: "var(--ant-color-text-placeholder)" }}>选择图标</span>}
			</div>
		</Popover>
	);
}
