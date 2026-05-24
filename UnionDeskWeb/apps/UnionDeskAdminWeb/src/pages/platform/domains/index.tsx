import type { AdminDomain } from "@uniondesk/shared";
import {
	P0_STEP_UP_OPERATION,
	deleteAdminDomain,
	fetchAdminDomainsPage,
	updateAdminDomain,
	toErrorMessage,
} from "@uniondesk/shared";

import StepUpModal from "#src/components/step-up-modal";
import { AuthGuarded } from "#src/components/auth-guarded";
import { BasicContent } from "#src/components/basic-content";

import { PlusOutlined, ReloadOutlined } from "@ant-design/icons";
import { App, Button, Card, Empty, Pagination, Space, Spin } from "antd";
import { useCallback, useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router";

import { DomainCard } from "./components/domain-card";
import { DomainCreateDrawer } from "./components/domain-create-drawer";
import { DomainEditDrawer } from "./components/domain-edit-drawer";
import { SearchPanel, type DomainSearchValues } from "./components/search-panel";
import { PLATFORM_DOMAINS_CREATE_QUERY } from "./domain-routes";
import { openPlatformDomainDetailTab } from "./open-domain-tab";

export default function PlatformBusinessDomains() {
	const { message, modal } = App.useApp();
	const navigate = useNavigate();
	const [searchParams, setSearchParams] = useSearchParams();

	const [rows, setRows] = useState<AdminDomain[]>([]);
	const [total, setTotal] = useState(0);
	const [loading, setLoading] = useState(false);
	const [page, setPage] = useState(1);
	const [pageSize, setPageSize] = useState(20);
	const [searchValues, setSearchValues] = useState<DomainSearchValues>({});

	const [createDrawerOpen, setCreateDrawerOpen] = useState(false);
	const [editingDomain, setEditingDomain] = useState<AdminDomain | null>(null);
	const [stepUpOpen, setStepUpOpen] = useState(false);
	const [pendingDeleteId, setPendingDeleteId] = useState<string | null>(null);

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
		if (searchParams.get(PLATFORM_DOMAINS_CREATE_QUERY) === "1") {
			setCreateDrawerOpen(true);
			const next = new URLSearchParams(searchParams);
			next.delete(PLATFORM_DOMAINS_CREATE_QUERY);
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
		openPlatformDomainDetailTab(navigate, domain.id, domain.name);
	};

	const handleToggleStatus = (domain: AdminDomain) => {
		const enabled = domain.status === "1" || domain.status === "active" || domain.status === "enabled";
		const nextStatus = enabled ? 0 : 1;
		const actionText = enabled ? "禁用" : "启用";

		modal.confirm({
			title: `${actionText}业务域`,
			content: `确认${actionText}业务域「${domain.name}」吗？`,
			onOk: async () => {
				try {
					await updateAdminDomain(domain.id, { status: nextStatus });
					message.success(`已${actionText}业务域`);
					await reload();
				}
				catch (error) {
					message.error(toErrorMessage(error));
				}
			},
		});
	};

	const requestDelete = (domain: AdminDomain) => {
		setPendingDeleteId(domain.id);
		setStepUpOpen(true);
	};

	const afterStepUp = async (token: string) => {
		setStepUpOpen(false);
		if (!pendingDeleteId) {
			return;
		}
		const id = pendingDeleteId;
		setPendingDeleteId(null);
		try {
			await deleteAdminDomain(id, { stepUpToken: token });
			message.success("业务域已删除");
			await reload();
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
	};

	const handleCreated = async ({ id, name }: { id: string; name: string }) => {
		await reload();
		openPlatformDomainDetailTab(navigate, id, name, "basic");
	};

	const handleDomainSaved = (saved: AdminDomain) => {
		setRows(prev => prev.map(item => (item.id === saved.id ? saved : item)));
	};

	return (
		<BasicContent className="h-full bg-colorBgLayout">
			<div className="flex h-full flex-col gap-4 overflow-hidden">
				<SearchPanel onSearch={handleSearch} onReset={handleReset} loading={loading} />

				<Card
					className="min-h-0 flex-1"
					styles={{ body: { display: "flex", flexDirection: "column", height: "100%", overflow: "hidden" } }}
					title="业务域列表"
					extra={(
						<Space>
							<Button icon={<ReloadOutlined />} onClick={() => reload()}>刷新</Button>
							<AuthGuarded auth="domain.admin.create">
								<Button
									type="primary"
									icon={<PlusOutlined />}
									onClick={() => setCreateDrawerOpen(true)}
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
										<DomainCard
											key={item.id}
											domain={item}
											onManage={handleManage}
											onEdit={setEditingDomain}
											onToggleStatus={handleToggleStatus}
											onDelete={requestDelete}
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

			<DomainCreateDrawer
				open={createDrawerOpen}
				onClose={() => setCreateDrawerOpen(false)}
				onCreated={handleCreated}
			/>

			<DomainEditDrawer
				open={editingDomain != null}
				domain={editingDomain}
				onClose={() => setEditingDomain(null)}
				onSaved={handleDomainSaved}
			/>

			<StepUpModal
				open={stepUpOpen}
				title="安全验证"
				description="删除业务域为高危操作，请验证身份。"
				operationCode={P0_STEP_UP_OPERATION.DELETE_BUSINESS_DOMAIN}
				onCancel={() => {
					setStepUpOpen(false);
					setPendingDeleteId(null);
				}}
				onVerified={afterStepUp}
			/>
		</BasicContent>
	);
}
