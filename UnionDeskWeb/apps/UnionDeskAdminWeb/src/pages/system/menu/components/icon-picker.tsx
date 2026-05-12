import { parseIconValue } from "#src/icons/render-icon";

import { Icon } from "@iconify/react";
import { Input, Popover, Select, Spin, Tabs, Tooltip } from "antd";
import type { MouseEvent } from "react";
import { useRef, useState } from "react";

interface IconPickerProps {
	value?: string
	onChange?: (value: string) => void
}

const PRESET_COLLECTIONS = [
	{ label: "Ant Design", value: "ant-design" },
	{ label: "Material Design Icons", value: "mdi" },
	{ label: "Lucide", value: "lucide" },
	{ label: "Heroicons", value: "heroicons" },
	{ label: "Tabler Icons", value: "tabler" },
	{ label: "Font Awesome 6", value: "fa6-solid" },
	{ label: "Remix Icon", value: "ri" },
	{ label: "Carbon", value: "carbon" },
];

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

export function IconPicker({ value, onChange }: IconPickerProps) {
	const [open, setOpen] = useState(false);
	const [tab, setTab] = useState<"search" | "browse">("search");

	const [searchIcons, setSearchIcons] = useState<string[]>([]);
	const [searchLoading, setSearchLoading] = useState(false);
	const [keyword, setKeyword] = useState("");
	const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

	const [collection, setCollection] = useState(PRESET_COLLECTIONS[0].value);
	const [browseIcons, setBrowseIcons] = useState<string[]>([]);
	const [browseLoading, setBrowseLoading] = useState(false);
	const browseLoadedRef = useRef<string>("");

	const search = (kw: string) => {
		setKeyword(kw);
		if (timerRef.current) clearTimeout(timerRef.current);
		if (!kw || kw.length < 2) { setSearchIcons([]); return; }
		timerRef.current = setTimeout(async () => {
			setSearchLoading(true);
			try {
				const res = await fetch(`https://api.iconify.design/search?query=${encodeURIComponent(kw)}&limit=120`);
				const data = await res.json();
				setSearchIcons(data.icons ?? []);
			}
			catch { setSearchIcons([]); }
			finally { setSearchLoading(false); }
		}, 400);
	};

	const loadCollection = async (prefix: string) => {
		if (browseLoadedRef.current === prefix) return;
		browseLoadedRef.current = prefix;
		setBrowseLoading(true);
		setBrowseIcons([]);
		try {
			const res = await fetch(`https://api.iconify.design/collection?prefix=${prefix}&limit=240`);
			const data = await res.json();
			const uncategorized: string[] = data.uncategorized ?? [];
			const categorized: string[] = Object.values(data.categories ?? {}).flat() as string[];
			const names = [...new Set([...uncategorized, ...categorized])].slice(0, 240);
			setBrowseIcons(names.map(name => `${prefix}:${name}`));
		}
		catch { setBrowseIcons([]); }
		finally { setBrowseLoading(false); }
	};

	const handleTabChange = (key: string) => {
		setTab(key as "search" | "browse");
		if (key === "browse") loadCollection(collection);
	};

	const handleCollectionChange = (prefix: string) => {
		setCollection(prefix);
		browseLoadedRef.current = "";
		loadCollection(prefix);
	};

	const handleSelect = (icon: string) => { onChange?.(icon); setOpen(false); };
	const handleClear = (e: MouseEvent) => { e.stopPropagation(); onChange?.(""); };

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
									placeholder="搜索图标（如：home、user、setting）"
									value={keyword}
									onChange={e => search(e.target.value)}
									allowClear
									style={{ marginBottom: 8 }}
								/>
								<div style={{ height: 260, overflowY: "auto" }}>
									{searchLoading && <div className="flex justify-center items-center h-full"><Spin /></div>}
									{!searchLoading && searchIcons.length === 0 && (
										<div className="flex justify-center items-center h-full text-sm" style={{ color: "var(--ant-color-text-quaternary)" }}>
											{keyword.length < 2 ? "请输入至少2个字符搜索" : "未找到匹配图标"}
										</div>
									)}
									{!searchLoading && <IconGrid icons={searchIcons} selected={value} onSelect={handleSelect} />}
								</div>
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
									options={PRESET_COLLECTIONS}
									onChange={handleCollectionChange}
								/>
								<div style={{ height: 260, overflowY: "auto" }}>
									{browseLoading && <div className="flex justify-center items-center h-full"><Spin /></div>}
									{!browseLoading && browseIcons.length === 0 && (
										<div className="flex justify-center items-center h-full text-sm" style={{ color: "var(--ant-color-text-quaternary)" }}>暂无图标</div>
									)}
									{!browseLoading && <IconGrid icons={browseIcons} selected={value} onSelect={handleSelect} />}
								</div>
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
								✕
							</span>
						</>
					)
					: <span className="text-sm" style={{ color: "var(--ant-color-text-placeholder)" }}>选择图标</span>}
			</div>
		</Popover>
	);
}
