import type { TicketStatusFlow, TicketStatusFlowState, TransitionRule } from "@uniondesk/shared";

import {
	DndContext,
	closestCenter,
	KeyboardSensor,
	PointerSensor,
	useSensor,
	useSensors,
	type DragEndEvent,
} from "@dnd-kit/core";
import { arrayMove, SortableContext, sortableKeyboardCoordinates, useSortable, verticalListSortingStrategy } from "@dnd-kit/sortable";
import { CSS } from "@dnd-kit/utilities";

import { Empty, Input, Dropdown } from "antd";
import { DeleteOutlined, SearchOutlined, StarOutlined, StarFilled, EllipsisOutlined, HolderOutlined } from "@ant-design/icons";
import { useMemo, useState, useCallback } from "react";

import { StatusLabelByCode } from "./status-label";
import { isInitialState } from "./workflow-initial-state";
import "./workflow-list-view.less";

interface WorkflowListViewProps {
	statusFlow: TicketStatusFlow;
	rules: TransitionRule[];
	canUpdate: boolean;
	onCreateStep: () => void;
	onOpenStepSettings: (rule: TransitionRule) => void;
	onDeleteRule: (rule: TransitionRule) => void;
	onRemoveState: (stateCode: string) => void;
	onReorderStates?: (states: TicketStatusFlowState[]) => void;
	onReorderRules?: (fromStateCode: string, rules: TransitionRule[]) => void;
	onSetInitialState?: (stateCode: string) => void;
}

const STATE_TYPE_LABEL: Record<string, string> = {
	in_progress: "进行中",
	paused: "暂停",
	terminal: "已完成",
};

const STATE_TYPE_HEADING_COLOR: Record<string, string> = {
	in_progress: "var(--ant-color-primary)",
	paused: "var(--ant-color-warning)",
	terminal: "var(--ant-color-success)",
};

/** 获取权限摘要 */
function getPermissionText(rule: TransitionRule): string {
	if (rule.permission_mode === "none") return "全部成员";
	if (rule.permission_mode === "members") return `${rule.member_ids.length} 位成员`;
	if (rule.permission_mode === "roles") return `${rule.role_ids.length} 个角色`;
	return "全部成员";
}

/**
 * 可排序的状态卡片（大块）
 */
function SortableStateCard({
	state,
	isInitial,
	canUpdate,
	onRemove,
	onSetInitial,
	children,
}: {
	state: TicketStatusFlowState;
	isInitial: boolean;
	canUpdate: boolean;
	onRemove: () => void;
	onSetInitial: () => void;
	children: React.ReactNode;
}) {
	const {
		attributes,
		listeners,
		setNodeRef,
		transform,
		transition,
		isDragging,
	} = useSortable({
		id: `list-state-${state.code}`,
		data: {
			type: "state-card",
			state,
		},
	});

	const style = {
		transform: CSS.Transform.toString(transform),
		transition,
		opacity: isDragging ? 0.5 : 1,
	};

	const menuItems = [
		{
			key: 'set-initial',
			label: isInitial ? '已是初始状态' : '设置为初始状态',
			icon: isInitial ? <StarFilled /> : <StarOutlined />,
			disabled: isInitial,
			onClick: onSetInitial,
		},
		{
			type: 'divider' as const,
		},
		{
			key: 'remove',
			label: '移除',
			icon: <DeleteOutlined />,
			danger: true,
			onClick: onRemove,
		},
	];

	return (
		<div
			ref={setNodeRef}
			style={style}
			className={`state-card ${isDragging ? 'dragging' : ''}`}
			{...attributes}
		>
			<div className="state-card-header">
				<span className="drag-handle" {...listeners}>
					<HolderOutlined />
				</span>
				<span
					className="state-card-title"
					style={{ color: STATE_TYPE_HEADING_COLOR[state.state_type] ?? "var(--ant-color-primary)" }}
				>
					{state.name}
				</span>
				{isInitial && (
					<span className="initial-state-tag">初始状态</span>
				)}
				<span className="state-card-type">{STATE_TYPE_LABEL[state.state_type] ?? ""}</span>
				<div className="state-card-header-spacer" />
				{canUpdate && (
					<>
						<Dropdown
							menu={{ items: menuItems }}
							trigger={['click']}
							placement="bottomRight"
						>
							<span className="state-card-more">
								<EllipsisOutlined />
							</span>
						</Dropdown>
						<span
							className="state-card-delete"
							onClick={onRemove}
						>
							<DeleteOutlined />
						</span>
					</>
				)}
			</div>
			<div className="state-card-body">
				{children}
			</div>
		</div>
	);
}

/**
 * 可排序的步骤行（子块）
 */
function SortableStepRow({
	rule,
	fromState,
	states,
	canUpdate,
	onOpenStepSettings,
	onDelete,
}: {
	rule: TransitionRule;
	fromState: TicketStatusFlowState;
	states: TicketStatusFlowState[];
	canUpdate: boolean;
	onOpenStepSettings: () => void;
	onDelete: () => void;
}) {
	const {
		attributes,
		listeners,
		setNodeRef,
		transform,
		transition,
		isDragging,
	} = useSortable({
		id: `step-${rule.from_state_code}-${rule.to_state_code}`,
		data: {
			type: "step-row",
			rule,
			parentId: fromState.code,
		},
	});

	const style = {
		transform: CSS.Transform.toString(transform),
		transition,
		opacity: isDragging ? 0.5 : 1,
	};

	return (
		<div
			ref={setNodeRef}
			style={style}
			className={`transfer-step ${isDragging ? "dragging" : ""}`}
			{...attributes}
			onClick={() => canUpdate && onOpenStepSettings()}
			onKeyDown={(e) => {
				if (!canUpdate) return;
				if (e.key === "Enter" || e.key === " ") {
					e.preventDefault();
					onOpenStepSettings();
				}
			}}
			role={canUpdate ? "button" : undefined}
			tabIndex={canUpdate ? 0 : undefined}
		>
			<div className="col col-name">
				<span className="drag-handle step-drag-handle" {...listeners}>
					<HolderOutlined />
				</span>
				<span className="step-text">{rule.step_name}</span>
			</div>
			<div className="col col-status">
				<StatusLabelByCode code={rule.from_state_code} states={states} />
			</div>
			<div className="col col-arrow" />
			<div className="col col-status">
				<StatusLabelByCode code={rule.to_state_code} states={states} />
			</div>
			<div className="col col-authority">
				<span className="authority-text">{getPermissionText(rule)}</span>
				{canUpdate && (
					<span
						className="trash-icon"
						onClick={(e) => {
							e.stopPropagation();
							onDelete();
						}}
					>
						<DeleteOutlined />
					</span>
				)}
			</div>
		</div>
	);
}

/**
 * 工作流列表视图
 * 对齐 CODING 事项类型工作流配置的列表布局
 * 支持双层拖拽：大块（状态卡片）+ 子块（步骤行）
 */
export function WorkflowListView({
	statusFlow,
	rules,
	canUpdate,
	onCreateStep,
	onOpenStepSettings,
	onDeleteRule,
	onRemoveState,
	onReorderStates,
	onReorderRules,
	onSetInitialState,
}: WorkflowListViewProps) {
	const [searchKeyword, setSearchKeyword] = useState("");

	// dnd-kit sensors 配置
	const sensors = useSensors(
		useSensor(PointerSensor, {
			activationConstraint: {
				distance: 8,
			},
		}),
		useSensor(KeyboardSensor, {
			coordinateGetter: sortableKeyboardCoordinates,
		})
	);

	// 按状态分组步骤
	const groupedData = useMemo(() => {
		return statusFlow.states.map(state => {
			const stateRules = rules.filter(r => r.from_state_code === state.code);
			const filtered = searchKeyword.trim()
				? stateRules.filter(r =>
					r.step_name.toLowerCase().includes(searchKeyword.toLowerCase().trim())
				)
				: stateRules;
			return { state, rules: filtered, total: stateRules.length };
		});
	}, [statusFlow.states, rules, searchKeyword]);

	// 状态 ID 列表（用于大块排序）
	const stateIds = useMemo(() => statusFlow.states.map(s => s.code), [statusFlow.states]);

	// 处理大块拖拽结束
	const handleStateDragEnd = useCallback((event: DragEndEvent) => {
		const { active, over } = event;
		if (!over || active.id === over.id || !onReorderStates) return;

		const activeId = String(active.id);
		const overId = String(over.id);
		const oldIndex = stateIds.indexOf(activeId.replace("list-state-", ""));
		const newIndex = stateIds.indexOf(overId.replace("list-state-", ""));

		if (oldIndex !== -1 && newIndex !== -1) {
			const newStates = arrayMove(statusFlow.states, oldIndex, newIndex);
			onReorderStates(newStates);
		}
	}, [stateIds, statusFlow.states, onReorderStates]);

	// 处理子块拖拽结束
	const handleStepDragEnd = useCallback((event: DragEndEvent) => {
		const { active, over } = event;
		if (!over || active.id === over.id || !onReorderRules) return;

		const activeId = String(active.id).replace("step-", "");
		const overId = String(over.id).replace("step-", "");

		const [activeFrom, activeTo] = activeId.split("-");
		const [overFrom, overTo] = overId.split("-");

		// 只在同一父级内排序
		if (activeFrom !== overFrom || !onReorderRules) return;

		const fromStateRules = rules.filter(r => r.from_state_code === activeFrom);
		const activeIndex = fromStateRules.findIndex(r =>
			r.from_state_code === activeFrom && r.to_state_code === activeTo
		);
		const overIndex = fromStateRules.findIndex(r =>
			r.from_state_code === overFrom && r.to_state_code === overTo
		);

		if (activeIndex !== -1 && overIndex !== -1) {
			const newRules = arrayMove(fromStateRules, activeIndex, overIndex);
			onReorderRules(activeFrom, newRules);
		}
	}, [rules, onReorderRules]);

	// 统一拖拽处理
	const handleDragEnd = useCallback((event: DragEndEvent) => {
		const activeId = String(event.active.id);
		if (activeId.startsWith("list-state-")) {
			handleStateDragEnd(event);
		}
		else if (activeId.startsWith("step-")) {
			handleStepDragEnd(event);
		}
	}, [handleStateDragEnd, handleStepDragEnd]);

	if (statusFlow.states.length === 0) {
		return (
			<div className="workflow-list-empty">
				<Empty description="暂无状态，请先添加状态" />
			</div>
		);
	}

	return (
		<DndContext
			sensors={sensors}
			collisionDetection={closestCenter}
			onDragEnd={handleDragEnd}
		>
			<div className="workflow-list-container">
				{/* 搜索框 */}
				<div className="list-search-bar">
					<Input
						placeholder="搜索步骤..."
						prefix={<SearchOutlined style={{ color: "var(--ant-color-text-quaternary)" }} />}
						value={searchKeyword}
						onChange={e => setSearchKeyword(e.target.value)}
						allowClear
						style={{ width: 160 }}
					/>
				</div>

				{/* 状态卡片列表 */}
				<SortableContext items={stateIds.map(id => `list-state-${id}`)} strategy={verticalListSortingStrategy}>
					{groupedData.map(({ state, rules: stateRules, total }) => {
						const isInitial = isInitialState(statusFlow, state.code);

						// 子块 ID 列表
						const stepIds = stateRules.map(r => `step-${r.from_state_code}-${r.to_state_code}`);

						return (
							<SortableContext key={state.code} items={stepIds} strategy={verticalListSortingStrategy}>
								<SortableStateCard
									state={state}
									isInitial={isInitial}
									canUpdate={canUpdate}
									onRemove={() => onRemoveState(state.code)}
									onSetInitial={() => onSetInitialState?.(state.code)}
								>
									{total === 0 ? (
										<div className="no-steps">暂无流转步骤</div>
									) : stateRules.length === 0 ? (
										<div className="no-steps">未找到匹配的步骤</div>
									) : (
										<div className="transfer-box-wrapper">
											<div className="transfer-box">
												{/* 表头 */}
												<div className="transfer-header">
													<div className="col col-name">步骤名称</div>
													<div className="col col-status">开始状态</div>
													<div className="col col-arrow" />
													<div className="col col-status">目标状态</div>
													<div className="col col-authority">用户权限</div>
												</div>
												{/* 步骤行 */}
												{stateRules.map(rule => (
													<SortableStepRow
														key={`${rule.from_state_code}-${rule.to_state_code}`}
														rule={rule}
														fromState={state}
														states={statusFlow.states}
														canUpdate={canUpdate}
														onOpenStepSettings={() => onOpenStepSettings(rule)}
														onDelete={() => onDeleteRule(rule)}
													/>
												))}
											</div>
										</div>
									)}

									{/* 创建步骤按钮 */}
									{canUpdate && (
										<div className="transfer-add">
											<span className="add-btn" onClick={onCreateStep}>
												创建步骤
											</span>
										</div>
									)}
								</SortableStateCard>
							</SortableContext>
						);
					})}
				</SortableContext>
			</div>
		</DndContext>
	);
}
