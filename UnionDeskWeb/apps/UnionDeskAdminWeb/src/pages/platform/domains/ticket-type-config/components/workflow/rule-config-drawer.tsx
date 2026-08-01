import type {
	AdditionalAttributeItem,
	AttributeUpdateItem,
	TicketAttribute,
	TicketAttributeSlot,
	TicketStatusFlowState,
	TransitionRule,
} from "@uniondesk/shared";

import {
	Button,
	Dropdown,
	Drawer,
	Input,
	Space,
} from "antd";
import {
	DeleteOutlined,
	DownOutlined,
	PlusOutlined,
	RightOutlined,
} from "@ant-design/icons";
import { useEffect, useState, type ReactNode } from "react";

import {
	hasAdditionalAttributeRule,
	normalizeAdditionalAttributes,
	summarizeAdditionalAttributes,
	syncRequiredSlotIds,
} from "./additional-attribute-utils";
import { StatusLabelByCode } from "./status-label";
import {
	StepRuleAttributeUpdateEditor,
	hasAttributeUpdateRule,
	summarizeAttributeUpdates,
} from "./step-rule-attribute-update-editor";
import { StepRuleAdditionalAttributeListEditor } from "./step-rule-additional-attribute-list-editor";
import {
	StepRulePermissionEditor,
	hasPermissionRule,
	summarizePermission,
	type PermissionDraft,
} from "./step-rule-permission-editor";
import {
	buildRuleKindMenuItems,
	hasAddableRuleKind,
	type WorkflowRuleKind,
} from "./workflow-rule-kinds";
import "./rule-config-drawer.less";
import "./typed-rule-config-modal.less";

type RuleKind = Exclude<WorkflowRuleKind, "assignee">;

interface RuleConfigDrawerProps {
	open: boolean;
	rule: TransitionRule | null;
	states: TicketStatusFlowState[];
	slots: Array<TicketAttributeSlot & { dragId: string }>;
	availableAttributes: TicketAttribute[];
	isPlatformLevel: boolean;
	domainId?: string;
	onClose: () => void;
	onUpdate: (fromCode: string, toCode: string, patch: Partial<TransitionRule>) => void;
	onDelete: (rule: TransitionRule) => void;
}

type DraftState = {
	step_name: string;
	permission_mode: TransitionRule["permission_mode"];
	member_ids: number[];
	role_ids: number[];
	additional_attributes: AdditionalAttributeItem[];
	attribute_updates: AttributeUpdateItem[];
	enabledRules: RuleKind[];
	expandedRules: RuleKind[];
};

function deriveEnabledRules(rule: TransitionRule, isPlatformLevel: boolean): RuleKind[] {
	const kinds: RuleKind[] = [];
	if (!isPlatformLevel && hasPermissionRule(rule)) {
		kinds.push("permission");
	}
	if (hasAdditionalAttributeRule(rule)) {
		kinds.push("required");
	}
	if (hasAttributeUpdateRule(rule.attribute_updates)) {
		kinds.push("attribute_update");
	}
	return kinds;
}

function createDraft(rule: TransitionRule, isPlatformLevel: boolean): DraftState {
	return {
		step_name: rule.step_name,
		permission_mode: rule.permission_mode,
		member_ids: [...(rule.member_ids ?? [])],
		role_ids: [...(rule.role_ids ?? [])],
		additional_attributes: normalizeAdditionalAttributes(rule),
		attribute_updates: [...(rule.attribute_updates ?? [])],
		enabledRules: deriveEnabledRules(rule, isPlatformLevel),
		expandedRules: [],
	};
}

export function RuleConfigDrawer({
	open,
	rule,
	states,
	slots,
	availableAttributes,
	isPlatformLevel,
	domainId,
	onClose,
	onUpdate,
	onDelete,
}: RuleConfigDrawerProps) {
	const [draft, setDraft] = useState<DraftState | null>(null);
	const [nameError, setNameError] = useState<string | null>(null);

	useEffect(() => {
		if (open && rule) {
			setDraft(createDraft(rule, isPlatformLevel));
			setNameError(null);
		}
		if (!open) {
			setDraft(null);
			setNameError(null);
		}
	}, [open, rule, isPlatformLevel]);

	if (!rule || !draft) return null;

	const patchDraft = (patch: Partial<DraftState>) => {
		setDraft(prev => (prev ? { ...prev, ...patch } : prev));
	};

	const toggleExpanded = (kind: RuleKind) => {
		setDraft((prev) => {
			if (!prev) return prev;
			const expanded = prev.expandedRules.includes(kind)
				? prev.expandedRules.filter(k => k !== kind)
				: [...prev.expandedRules, kind];
			return { ...prev, expandedRules: expanded };
		});
	};

	const addRule = (kind: RuleKind) => {
		if (kind === "permission" && isPlatformLevel) return;
		setDraft((prev) => {
			if (!prev || prev.enabledRules.includes(kind)) return prev;
			const next: DraftState = {
				...prev,
				enabledRules: [...prev.enabledRules, kind],
				expandedRules: prev.expandedRules.includes(kind)
					? prev.expandedRules
					: [...prev.expandedRules, kind],
			};
			if (kind === "permission" && next.permission_mode === "none") {
				next.permission_mode = "members";
			}
			return next;
		});
	};

	const removeRule = (kind: RuleKind) => {
		setDraft((prev) => {
			if (!prev) return prev;
			const next: DraftState = {
				...prev,
				enabledRules: prev.enabledRules.filter(k => k !== kind),
				expandedRules: prev.expandedRules.filter(k => k !== kind),
			};
			if (kind === "permission") {
				next.permission_mode = "none";
				next.member_ids = [];
				next.role_ids = [];
			}
			if (kind === "required") {
				next.additional_attributes = [];
			}
			if (kind === "attribute_update") {
				next.attribute_updates = [];
			}
			return next;
		});
	};

	const addMenuItems = buildRuleKindMenuItems(
		isPlatformLevel,
		{
			onSelectKind: (kind) => {
				if (kind === "assignee") return;
				addRule(kind);
			},
		},
		{ alreadyAddedKinds: draft.enabledRules },
	);

	const handleSave = () => {
		const trimmed = draft.step_name.trim();
		if (!trimmed) {
			setNameError("请输入步骤名称");
			return;
		}
		setNameError(null);

		const permissionEnabled = draft.enabledRules.includes("permission") && !isPlatformLevel;
		const requiredEnabled = draft.enabledRules.includes("required");
		const updateEnabled = draft.enabledRules.includes("attribute_update");

		onUpdate(rule.from_state_code, rule.to_state_code, {
			step_name: trimmed,
			permission_mode: permissionEnabled ? draft.permission_mode : "none",
			member_ids: permissionEnabled ? draft.member_ids : [],
			role_ids: permissionEnabled ? draft.role_ids : [],
			additional_attributes: requiredEnabled ? draft.additional_attributes : [],
			required_slot_ids: requiredEnabled
				? syncRequiredSlotIds(draft.additional_attributes)
				: [],
			attribute_updates: updateEnabled ? draft.attribute_updates : [],
		});
		onClose();
	};

	const handleDelete = () => {
		onDelete(rule);
	};

	const permissionValue: PermissionDraft = {
		permission_mode: draft.permission_mode,
		member_ids: draft.member_ids,
		role_ids: draft.role_ids,
	};

	const renderFromState = () => {
		if (rule.from_state_code === "*") {
			return <span className="step-settings__any-state">任何状态</span>;
		}
		return <StatusLabelByCode code={rule.from_state_code} states={states} size="small" />;
	};

	const ruleCards: Array<{
		kind: RuleKind;
		title: string;
		summary: string;
		body: ReactNode;
	}> = [];

	if (draft.enabledRules.includes("permission")) {
		ruleCards.push({
			kind: "permission",
			title: "限制步骤权限",
			summary: summarizePermission(permissionValue),
			body: (
				<StepRulePermissionEditor
					value={permissionValue}
					disabled={isPlatformLevel}
					onChange={patch => patchDraft(patch)}
				/>
			),
		});
	}
	if (draft.enabledRules.includes("required")) {
		ruleCards.push({
			kind: "required",
			title: "附加属性",
			summary: summarizeAdditionalAttributes(draft.additional_attributes.length),
			body: (
				<StepRuleAdditionalAttributeListEditor
					value={draft.additional_attributes}
					slots={slots}
					availableAttributes={availableAttributes}
					onChange={additional_attributes => patchDraft({ additional_attributes })}
				/>
			),
		});
	}
	if (draft.enabledRules.includes("attribute_update")) {
		ruleCards.push({
			kind: "attribute_update",
			title: "更改属性值",
			summary: summarizeAttributeUpdates(draft.attribute_updates.length),
			body: (
				<StepRuleAttributeUpdateEditor
					value={draft.attribute_updates}
					slots={slots}
					availableAttributes={availableAttributes}
					domainId={domainId}
					onChange={attribute_updates => patchDraft({ attribute_updates })}
				/>
			),
		});
	}

	const canAddMore = hasAddableRuleKind(isPlatformLevel, draft.enabledRules);

	return (
		<Drawer
			className="step-settings-drawer"
			title="步骤设置"
			open={open}
			onClose={onClose}
			width={380}
			destroyOnHidden
			footer={(
				<>
					<Button
						type="link"
						danger
						icon={<DeleteOutlined />}
						className="step-settings__delete"
						onClick={handleDelete}
					>
						删除该步骤
					</Button>
					<Button type="primary" onClick={handleSave}>
						保存
					</Button>
				</>
			)}
		>
			<div className="step-settings">
				<p className="step-settings__intro">
					通过步骤可将事项从一个状态转为另一个状态，它表示在工作流中解决问题而采取的行动
				</p>

				<section>
					<label className="step-settings__section-label" htmlFor="step-settings-name">
						步骤名称
					</label>
					<Input
						id="step-settings-name"
						value={draft.step_name}
						status={nameError ? "error" : undefined}
						placeholder="请输入步骤名称"
						onChange={(e) => {
							setNameError(null);
							patchDraft({ step_name: e.target.value });
						}}
					/>
					{nameError && (
						<div style={{ marginTop: 4, color: "var(--ant-color-error)", fontSize: 12 }}>{nameError}</div>
					)}
				</section>

				<section>
					<span className="step-settings__section-label">状态转化</span>
					<div className="step-settings__transition">
						{renderFromState()}
						<span className="step-settings__transition-arrow">→</span>
						<StatusLabelByCode code={rule.to_state_code} states={states} size="small" />
					</div>
				</section>

				<section>
					<div className="step-settings__rules-header">
						<div>
							<h4 className="step-settings__rules-title">规则</h4>
							<p className="step-settings__rules-desc">
								执行状态转换前检测限制条件，转换后自动执行多个自定义操作
							</p>
						</div>
						<Dropdown
							overlayClassName="rule-kind-dropdown"
							menu={{ items: addMenuItems }}
							trigger={["click"]}
							disabled={!canAddMore}
						>
							<Button
								type="text"
								size="small"
								icon={<PlusOutlined />}
								disabled={!canAddMore}
								aria-label="添加规则"
							/>
						</Dropdown>
					</div>

					{ruleCards.length === 0 ? (
						<div className="step-settings__rules-empty">
							尚未添加规则，点击右上方 + 号添加规则
						</div>
					) : (
						<div className="step-settings__rule-list">
							{ruleCards.map((card) => {
								const expanded = draft.expandedRules.includes(card.kind);
								return (
									<div key={card.kind} className="step-settings__rule-card">
										<div
											className="step-settings__rule-card-header"
											onClick={() => toggleExpanded(card.kind)}
											onKeyDown={(e) => {
												if (e.key === "Enter" || e.key === " ") {
													e.preventDefault();
													toggleExpanded(card.kind);
												}
											}}
											role="button"
											tabIndex={0}
										>
											<div>
												<p className="step-settings__rule-card-title">{card.title}</p>
												<p className="step-settings__rule-card-summary">{card.summary}</p>
											</div>
											<Space className="step-settings__rule-card-actions" size={0}>
												<Button
													type="text"
													size="small"
													danger
													icon={<DeleteOutlined />}
													aria-label={`移除${card.title}`}
													onClick={(e) => {
														e.stopPropagation();
														removeRule(card.kind);
													}}
												/>
												{expanded ? <DownOutlined /> : <RightOutlined />}
											</Space>
										</div>
										{expanded && (
											<div className="step-settings__rule-card-body">
												{card.body}
											</div>
										)}
									</div>
								);
							})}
						</div>
					)}
				</section>
			</div>
		</Drawer>
	);
}
