import { useDebounceEffect, useLocalStorageState } from "ahooks";
import { DownOutlined, SearchOutlined, UserOutlined } from "@ant-design/icons";
import { App, Avatar, Button, Dropdown, Empty, Input, Spin } from "antd";
import { useCallback, useMemo, useState } from "react";

import { fetchDomainStaffCandidates, toErrorMessage, type DomainStaffCandidate } from "@uniondesk/shared";
import { assignAdminTicket, type TicketRow } from "#src/api/platform/ticket";

import styles from "./assignee-cell.module.less";

interface AssigneeCellProps {
	domainId: string
	row: TicketRow
	/** 有指派权限才可交互；无权限时仅展示 */
	editable?: boolean
	/** 指派/清除成功后回调（父级刷新列表） */
	onChanged: () => void
}

const RECENT_KEY_PREFIX = "__recent-assignees-";

function displayName(candidate: DomainStaffCandidate) {
	return candidate.real_name ?? candidate.nickname ?? candidate.username ?? `员工 #${candidate.id}`;
}

/** 工单队列「处理人」列：头像方块 + 姓名；悬浮置灰并显示下拉图标，点击弹出极简选人下拉（搜索/清除已选/近期排前） */
export function AssigneeCell({ domainId, row, editable = true, onChanged }: AssigneeCellProps) {
	const { message } = App.useApp();
	const [open, setOpen] = useState(false);
	const [keyword, setKeyword] = useState("");
	const [candidates, setCandidates] = useState<DomainStaffCandidate[]>([]);
	const [loading, setLoading] = useState(false);
	const [submitting, setSubmitting] = useState(false);
	const [recent, setRecent] = useLocalStorageState<string[]>(`${RECENT_KEY_PREFIX}${domainId}`, {
		defaultValue: [],
	});

	const name = row.assigneeName || (row.assignedTo ? `员工 #${row.assignedTo}` : "");

	const loadCandidates = useCallback(async (nextKeyword: string) => {
		setLoading(true);
		try {
			const result = await fetchDomainStaffCandidates({
				domainId,
				page: 1,
				page_size: 200,
				keyword: nextKeyword.trim() || undefined,
			});
			setCandidates(result.list);
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
		finally {
			setLoading(false);
		}
	}, [domainId, message]);

	useDebounceEffect(
		() => {
			if (open) {
				void loadCandidates(keyword);
			}
		},
		[keyword, open],
		{ wait: 300 },
	);

	/** 近期选过的排前，其余按姓名排序 */
	const ordered = useMemo(() => {
		const recentIds = new Set(recent ?? []);
		const recentItems: DomainStaffCandidate[] = [];
		const others: DomainStaffCandidate[] = [];
		for (const item of candidates) {
			(recentIds.has(item.id) ? recentItems : others).push(item);
		}
		recentItems.sort((a, b) => (recent ?? []).indexOf(a.id) - (recent ?? []).indexOf(b.id));
		others.sort((a, b) => displayName(a).localeCompare(displayName(b), "zh-Hans-CN"));
		return [...recentItems, ...others];
	}, [candidates, recent]);

	const recordRecent = useCallback((staffAccountId: string) => {
		setRecent([staffAccountId, ...(recent ?? []).filter(item => item !== staffAccountId)].slice(0, 10));
	}, [recent, setRecent]);

	const handleSelect = useCallback(async (candidate: DomainStaffCandidate) => {
		if (submitting) {
			return;
		}
		setSubmitting(true);
		try {
			await assignAdminTicket(domainId, row.id, {
				version: row.version,
				assigneeStaffAccountId: candidate.id,
			});
			recordRecent(candidate.id);
			message.success(`已指派给 ${displayName(candidate)}`);
			setOpen(false);
			onChanged();
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
		finally {
			setSubmitting(false);
		}
	}, [domainId, message, onChanged, recordRecent, row.id, row.version, submitting]);

	const handleClear = useCallback(async () => {
		if (submitting) {
			return;
		}
		setSubmitting(true);
		try {
			await assignAdminTicket(domainId, row.id, {
				version: row.version,
				assigneeStaffAccountId: null,
			});
			message.success("已清除处理人");
			setOpen(false);
			onChanged();
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
		finally {
			setSubmitting(false);
		}
	}, [domainId, message, onChanged, row.id, row.version, submitting]);

	const trigger = (
		<div
			className={`${styles.trigger} ${open ? styles.open : ""} ${row.assignedTo ? "" : styles.unassigned}`}
			title={name || "未分配"}
		>
			<Avatar shape="square" size={22} className={styles.avatar} icon={row.assignedTo ? undefined : <UserOutlined />}>
				{row.assignedTo ? name.slice(0, 1) : null}
			</Avatar>
			<span className={styles.name}>{name || "-"}</span>
			<DownOutlined className={styles.caret} />
		</div>
	);

	const panel = (
		<div className={styles.panel} onClick={event => event.stopPropagation()}>
			<Input
				size="small"
				autoFocus
				allowClear
				prefix={<SearchOutlined />}
				placeholder="搜索人员"
				value={keyword}
				onChange={event => setKeyword(event.target.value)}
			/>
			{row.assignedTo ? (
				<div className={styles.clearRow}>
					<Button type="text" size="small" loading={submitting} onClick={() => void handleClear()}>
						清除已选
					</Button>
				</div>
			) : null}
			<div className={styles.list}>
				{loading ? (
					<div className={styles.loadingBox}><Spin size="small" /></div>
				) : ordered.length === 0 ? (
					<Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="无匹配人员" className="!my-2" />
				) : (
					ordered.map(candidate => (
						<div
							key={candidate.id}
							className={`${styles.item} ${String(row.assignedTo) === candidate.id ? styles.selected : ""}`}
							onClick={() => void handleSelect(candidate)}
						>
							<Avatar shape="square" size={22} className={styles.avatar}>
								{displayName(candidate).slice(0, 1)}
							</Avatar>
							<span className="truncate">{displayName(candidate)}</span>
						</div>
					))
				)}
			</div>
		</div>
	);

	if (!editable) {
		return trigger;
	}
	return (
		<Dropdown
			open={open}
			onOpenChange={next => {
				setOpen(next);
				if (!next) {
					setKeyword("");
				}
			}}
			trigger={["click"]}
			placement="bottom"
			popupRender={() => panel}
		>
			{trigger}
		</Dropdown>
	);
}
