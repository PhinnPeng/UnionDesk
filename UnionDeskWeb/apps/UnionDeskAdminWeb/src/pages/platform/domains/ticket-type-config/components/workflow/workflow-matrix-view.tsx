import type { DraggableAttributes } from "@dnd-kit/core";
import type { SyntheticListenerMap } from "@dnd-kit/core/dist/hooks/utilities";
import type { MenuProps } from "antd";
import type { TicketStatusFlow, TicketStatusFlowState, TransitionRule } from "@uniondesk/shared";

import {
	DndContext,
	DragOverlay,
	closestCenter,
	KeyboardSensor,
	PointerSensor,
	useSensor,
	useSensors,
	type DragEndEvent,
	type DragOverEvent,
	type DragStartEvent,
} from "@dnd-kit/core";
import { arrayMove, SortableContext, sortableKeyboardCoordinates, useSortable, verticalListSortingStrategy } from "@dnd-kit/sortable";

import { Empty, Dropdown } from "antd";
import { DeleteOutlined, HolderOutlined, HomeFilled, HomeOutlined, MoreOutlined } from "@ant-design/icons";
import { useMemo, useCallback, useRef, useState } from "react";

import { StatusLabel } from "./status-label";
import { isInitialState } from "./workflow-initial-state";
import "./workflow-matrix-view.less";

interface WorkflowMatrixViewProps {
	statusFlow: TicketStatusFlow;
	rules: TransitionRule[];
	canUpdate: boolean;
	onAddStep: (fromCode: string, toCode: string) => void;
	onOpenStepSettings: (rule: TransitionRule) => void;
	onDeleteRule: (rule: TransitionRule) => void;
	onReorderStates?: (states: TicketStatusFlowState[]) => void;
	onSetInitialState?: (stateCode: string) => void;
	onRemoveState?: (stateCode: string) => void;
}

const ROW_HEADER_WIDTH = 165;
const COL_WIDTH = 136;

const matrixRowStyle = {
	display: "grid",
	gridTemplateColumns: "var(--matrix-grid-columns)",
} as React.CSSProperties;

/** 是否已配置任意规则 */
function hasConfiguredRules(rule: TransitionRule): boolean {
	return rule.permission_mode !== "none"
		|| (rule.member_ids?.length ?? 0) > 0
		|| (rule.role_ids?.length ?? 0) > 0
		|| (rule.required_slot_ids?.length ?? 0) > 0
		|| (rule.additional_attributes?.length ?? 0) > 0
		|| (rule.attribute_updates?.length ?? 0) > 0;
}

interface MatrixStateCellProps {
	state: TicketStatusFlowState;
	variant: "header" | "row";
	isInitial: boolean;
	canUpdate: boolean;
	menuItems: MenuProps["items"];
	isOverlay?: boolean;
	dragListeners?: SyntheticListenerMap;
	dragAttributes?: DraggableAttributes;
}

/** 行头/列头状态块内容 */
function MatrixStateCell({
	state,
	variant,
	isInitial,
	canUpdate,
	menuItems,
	isOverlay = false,
	dragListeners,
	dragAttributes,
}: MatrixStateCellProps) {
	const showActions = variant === "row" && canUpdate && !isOverlay;
	const showDragHandle = variant === "row" && !!dragListeners && !isOverlay;

	return (
		<div
			className={[
				"matrix-state-cell",
				`matrix-state-cell--${variant}`,
				isOverlay ? "matrix-state-cell--overlay" : "",
			].filter(Boolean).join(" ")}
		>
			<div className={`matrix-state-cell__inner ${variant === "row" ? "matrix-state-cell__inner--row" : "matrix-state-cell__inner--header"}`}>
				{variant === "row" && isInitial && (
					<span className="matrix-state-cell__initial-badge" title="初始状态">
						<HomeFilled />
					</span>
				)}
				{(showDragHandle || isOverlay) && (
					<span
						className={`matrix-state-cell__drag ${isOverlay ? "matrix-state-cell__drag--visible" : ""}`}
						{...(isOverlay ? {} : dragAttributes)}
						{...(isOverlay ? {} : dragListeners)}
						onClick={(e) => e.stopPropagation()}
					>
						<HolderOutlined />
					</span>
				)}
				<div className="matrix-state-cell__label-wrap">
					<StatusLabel state={state} />
				</div>
				{showActions && (
					<div className="matrix-state-cell__actions">
						<Dropdown
							menu={{ items: menuItems }}
							trigger={["click"]}
							placement="bottomRight"
						>
							<span
								className="matrix-state-cell__action-trigger"
								onClick={(e) => e.stopPropagation()}
								onPointerDown={(e) => e.stopPropagation()}
							>
								<MoreOutlined />
							</span>
						</Dropdown>
					</div>
				)}
			</div>
		</div>
	);
}

interface MatrixStepCellProps {
	rule?: TransitionRule;
	canUpdate: boolean;
	onAdd: () => void;
	onOpenStepSettings: (rule: TransitionRule) => void;
}

/** 矩阵步骤单元格：空 / 无规则 / 已配置规则；点击已有步骤打开步骤设置抽屉 */
function MatrixStepCell({
	rule,
	canUpdate,
	onAdd,
	onOpenStepSettings,
}: MatrixStepCellProps) {
	if (!rule) {
		return (
			<div
				className={`matrix-step-cell matrix-step-cell--empty ${canUpdate ? "is-editable" : ""}`}
				onClick={() => canUpdate && onAdd()}
			>
				<div className="matrix-step-cell__card matrix-step-cell__card--empty">
					<span className="matrix-step-cell__add-symbol">+</span>
					<span className="matrix-step-cell__add-text">创建流转步骤</span>
				</div>
			</div>
		);
	}

	const body = !hasConfiguredRules(rule)
		? (
			<div className="matrix-step-cell__card matrix-step-cell__card--no-rule has-flow-line">
				<span className="matrix-step-cell__name">{rule.step_name}</span>
				<span className="matrix-step-cell__hint">未设置规则</span>
			</div>
		)
		: (
			<div className="matrix-step-cell__card matrix-step-cell__card--configured has-flow-line">
				<span className="matrix-step-cell__name">{rule.step_name}</span>
			</div>
		);

	const cellClass = !hasConfiguredRules(rule)
		? "matrix-step-cell matrix-step-cell--no-rule"
		: "matrix-step-cell matrix-step-cell--configured";

	return (
		<div
			className={`${cellClass}${canUpdate ? " is-clickable" : ""}`}
			role={canUpdate ? "button" : undefined}
			tabIndex={canUpdate ? 0 : undefined}
			onClick={() => canUpdate && onOpenStepSettings(rule)}
			onKeyDown={(e) => {
				if (!canUpdate) return;
				if (e.key === "Enter" || e.key === " ") {
					e.preventDefault();
					onOpenStepSettings(rule);
				}
			}}
		>
			{body}
		</div>
	);
}

interface SortableMatrixRowProps {
	fromState: TicketStatusFlowState;
	states: TicketStatusFlowState[];
	statusFlow: TicketStatusFlow;
	canUpdate: boolean;
	sortable: boolean;
	findRule: (fromCode: string, toCode: string) => TransitionRule | undefined;
	getStateMenuItems: (stateCode: string) => MenuProps["items"];
	onAddStep: (fromCode: string, toCode: string) => void;
	onOpenStepSettings: (rule: TransitionRule) => void;
}

/** 可排序的数据行 */
function SortableMatrixRow({
	fromState,
	states,
	statusFlow,
	canUpdate,
	sortable,
	findRule,
	getStateMenuItems,
	onAddStep,
	onOpenStepSettings,
}: SortableMatrixRowProps) {
	const {
		attributes,
		listeners,
		setNodeRef,
		isDragging,
	} = useSortable({
		id: fromState.code,
		disabled: !sortable,
	});

	const rowStyle = matrixRowStyle;

	return (
		<div
			ref={setNodeRef}
			style={rowStyle}
			className={`matrix-sortable-row ${isDragging ? "is-dragging" : ""}`}
		>
			<div className="matrix-line__col matrix-line__col--row-header">
				{isDragging ? (
					<div className="matrix-state-cell matrix-state-cell--replacement-source" aria-hidden="true" />
				) : (
					<MatrixStateCell
						state={fromState}
						variant="row"
						isInitial={isInitialState(statusFlow, fromState.code)}
						canUpdate={canUpdate}
						menuItems={getStateMenuItems(fromState.code)}
						dragListeners={sortable ? listeners : undefined}
						dragAttributes={sortable ? attributes : undefined}
					/>
				)}
			</div>
			{states.map(toState => {
				const isDiagonal = fromState.code === toState.code;
				const rule = findRule(fromState.code, toState.code);

				return (
					<div
						key={toState.code}
						className={`matrix-line__col matrix-line__col--step ${isDiagonal ? "is-diagonal" : ""}`}
					>
						{isDiagonal
							? <div className="matrix-step-cell matrix-step-cell--diagonal" />
							: (
								<MatrixStepCell
									rule={rule}
									canUpdate={canUpdate}
									onAdd={() => onAddStep(fromState.code, toState.code)}
									onOpenStepSettings={onOpenStepSettings}
								/>
							)}
					</div>
				);
			})}
		</div>
	);
}

/**
 * 工作流矩阵表格视图
 * 对齐 CODING 事项类型工作流配置的矩阵布局
 */
export function WorkflowMatrixView({
	statusFlow,
	rules,
	canUpdate,
	onAddStep,
	onOpenStepSettings,
	onReorderStates,
	onSetInitialState,
	onRemoveState,
}: WorkflowMatrixViewProps) {
	const { states, transitions } = statusFlow;
	const [activeId, setActiveId] = useState<string | null>(null);

	const dragOriginStatesRef = useRef<TicketStatusFlowState[] | null>(null);
	const lastOverIdRef = useRef<string | null>(null);
	const lastActiveIndexRef = useRef<number | null>(null);

	const sensors = useSensors(
		useSensor(PointerSensor, {
			activationConstraint: {
				distance: 8,
			},
		}),
		useSensor(KeyboardSensor, {
			coordinateGetter: sortableKeyboardCoordinates,
		}),
	);

	const findRule = useCallback((fromCode: string, toCode: string) => {
		return rules.find(r => r.from_state_code === fromCode && r.to_state_code === toCode);
	}, [rules]);

	const stateIds = useMemo(() => states.map(s => s.code), [states]);

	const activeState = useMemo(
		() => (activeId ? states.find(s => s.code === activeId) : undefined),
		[activeId, states],
	);

	const resetDragState = useCallback(() => {
		setActiveId(null);
		dragOriginStatesRef.current = null;
		lastOverIdRef.current = null;
		lastActiveIndexRef.current = null;
	}, []);

	const handleDragStart = useCallback((event: DragStartEvent) => {
		const id = String(event.active.id);
		dragOriginStatesRef.current = states;
		lastOverIdRef.current = id;
		setActiveId(id);
	}, [states]);

	const handleDragOver = useCallback((event: DragOverEvent) => {
		const { active, over } = event;
		if (!over || !onReorderStates) return;

		const activeCode = String(active.id);
		const overCode = String(over.id);
		if (activeCode === overCode) return;

		const oldIndex = states.findIndex(s => s.code === activeCode);
		const newIndex = states.findIndex(s => s.code === overCode);
		if (oldIndex === -1 || newIndex === -1 || oldIndex === newIndex) return;

		// 替换后 active 索引变化，允许再次悬停同一目标触发回退
		if (lastActiveIndexRef.current !== null && lastActiveIndexRef.current !== oldIndex) {
			lastOverIdRef.current = null;
		}
		lastActiveIndexRef.current = oldIndex;

		if (overCode === lastOverIdRef.current) return;
		lastOverIdRef.current = overCode;

		onReorderStates(arrayMove(states, oldIndex, newIndex));
	}, [onReorderStates, states]);

	const handleDragEnd = useCallback((_event: DragEndEvent) => {
		resetDragState();
	}, [resetDragState]);

	const handleDragCancel = useCallback(() => {
		if (dragOriginStatesRef.current && onReorderStates) {
			onReorderStates(dragOriginStatesRef.current);
		}
		resetDragState();
	}, [onReorderStates, resetDragState]);

	const getStateMenuItems = useCallback((stateCode: string): MenuProps["items"] => {
		const initial = isInitialState(statusFlow, stateCode);
		return [
			{
				key: "set-initial",
				label: initial ? "已是初始状态" : "设置为初始状态",
				icon: initial ? <HomeFilled /> : <HomeOutlined />,
				disabled: initial,
				onClick: () => !initial && onSetInitialState?.(stateCode),
			},
			{
				type: "divider",
			},
			{
				key: "remove",
				label: "移除",
				icon: <DeleteOutlined />,
				danger: true,
				onClick: () => onRemoveState?.(stateCode),
			},
		];
	}, [statusFlow, onSetInitialState, onRemoveState]);

	const rowSortable = canUpdate && !!onReorderStates;

	if (states.length === 0) {
		return (
			<div className="workflow-matrix-empty">
				<Empty description="暂无状态，请先添加状态" />
			</div>
		);
	}

	return (
		<div className="workflow-matrix-container">
			<DndContext
				sensors={sensors}
				collisionDetection={closestCenter}
				onDragStart={handleDragStart}
				onDragOver={handleDragOver}
				onDragEnd={handleDragEnd}
				onDragCancel={handleDragCancel}
			>
				<div className="workflow-matrix-scroll">
					<div
						className="workflow-matrix-table"
						style={{
							"--matrix-grid-columns": `${ROW_HEADER_WIDTH}px repeat(${states.length}, ${COL_WIDTH}px)`,
						} as React.CSSProperties}
					>
						{/* 表头行 */}
						<div className="matrix-header-row" style={matrixRowStyle}>
							<div className="matrix-line__corner">
								<span className="matrix-line__corner-label matrix-line__corner-label--end">结束状态</span>
								<span className="matrix-line__corner-label matrix-line__corner-label--start">开始状态</span>
							</div>
							{states.map(toState => (
								<div key={`header-${toState.code}`} className="matrix-line__col matrix-line__col--header-state">
									<MatrixStateCell
										state={toState}
										variant="header"
										isInitial={false}
										canUpdate={false}
										menuItems={[]}
									/>
								</div>
							))}
						</div>

						{/* 可排序数据行 */}
						<SortableContext items={stateIds} strategy={verticalListSortingStrategy}>
							{states.map(fromState => (
								<SortableMatrixRow
									key={fromState.code}
									fromState={fromState}
									states={states}
									statusFlow={statusFlow}
									canUpdate={canUpdate}
									sortable={rowSortable}
									findRule={findRule}
									getStateMenuItems={getStateMenuItems}
									onAddStep={onAddStep}
									onOpenStepSettings={onOpenStepSettings}
								/>
							))}
						</SortableContext>

						{/* "任何状态" 行 */}
						<div className="matrix-any-row" style={matrixRowStyle}>
							<div className="matrix-line__col matrix-line__col--any-label">
								<span className="matrix-line__any-label">任何状态</span>
							</div>
							{states.map(toState => (
								<div key={toState.code} className="matrix-line__col matrix-line__col--step">
									<MatrixStepCell
										rule={findRule("*", toState.code)}
										canUpdate={canUpdate}
										onAdd={() => onAddStep("*", toState.code)}
										onOpenStepSettings={onOpenStepSettings}
									/>
								</div>
							))}
						</div>
					</div>
				</div>

				<DragOverlay dropAnimation={{ duration: 200, easing: "ease" }}>
					{activeState ? (
						<MatrixStateCell
							state={activeState}
							variant="row"
							isInitial={isInitialState(statusFlow, activeState.code)}
							canUpdate={false}
							menuItems={[]}
							isOverlay
						/>
					) : null}
				</DragOverlay>
			</DndContext>
		</div>
	);
}
