import type {
	TicketAttribute,
	TicketAttributeSlot,
	TicketStatusDefinition,
	TicketStatusFlow,
	TicketStatusFlowState,
	TransitionRule,
} from "@uniondesk/shared";
import { fetchPlatformTicketStatuses, toErrorMessage, updateDomainTicketType, updatePlatformTicketType } from "@uniondesk/shared";

import { AuthGuarded } from "#src/components/auth-guarded";
import {
	PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_UPDATE,
} from "#src/pages/platform/domains/platform-domain-permissions";

import {
	App,
	Dropdown,
	Empty,
	Modal,
	Spin,
} from "antd";
import {
	PlusOutlined,
	ApartmentOutlined,
	SettingOutlined,
} from "@ant-design/icons";
import { useCallback, useEffect, useRef, useState } from "react";

import { AddStateModal } from "./workflow/add-state-modal";
import { AddStepModal } from "./workflow/add-step-modal";
import { RuleConfigDrawer } from "./workflow/rule-config-drawer";
import { TypedRuleConfigModal } from "./workflow/typed-rule-config-modal";
import { useWorkflowDraft } from "./workflow/use-workflow-draft";
import { isInitialState, normalizeStatusFlow, pickFallbackInitialCode } from "./workflow/workflow-initial-state";
import { WorkflowMatrixView } from "./workflow/workflow-matrix-view";
import { WorkflowListView } from "./workflow/workflow-list-view";
import {
	buildRuleKindMenuItems,
	type WorkflowRuleKind,
} from "./workflow/workflow-rule-kinds";

import "./workflow-tab.less";
import "./workflow/typed-rule-config-modal.less";

type ViewMode = "matrix" | "list";

interface WorkflowTabProps {
	loading: boolean;
	domainId: string;
	ticketType: { id: string; name: string; status_flow?: TicketStatusFlow | Record<string, unknown> | null; transition_rules?: TransitionRule[] } | null;
	availableAttributes: TicketAttribute[];
	slots: Array<TicketAttributeSlot & { dragId: string }>;
	canUpdate?: boolean;
	onRefresh: () => void;
}

export function WorkflowTab({
	loading,
	domainId,
	ticketType,
	availableAttributes,
	slots,
	canUpdate = true,
	onRefresh,
}: WorkflowTabProps) {
	const { message, modal } = App.useApp();
	const isPlatformLevel = !domainId || domainId === "0";

	// 状态
	const [statusFlow, setStatusFlow] = useState<TicketStatusFlow>(normalizeStatusFlow(null));
	const [rules, setRules] = useState<TransitionRule[]>([]);
	const [viewMode, setViewMode] = useState<ViewMode>("matrix");
	const [isDirty, setIsDirty] = useState(false);
	const [saving, setSaving] = useState(false);
	const [platformStatuses, setPlatformStatuses] = useState<TicketStatusDefinition[]>([]);

	// Modal/Drawer 状态
	const [addStateOpen, setAddStateOpen] = useState(false);
	const [addStepOpen, setAddStepOpen] = useState(false);
	const [addStepPreset, setAddStepPreset] = useState<{ fromCode: string; toCode: string } | null>(null);
	const [selectedRule, setSelectedRule] = useState<TransitionRule | null>(null);
	const [drawerOpen, setDrawerOpen] = useState(false);
	const [typedRuleOpen, setTypedRuleOpen] = useState(false);
	const [typedRuleKind, setTypedRuleKind] = useState<WorkflowRuleKind | null>(null);
	const [typedRuleRequireStep, setTypedRuleRequireStep] = useState(true);
	const [typedRuleStepKey, setTypedRuleStepKey] = useState<{
		from_state_code: string;
		to_state_code: string;
	} | null>(null);

	const openTypedRuleModal = useCallback((
		kind: WorkflowRuleKind,
		options?: { rule?: TransitionRule },
	) => {
		setTypedRuleKind(kind);
		if (options?.rule) {
			setTypedRuleRequireStep(false);
			setTypedRuleStepKey({
				from_state_code: options.rule.from_state_code,
				to_state_code: options.rule.to_state_code,
			});
		}
		else {
			setTypedRuleRequireStep(true);
			setTypedRuleStepKey(null);
		}
		setTypedRuleOpen(true);
	}, []);

	const openStepSettings = useCallback((rule: TransitionRule) => {
		setSelectedRule(rule);
		setDrawerOpen(true);
	}, []);

	// 本地草稿
	const { hasDraft, draftAge, saveDraft, loadDraft, clearDraft } = useWorkflowDraft(domainId, ticketType?.id ?? "");
	const draftSaveRef = useRef<ReturnType<typeof setInterval> | null>(null);

	// 初始化加载
	useEffect(() => {
		if (ticketType?.status_flow && typeof ticketType.status_flow === "object") {
			setStatusFlow(normalizeStatusFlow(ticketType.status_flow));
		}
		else {
			setStatusFlow(normalizeStatusFlow(null));
		}
		setRules(ticketType?.transition_rules ?? []);
	}, [ticketType]);

	// 加载平台状态字典
	useEffect(() => {
		void fetchPlatformTicketStatuses({ page_size: 100 }).then(res => {
			setPlatformStatuses(res.items.filter(s => s.status === "active"));
		}).catch(() => {
			// 忽略错误
		});
	}, []);

	// 脏检测
	useEffect(() => {
		if (!ticketType) return;
		const originalFlow = (ticketType.status_flow as TicketStatusFlow) ?? { states: [], transitions: [] };
		const originalRules = ticketType.transition_rules ?? [];
		const flowChanged = JSON.stringify(statusFlow) !== JSON.stringify(originalFlow);
		const rulesChanged = JSON.stringify(rules) !== JSON.stringify(originalRules);
		setIsDirty(flowChanged || rulesChanged);
	}, [statusFlow, rules, ticketType]);

	// 定时自动保存草稿
	useEffect(() => {
		if (!isDirty) return;
		draftSaveRef.current = setInterval(() => {
			saveDraft(statusFlow, rules);
			message.info("工作流草稿已自动保存");
		}, 5 * 60 * 1000);
		return () => {
			if (draftSaveRef.current) {
				clearInterval(draftSaveRef.current);
			}
		};
	}, [isDirty, statusFlow, rules, saveDraft, message]);

	// 恢复草稿提示
	useEffect(() => {
		if (hasDraft && draftAge && draftAge < 3 * 24 * 60 * 60 * 1000) {
			modal.confirm({
				title: "恢复草稿",
				content: "检测到未保存的工作流草稿，是否恢复？",
				okText: "恢复",
				cancelText: "丢弃",
				onOk: () => {
					const draft = loadDraft();
					if (draft) {
						setStatusFlow(normalizeStatusFlow(draft.statusFlow));
						setRules(draft.rules);
						message.success("草稿已恢复");
					}
				},
				onCancel: () => {
					clearDraft();
				},
			});
		}
	}, [hasDraft, draftAge, loadDraft, clearDraft, modal, message]);

	// 获取状态名称
	const getStateName = useCallback((code: string) => {
		const state = statusFlow.states.find(s => s.code === code);
		return state?.name ?? code;
	}, [statusFlow.states]);

	// 添加状态
	const handleAddState = useCallback((statusDef: TicketStatusDefinition, addGlobalTransitions: boolean) => {
		const newState: TicketStatusFlowState = {
			code: statusDef.code,
			name: statusDef.name,
			state_type: statusDef.state_type,
		};
		setStatusFlow(prev => {
			const newStates = [...prev.states, newState];
			let newTransitions = [...prev.transitions];
			if (addGlobalTransitions && prev.states.length > 0) {
				prev.states.forEach(s => {
					newTransitions.push({ from: s.code, to: statusDef.code });
				});
			}
			return {
				states: newStates,
				transitions: newTransitions,
				// 首个状态自动成为唯一初始；后续添加不改初始
				initial_state_code: prev.states.length === 0
					? statusDef.code
					: (prev.initial_state_code ?? statusDef.code),
			};
		});
		if (addGlobalTransitions && statusFlow.states.length > 0) {
			const newRules: TransitionRule[] = statusFlow.states.map(s => ({
				from_state_code: s.code,
				to_state_code: statusDef.code,
				step_name: statusDef.name,
				permission_mode: "none",
				member_ids: [],
				role_ids: [],
				required_slot_ids: [],
				attribute_updates: [],
				additional_attributes: [],
			}));
			setRules(prev => [...prev, ...newRules]);
		}
	}, [statusFlow.states]);

	// 移除状态
	const handleRemoveState = useCallback((stateCode: string) => {
		modal.confirm({
			title: "确认移除状态",
			content: `移除状态「${getStateName(stateCode)}」将同时删除所有关联的步骤，是否继续？`,
			okText: "移除",
			okButtonProps: { danger: true },
			onOk: () => {
				setStatusFlow(prev => {
					const states = prev.states.filter(s => s.code !== stateCode);
					const transitions = prev.transitions.filter(t => t.from !== stateCode && t.to !== stateCode);
					const initial_state_code = prev.initial_state_code === stateCode
						? pickFallbackInitialCode(states)
						: pickFallbackInitialCode(states, prev.initial_state_code);
					return { states, transitions, initial_state_code };
				});
				setRules(prev => prev.filter(r => r.from_state_code !== stateCode && r.to_state_code !== stateCode));
			},
		});
	}, [getStateName, modal]);

	// 设置初始状态（严格单初始，仅改显式字段）
	const handleSetInitialState = useCallback((stateCode: string) => {
		if (isInitialState(statusFlow, stateCode)) {
			message.info(`「${getStateName(stateCode)}」已经是初始状态`);
			return;
		}
		const currentInitial = statusFlow.states.find(s => s.code === statusFlow.initial_state_code);
		const apply = () => {
			setStatusFlow(prev => ({
				...prev,
				initial_state_code: stateCode,
			}));
			message.success(`已将「${getStateName(stateCode)}」设置为初始状态`);
		};
		if (currentInitial && currentInitial.code !== stateCode) {
			modal.confirm({
				title: "替换初始状态",
				content: `当前初始状态为「${currentInitial.name}」，是否替换为「${getStateName(stateCode)}」？`,
				okText: "替换",
				cancelText: "取消",
				onOk: apply,
			});
			return;
		}
		apply();
	}, [statusFlow, getStateName, message, modal]);

	// 创建步骤
	const handleAddStep = useCallback((fromCode: string, toCode: string, stepName?: string) => {
		if (!fromCode || !toCode) return;
		const resolvedStepName = stepName?.trim() || getStateName(toCode);
		setStatusFlow(prev => ({
			...prev,
			transitions: [...prev.transitions, { from: fromCode, to: toCode }],
		}));
		setRules(prev => [...prev, {
			from_state_code: fromCode,
			to_state_code: toCode,
			step_name: resolvedStepName,
			permission_mode: "none",
			member_ids: [],
			role_ids: [],
			required_slot_ids: [],
			attribute_updates: [],
			additional_attributes: [],
		}]);
	}, [getStateName]);

	const openAddStepModal = useCallback((fromCode?: string, toCode?: string) => {
		if (fromCode && toCode) {
			setAddStepPreset({ fromCode, toCode });
		} else {
			setAddStepPreset(null);
		}
		setAddStepOpen(true);
	}, []);

	// 删除步骤
	const handleRemoveStep = useCallback((rule: TransitionRule) => {
		modal.confirm({
			title: "确认删除步骤",
			content: `删除步骤「${rule.step_name}」(${rule.from_state_code} → ${rule.to_state_code})？`,
			okText: "删除",
			okButtonProps: { danger: true },
			onOk: () => {
				setStatusFlow(prev => ({
					...prev,
					transitions: prev.transitions.filter(
						t => !(t.from === rule.from_state_code && t.to === rule.to_state_code)
					),
				}));
				setRules(prev => prev.filter(
					r => !(r.from_state_code === rule.from_state_code && r.to_state_code === rule.to_state_code)
				));
				setDrawerOpen(false);
				setSelectedRule(null);
			},
		});
	}, [modal]);

	// 更新规则
	const handleUpdateRule = useCallback((fromCode: string, toCode: string, patch: Partial<TransitionRule>) => {
		setRules(prev => prev.map(r => {
			if (r.from_state_code === fromCode && r.to_state_code === toCode) {
				return { ...r, ...patch };
			}
			return r;
		}));
	}, []);

	// 应用配置
	const handleSave = useCallback(async () => {
		if (!ticketType) return;
		setSaving(true);
		try {
			const payload = {
				status_flow: statusFlow,
				transition_rules: rules.map(r => ({
					from_state_code: r.from_state_code,
					to_state_code: r.to_state_code,
					step_name: r.step_name,
					permission_mode: r.permission_mode,
					member_ids: r.member_ids,
					role_ids: r.role_ids,
					required_slot_ids: r.required_slot_ids,
					attribute_updates: r.attribute_updates,
					additional_attributes: r.additional_attributes ?? [],
				})),
			};
			if (isPlatformLevel) {
				await updatePlatformTicketType(ticketType.id, payload);
			}
			else {
				await updateDomainTicketType(domainId, ticketType.id, payload);
			}
			clearDraft();
			message.success("工作流已保存");
			onRefresh();
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
		finally {
			setSaving(false);
		}
	}, [domainId, isPlatformLevel, ticketType, statusFlow, rules, clearDraft, message, onRefresh]);

	// 取消修改
	const handleCancel = useCallback(() => {
		if (!ticketType) return;
		setStatusFlow(normalizeStatusFlow(ticketType.status_flow));
		setRules(ticketType.transition_rules ?? []);
		clearDraft();
		message.info("已取消修改，恢复到上次保存的状态");
	}, [ticketType, clearDraft, message]);


	if (loading) {
		return (
			<div className="flex justify-center py-16">
				<Spin />
			</div>
		);
	}

	if (!ticketType) {
		return <Empty description="未找到事项类型" />;
	}

	return (
		<div className="workflow-container">
			{/* 工具栏 */}
			<div className="workflow-toolbar">
				{/* 视图切换 Tab */}
				<div className="view-tabs">
					<div
						className={`view-tab ${viewMode === "matrix" ? "active" : ""}`}
						onClick={() => setViewMode("matrix")}
					>
						表格视图
					</div>
					<div
						className={`view-tab ${viewMode === "list" ? "active" : ""}`}
						onClick={() => setViewMode("list")}
					>
						列表视图
					</div>
				</div>
				{/* 操作按钮 */}
				<div className="toolbar-actions">
					<AuthGuarded auth={PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_UPDATE} fallback={null}>
						<button
							className="action-btn"
							disabled={!canUpdate}
							onClick={() => setAddStateOpen(true)}
						>
							<span className="action-icon"><PlusOutlined /></span>
							添加状态
						</button>
					</AuthGuarded>
					<AuthGuarded auth={PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_UPDATE} fallback={null}>
						<button
							className="action-btn"
							disabled={!canUpdate || statusFlow.states.length < 2}
							onClick={() => openAddStepModal()}
						>
							<span className="action-icon"><ApartmentOutlined /></span>
							创建步骤
						</button>
					</AuthGuarded>
					<AuthGuarded auth={PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_UPDATE} fallback={null}>
						<Dropdown
							overlayClassName="rule-kind-dropdown"
							menu={{
								items: buildRuleKindMenuItems(isPlatformLevel, {
									onSelectKind: kind => openTypedRuleModal(kind),
								}),
							}}
							trigger={["click"]}
							disabled={!canUpdate || rules.length === 0}
						>
							<button
								className="action-btn"
								disabled={!canUpdate || rules.length === 0}
								type="button"
							>
								<span className="action-icon"><SettingOutlined /></span>
								配置规则
							</button>
						</Dropdown>
					</AuthGuarded>
				</div>
			</div>

			{/* 视图区域 */}
			<div className="workflow-view-area">
				{viewMode === "matrix" ? (
					<WorkflowMatrixView
						statusFlow={statusFlow}
						rules={rules}
						canUpdate={canUpdate}
						onAddStep={openAddStepModal}
						onOpenStepSettings={openStepSettings}
						onDeleteRule={handleRemoveStep}
						onReorderStates={(newStates) => {
							setStatusFlow(prev => ({ ...prev, states: newStates }));
						}}
						onSetInitialState={handleSetInitialState}
						onRemoveState={handleRemoveState}
					/>
				) : (
					<WorkflowListView
						statusFlow={statusFlow}
						rules={rules}
						canUpdate={canUpdate}
						onCreateStep={() => openAddStepModal()}
						onOpenStepSettings={openStepSettings}
						onDeleteRule={handleRemoveStep}
						onRemoveState={handleRemoveState}
						onReorderStates={(newStates) => {
							setStatusFlow(prev => ({ ...prev, states: newStates }));
						}}
						onReorderRules={(fromStateCode, newRules) => {
							// 替换指定 fromStateCode 的规则
							setRules(prev => [
								...prev.filter(r => r.from_state_code !== fromStateCode),
								...newRules,
							]);
						}}
						onSetInitialState={handleSetInitialState}
					/>
				)}
			</div>

			{/* 底部应用配置栏 */}
			<div className="workflow-footer">
				{isDirty && (
					<button className="cancel-btn" onClick={handleCancel}>
						取消
					</button>
				)}
				<AuthGuarded auth={PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_UPDATE} fallback={null}>
					<button
						className={`apply-btn ${isDirty ? "enabled" : ""}`}
						disabled={!isDirty || saving}
						onClick={handleSave}
					>
						{saving ? "保存中..." : "应用配置"}
					</button>
				</AuthGuarded>
			</div>

			{/* Modal & Drawer */}
			<AddStateModal
				open={addStateOpen}
				platformStatuses={platformStatuses}
				existingStateCodes={statusFlow.states.map(s => s.code)}
				onCancel={() => setAddStateOpen(false)}
				onOk={handleAddState}
			/>
			<AddStepModal
				open={addStepOpen}
				states={statusFlow.states}
				existingTransitions={statusFlow.transitions}
				initialFromCode={addStepPreset?.fromCode}
				initialToCode={addStepPreset?.toCode}
				onCancel={() => {
					setAddStepOpen(false);
					setAddStepPreset(null);
				}}
				onOk={(fromCode, toCode, stepName) => {
					handleAddStep(fromCode, toCode, stepName);
					setAddStepOpen(false);
					setAddStepPreset(null);
				}}
			/>
			<TypedRuleConfigModal
				open={typedRuleOpen}
				ruleKind={typedRuleKind}
				requireStepSelect={typedRuleRequireStep}
				initialStepKey={typedRuleStepKey}
				rules={rules}
				states={statusFlow.states}
				slots={slots}
				availableAttributes={availableAttributes}
				isPlatformLevel={isPlatformLevel}
				domainId={domainId}
				onCancel={() => {
					setTypedRuleOpen(false);
					setTypedRuleKind(null);
					setTypedRuleStepKey(null);
				}}
				onUpdate={handleUpdateRule}
			/>
			<RuleConfigDrawer
				open={drawerOpen}
				rule={selectedRule}
				states={statusFlow.states}
				slots={slots}
				availableAttributes={availableAttributes}
				isPlatformLevel={isPlatformLevel}
				domainId={domainId}
				onClose={() => {
					setDrawerOpen(false);
					setSelectedRule(null);
				}}
				onUpdate={handleUpdateRule}
				onDelete={handleRemoveStep}
			/>
		</div>
	);
}
