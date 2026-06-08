import type { AdminDomain } from "@uniondesk/shared";
import {
	fetchAdminDomainsPage,
	toErrorMessage,
} from "@uniondesk/shared";

import { AuthGuarded } from "#src/components/auth-guarded";
import { BasicContent } from "#src/components/basic-content";
import { TableSearchForm } from "#src/components/table-search-form";
import { appScopes } from "#src/router/extra-info/app-scope";
import { openAppScopeTab } from "#src/utils/tabbar-utils";

import { PlusOutlined, ReloadOutlined, SearchOutlined } from "@ant-design/icons";
import { App, Button, Card, DatePicker, Empty, Form, Input, Pagination, Space, Spin } from "antd";
import type { Dayjs } from "dayjs";
import { useCallback, useEffect, useState } from "react";
import type { NavigateFunction } from "react-router";
import { useNavigate, useSearchParams } from "react-router";

import { DomainsCard } from "./components/domains-card";
import { DomainsModal } from "./components/domains-modal";
import { PLATFORM_DOMAIN_CREATE } from "./platform-domain-permissions";

const { RangePicker } = DatePicker;

/** 列表筛选表单值 */
export interface DomainSearchValues {
	keyword?: string;
	createdRange?: [Dayjs | null, Dayjs | null] | null;
}

/** 打开业务域控制台顶栏页签 */
function openDomainDetailTab(
	navigate: NavigateFunction,
	domainId: string,
	domainName?: string,
	tab: "overview" | "basic" = "overview",
) {
	const base = `/platform/domains/detail/${encodeURIComponent(domainId)}`;
	const path = tab === "overview" ? base : `${base}?tab=${encodeURIComponent(tab)}`;
	if (domainName?.trim()) {
		openAppScopeTab(appScopes.platform, navigate, path, {
			key: base,
			label: "业务域控制台",
			newTabTitle: `业务域控制台 - ${domainName.trim()}`,
			closable: true,
			draggable: true,
		});
	}
	else {
		navigate(path);
	}
}

export default function PlatformBusinessDomains() {
	const { message } = App.useApp();
	const navigate = useNavigate();
	const [searchParams, setSearchParams] = useSearchParams();

	const [rows, setRows] = useState<AdminDomain[]>([]);
	const [total, setTotal] = useState(0);
	const [loading, setLoading] = useState(false);
	const [page, setPage] = useState(1);
	const [pageSize, setPageSize] = useState(20);
	const [searchValues, setSearchValues] = useState<DomainSearchValues>({});

	const [createWizardOpen, setCreateWizardOpen] = useState(false);

	const reload = useCallback(async (p = page, ps = pageSize, sv = searchValues) => {
		setLoading(true);
		try {
			const res = await fetchAdminDomainsPage({
				page: p,
				page_size: ps,
				keyword: sv.keyword,
				created_from: sv.createdRange?.[0]?.format("YYYY-MM-DDTHH:mm:ss"),
				created_to: sv.createdRange?.[1]?.format("YYYY-MM-DDTHH:mm:ss"),
			});
			setRows(res.list);
			setTotal(res.total);
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
		finally {
			setLoading(false);
		}
	}, [message, page, pageSize, searchValues]);

	useEffect(() => {
		void reload();
	}, [reload]);

	useEffect(() => {
		if (searchParams.get("create") === "1") {
			setCreateWizardOpen(true);
			const next = new URLSearchParams(searchParams);
			next.delete("create");
			setSearchParams(next, { replace: true });
		}
	}, [searchParams, setSearchParams]);

	const handleSearch = (values: DomainSearchValues) => {
		setSearchValues(values);
		setPage(1);
		void reload(1, pageSize, values);
	};

	const handleReset = () => {
		setSearchValues({});
		setPage(1);
		void reload(1, pageSize, {});
	};

	const handleManage = (domain: AdminDomain) => {
		openDomainDetailTab(navigate, domain.id, domain.name);
	};

	const handleCreated = async ({ id, name }: { id: string; name: string }) => {
		await reload();
		openDomainDetailTab(navigate, id, name, "basic");
	};

	return (
		<BasicContent className="h-full bg-colorBgLayout">
			<div className="flex h-full flex-col gap-4 overflow-hidden">
				<Card
					title={(
						<Space>
							<SearchOutlined />
							<span>筛选条件</span>
						</Space>
					)}
					bordered={false}
					className="mb-4"
				>
					<TableSearchForm<DomainSearchValues>
						loading={loading}
						initialValues={{
							keyword: "",
							createdRange: null,
						}}
						onFinish={handleSearch}
						onReset={() => {
							handleReset();
						}}
					>
						<Form.Item name="keyword" label="关键词">
							<Input placeholder="请输入业务域编码或名称" allowClear disabled={loading} />
						</Form.Item>
						<Form.Item name="createdRange" label="创建时间">
							<RangePicker className="w-full" showTime disabled={loading} />
						</Form.Item>
					</TableSearchForm>
				</Card>

				<Card
					className="min-h-0 flex-1"
					styles={{ body: { display: "flex", flexDirection: "column", height: "100%", overflow: "hidden" } }}
					title="业务域列表"
					extra={(
						<Space>
							<Button icon={<ReloadOutlined />} onClick={() => reload()}>刷新</Button>
							<AuthGuarded auth={PLATFORM_DOMAIN_CREATE}>
								<Button
									type="primary"
									icon={<PlusOutlined />}
									onClick={() => setCreateWizardOpen(true)}
								>
									新建业务域
								</Button>
							</AuthGuarded>
						</Space>
					)}
					bordered={false}
				>
					<div className="min-h-0 flex-1 overflow-auto pr-2">
						<Spin spinning={loading}>
							{rows.length === 0 && !loading ? (
								<Empty description="暂无业务域数据" className="py-16" />
							) : (
								<div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
									{rows.map(item => (
										<DomainsCard
											key={item.id}
											domain={item}
											onManage={handleManage}
										/>
									))}
								</div>
							)}
						</Spin>
					</div>

					<div className="mt-4 flex justify-end">
						<Pagination
							current={page}
							pageSize={pageSize}
							total={total}
							onChange={(p, ps) => {
								setPage(p);
								setPageSize(ps);
								void reload(p, ps);
							}}
							showSizeChanger
							showTotal={t => `共 ${t} 条`}
						/>
					</div>
				</Card>
			</div>

			<DomainsModal
				open={createWizardOpen}
				onClose={() => setCreateWizardOpen(false)}
				onCreated={handleCreated}
			/>
		</BasicContent>
	);
}
