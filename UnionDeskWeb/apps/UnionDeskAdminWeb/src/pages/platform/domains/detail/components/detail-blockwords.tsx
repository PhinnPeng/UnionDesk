import type { BlockedWord } from "@uniondesk/shared";
import {
	createBlockedWord,
	createBlockedWordsBatch,
	deleteBlockedWord,
	fetchBlockedWordsPage,
	toErrorMessage,
} from "@uniondesk/shared";

import { AuthGuarded } from "#src/components/auth-guarded";
import { ConfirmPopover } from "#src/components/confirm-popover";
import { TableSearchForm } from "#src/components/table-search-form";

import {
	PLATFORM_DOMAIN_CONTROL_BLOCKED_WORD_CREATE,
	PLATFORM_DOMAIN_CONTROL_BLOCKED_WORD_DELETE,
} from "../../platform-domain-permissions";

import { DOMAIN_BLOCKED_WORD_READ_PERMISSION } from "./detail-shared";

import { PlusOutlined, ReloadOutlined, SearchOutlined } from "@ant-design/icons";
import {
	App,
	Button,
	Card,
	Empty,
	Form,
	Input,
	Modal,
	Space,
	Table,
	Typography,
} from "antd";
import type { TableColumnsType } from "antd";
import { useCallback, useEffect, useMemo, useState } from "react";

const { Title } = Typography;

export interface DetailBlockwordsProps {
	domainId: string;
}

interface BlockwordsSearchValues {
	keyword?: string;
}

function parseBatchInput(raw: string): string[] {
	return [...new Set(
		raw.split(/[\n,，、]+/)
			.map(item => item.trim())
			.filter(Boolean),
	)];
}

function formatBatchMessage(createdCount: number, skipped: { word: string; reason: string }[]): string {
	if (skipped.length === 0) {
		return `成功添加 ${createdCount} 条屏蔽词`;
	}
	const preview = skipped.slice(0, 3).map(item => item.word).join("、");
	const suffix = skipped.length > 3 ? " 等" : "";
	return `成功添加 ${createdCount} 条，跳过 ${skipped.length} 条重复：${preview}${suffix}`;
}

export function DetailBlockwords({ domainId }: DetailBlockwordsProps) {
	const { message } = App.useApp();
	const [loading, setLoading] = useState(false);
	const [submitting, setSubmitting] = useState(false);
	const [rows, setRows] = useState<BlockedWord[]>([]);
	const [total, setTotal] = useState(0);
	const [page, setPage] = useState(1);
	const [pageSize, setPageSize] = useState(20);
	const [keyword, setKeyword] = useState("");
	const [addOpen, setAddOpen] = useState(false);
	const [batchOpen, setBatchOpen] = useState(false);
	const [singleWord, setSingleWord] = useState("");
	const [batchText, setBatchText] = useState("");

	const loadWords = useCallback(async (nextPage = page, nextPageSize = pageSize, nextKeyword = keyword) => {
		if (!domainId) {
			setRows([]);
			setTotal(0);
			return;
		}
		setLoading(true);
		try {
			const result = await fetchBlockedWordsPage(domainId, {
				page: nextPage,
				page_size: nextPageSize,
				keyword: nextKeyword.trim() || undefined,
			});
			setRows(result.list);
			setTotal(result.total);
			setPage(nextPage);
			setPageSize(nextPageSize);
			setKeyword(nextKeyword);
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
		finally {
			setLoading(false);
		}
	}, [domainId, keyword, message, page, pageSize]);

	useEffect(() => {
		void loadWords(1, 20, "");
	// eslint-disable-next-line react-hooks/exhaustive-deps -- domainId 变化时初始化
	}, [domainId]);

	const handleSearch = useCallback((values: BlockwordsSearchValues) => {
		void loadWords(1, pageSize, values.keyword ?? "");
	}, [loadWords, pageSize]);

	const handleResetSearch = useCallback(() => {
		void loadWords(1, pageSize, "");
	}, [loadWords, pageSize]);

	const handleAddSingle = async () => {
		const word = singleWord.trim();
		if (!word) {
			message.warning("请输入屏蔽词");
			return;
		}
		setSubmitting(true);
		try {
			await createBlockedWord(domainId, word);
			message.success("已添加屏蔽词");
			setAddOpen(false);
			setSingleWord("");
			await loadWords(page, pageSize, keyword);
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
		finally {
			setSubmitting(false);
		}
	};

	const handleAddBatch = async () => {
		const words = parseBatchInput(batchText);
		if (words.length === 0) {
			message.warning("请至少输入一个屏蔽词");
			return;
		}
		setSubmitting(true);
		try {
			const result = await createBlockedWordsBatch(domainId, words);
			message.success(formatBatchMessage(result.created_count, result.skipped));
			setBatchOpen(false);
			setBatchText("");
			await loadWords(page, pageSize, keyword);
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
		finally {
			setSubmitting(false);
		}
	};

	const handleDelete = useCallback(async (wordId: string) => {
		try {
			await deleteBlockedWord(domainId, wordId);
			message.success("已删除屏蔽词");
			await loadWords(page, pageSize, keyword);
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
	}, [domainId, keyword, loadWords, message, page, pageSize]);

	const columns: TableColumnsType<BlockedWord> = useMemo(() => [
		{ title: "屏蔽词", dataIndex: "word", ellipsis: true },
		{ title: "创建时间", dataIndex: "created_at", width: 180, render: value => value ?? "—" },
		{
			title: "操作",
			key: "actions",
			width: 100,
			fixed: "right",
			render: (_, row) => (
				<AuthGuarded auth={PLATFORM_DOMAIN_CONTROL_BLOCKED_WORD_DELETE} fallback={null}>
					<ConfirmPopover
						title="确认删除该屏蔽词？"
						onConfirm={() => handleDelete(row.id)}
					>
						<Button type="link" size="small" danger>删除</Button>
					</ConfirmPopover>
				</AuthGuarded>
			),
		},
	], [handleDelete]);

	return (
		<AuthGuarded auth={DOMAIN_BLOCKED_WORD_READ_PERMISSION} fallback={<Empty description="无权限查看屏蔽词库" />}>
			<div>
				<Title level={5} className="!mb-4">屏蔽词库</Title>
				<div className="flex flex-col gap-4">
					<Card
						bordered={false}
						title={(
							<Space>
								<SearchOutlined />
								<span>筛选条件</span>
							</Space>
						)}
					>
						<TableSearchForm<BlockwordsSearchValues>
							loading={loading}
							initialValues={{ keyword: "" }}
							onFinish={handleSearch}
							onReset={handleResetSearch}
						>
							<Form.Item name="keyword" label="屏蔽词">
								<Input allowClear placeholder="输入屏蔽词关键字" disabled={loading} />
							</Form.Item>
						</TableSearchForm>
					</Card>

					<Card
						bordered={false}
						title="屏蔽词列表"
						extra={(
							<Space>
								<Button icon={<ReloadOutlined />} onClick={() => void loadWords(page, pageSize, keyword)}>
									刷新
								</Button>
								<AuthGuarded auth={PLATFORM_DOMAIN_CONTROL_BLOCKED_WORD_CREATE} fallback={null}>
									<Button type="primary" icon={<PlusOutlined />} onClick={() => setAddOpen(true)}>
										添加屏蔽词
									</Button>
									<Button onClick={() => setBatchOpen(true)}>批量添加</Button>
								</AuthGuarded>
							</Space>
						)}
					>
						<Table<BlockedWord>
							rowKey="id"
							loading={loading}
							columns={columns}
							dataSource={rows}
							pagination={{
								current: page,
								pageSize,
								total,
								showSizeChanger: true,
								showTotal: value => `共 ${value} 条`,
								onChange: (nextPage, nextPageSize) => {
									void loadWords(nextPage, nextPageSize, keyword);
								},
							}}
							locale={{ emptyText: <Empty description="暂无屏蔽词" /> }}
						/>
					</Card>
				</div>

				<Modal
					title="添加屏蔽词"
					open={addOpen}
					confirmLoading={submitting}
					okText="确认添加"
					cancelText="取消"
					onCancel={() => {
						setAddOpen(false);
						setSingleWord("");
					}}
					onOk={() => void handleAddSingle()}
				>
					<Input
						value={singleWord}
						placeholder="输入屏蔽词"
						maxLength={128}
						onChange={event => setSingleWord(event.target.value)}
						onPressEnter={() => void handleAddSingle()}
					/>
				</Modal>

				<Modal
					title="批量添加屏蔽词"
					open={batchOpen}
					width={560}
					confirmLoading={submitting}
					okText="确认添加"
					cancelText="取消"
					onCancel={() => {
						setBatchOpen(false);
						setBatchText("");
					}}
					onOk={() => void handleAddBatch()}
				>
					<p className="mb-2 text-xs text-gray-500">每行一个，或用逗号、顿号分隔；重复词条将自动跳过</p>
					<Input.TextArea
						value={batchText}
						rows={8}
						placeholder={"广告\n违禁词, spam"}
						onChange={event => setBatchText(event.target.value)}
					/>
				</Modal>
			</div>
		</AuthGuarded>
	);
}
