import type { MenuProps } from "antd";

/** 可配置的规则能力类型（与扁平 TransitionRule 字段对应） */
export type WorkflowRuleKind =
	| "permission"
	| "required"
	| "attribute_update"
	| "assignee";

/** 下拉固定顺序，工具栏与步骤抽屉「+」共用 */
export const WORKFLOW_RULE_KIND_ORDER: WorkflowRuleKind[] = [
	"permission",
	"required",
	"assignee",
	"attribute_update",
];

export const WORKFLOW_RULE_KIND_META: Record<
	WorkflowRuleKind,
	{ label: string; description: string }
> = {
	permission: {
		label: "限制步骤权限",
		description: "状态变更前，检查当前成员权限，只允许特定成员执行步骤",
	},
	required: {
		label: "附加属性",
		description: "状态变更前，需要额外录入属性的值，提交后才可继续执行步骤",
	},
	assignee: {
		label: "更改处理人",
		description: "状态变更后，自动修改事项的处理人",
	},
	attribute_update: {
		label: "更改属性值",
		description: "状态变更后，自动修改属性的值",
	},
};

export type RuleKindMenuHandlers = {
	onSelectKind: (kind: WorkflowRuleKind) => void;
};

export type BuildRuleKindMenuOptions = {
	/** 步骤抽屉内已添加的类型，对应项置灰并提示「该规则已添加」 */
	alreadyAddedKinds?: ReadonlyArray<Exclude<WorkflowRuleKind, "assignee"> | string>;
};

function resolveDisabled(
	kind: WorkflowRuleKind,
	isPlatformLevel: boolean,
	alreadyAdded: ReadonlySet<string>,
): { disabled: boolean; reason?: string } {
	if (kind === "permission" && isPlatformLevel) {
		return { disabled: true, reason: "平台级类型不支持配置步骤权限" };
	}
	if (kind === "assignee") {
		if (isPlatformLevel) {
			return { disabled: true, reason: "平台级类型不支持更改处理人" };
		}
		if (alreadyAdded.has("attribute_update") || alreadyAdded.has("assignee")) {
			return { disabled: true, reason: "该规则已添加" };
		}
		return { disabled: false };
	}
	if (alreadyAdded.has(kind)) {
		return { disabled: true, reason: "该规则已添加" };
	}
	return { disabled: false };
}

/**
 * 构建规则类型下拉（工具栏「配置规则」与步骤设置「+」共用）。
 * 固定四项，顺序一致。
 */
export function buildRuleKindMenuItems(
	isPlatformLevel: boolean,
	handlers: RuleKindMenuHandlers,
	options?: BuildRuleKindMenuOptions,
): MenuProps["items"] {
	const alreadyAdded = new Set(options?.alreadyAddedKinds ?? []);

	const items: NonNullable<MenuProps["items"]> = WORKFLOW_RULE_KIND_ORDER.map((kind) => {
		const meta = WORKFLOW_RULE_KIND_META[kind];
		const { disabled, reason } = resolveDisabled(kind, isPlatformLevel, alreadyAdded);

		return {
			key: kind,
			disabled,
			title: reason,
			label: (
				<div className="rule-kind-menu-item">
					<div className="rule-kind-menu-item__title">{meta.label}</div>
					<div className="rule-kind-menu-item__desc">{meta.description}</div>
				</div>
			),
			onClick: () => {
				if (!disabled) handlers.onSelectKind(kind);
			},
		};
	});

	return items;
}

/** 是否还有可新加的规则类型（用于控制「+」是否可点） */
export function hasAddableRuleKind(
	isPlatformLevel: boolean,
	alreadyAddedKinds: ReadonlyArray<string>,
): boolean {
	const alreadyAdded = new Set(alreadyAddedKinds);
	return WORKFLOW_RULE_KIND_ORDER.some((kind) => {
		const { disabled } = resolveDisabled(kind, isPlatformLevel, alreadyAdded);
		return !disabled;
	});
}
