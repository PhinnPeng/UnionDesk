import "@designable/react/dist/theme.less";
import "./formily-form-designer.less";

import { createDesigner, KeyCode, Shortcut } from "@designable/core";
import { transformToSchema, transformToTreeNode } from "@designable/formily-transformer";
import {
	ComponentTreeWidget,
	CompositePanel,
	Designer,
	DesignerToolsWidget,
	HistoryWidget,
	OutlineTreeWidget,
	ResourceWidget,
	SettingsPanel,
	StudioPanel,
	ToolbarPanel,
	ViewPanel,
	ViewportPanel,
	ViewToolsWidget,
	Workspace,
	WorkspacePanel,
	useDesigner,
} from "@designable/react";
import { SettingsForm, setNpmCDNRegistry } from "@designable/react-settings-form";
import { autorun } from "@formily/reactive";

import { Alert, Spin } from "antd";
import { useEffect, useMemo, useRef } from "react";

import {
	designableArraySources,
	designableComponents,
	designableDisplaySources,
	designableInputSources,
	designableLayoutSources,
} from "./designable-sources";
import { mergeSystemFormSchema } from "./form-schema-utils";
import { saveSchema, runSchemaAction } from "./service";
import {
	ActionsWidget,
	LogoWidget,
	MarkupSchemaWidget,
	PreviewWidget,
	SchemaEditorWidget,
} from "./widgets";

setNpmCDNRegistry("//unpkg.com");

export { DEFAULT_TICKET_FORM_SCHEMA, mergeSystemFormSchema } from "./form-schema-utils";

export interface FormilyFormDesignerProps {
	value: Record<string, unknown> | null | undefined;
	onChange: (schema: Record<string, unknown>) => void;
	onSaveDraft?: (schema: Record<string, unknown>) => void | Promise<void>;
	onPublish?: (schema: Record<string, unknown>) => void | Promise<void>;
	saving?: boolean;
	publishing?: boolean;
	disabled?: boolean;
	hint?: string;
	workspaceId?: string;
}

function DesignableSchemaLoader({ schema }: { schema: Record<string, unknown> }) {
	const engine = useDesigner();
	const initialSchemaRef = useRef(schema);

	useEffect(() => {
		const mergedSchema = mergeSystemFormSchema(initialSchemaRef.current);
		const tree = transformToTreeNode({ schema: mergedSchema });
		const frameId = requestAnimationFrame(() => {
			engine.setCurrentTree(tree);
		});
		return () => cancelAnimationFrame(frameId);
	}, [engine]);

	return null;
}

function DesignableSchemaSync({ onChange }: { onChange: (schema: Record<string, unknown>) => void }) {
	const engine = useDesigner();
	const onChangeRef = useRef(onChange);
	const lastSchemaRef = useRef("");
	onChangeRef.current = onChange;

	useEffect(() => {
		const workspace = engine.workbench.currentWorkspace;
		if (!workspace) {
			return;
		}
		return autorun(() => {
			void workspace.history.current;
			const tree = engine.getCurrentTree();
			if (!tree) {
				return;
			}
			const { schema } = transformToSchema(tree);
			if (!schema) {
				return;
			}
			const mergedSchema = mergeSystemFormSchema(schema as Record<string, unknown>);
			const serialized = JSON.stringify(mergedSchema);
			if (serialized === lastSchemaRef.current) {
				return;
			}
			lastSchemaRef.current = serialized;
			queueMicrotask(() => {
				onChangeRef.current(mergedSchema);
			});
		});
	}, [engine]);

	return null;
}

function DesignableDisabledGuard({ disabled }: { disabled?: boolean }) {
	const engine = useDesigner();

	useEffect(() => {
		const workspace = engine.workbench.currentWorkspace;
		if (!workspace) {
			return;
		}
		workspace.history.locking = Boolean(disabled);
	}, [disabled, engine]);

	return null;
}

export function FormilyFormDesigner({
	value,
	onChange,
	onSaveDraft,
	onPublish,
	saving,
	publishing,
	disabled,
	hint,
	workspaceId = "form",
}: FormilyFormDesignerProps) {
	const mergedSchema = useMemo(() => mergeSystemFormSchema(value), [value]);
	const onChangeRef = useRef(onChange);
	const onSaveDraftRef = useRef(onSaveDraft);
	const onPublishRef = useRef(onPublish);
	onChangeRef.current = onChange;
	onSaveDraftRef.current = onSaveDraft;
	onPublishRef.current = onPublish;

	const engine = useMemo(
		() =>
			createDesigner({
				shortcuts: [
					new Shortcut({
						codes: [
							[KeyCode.Meta, KeyCode.S],
							[KeyCode.Control, KeyCode.S],
						],
						handler(ctx) {
							if (onSaveDraftRef.current) {
								void runSchemaAction(ctx.engine, onSaveDraftRef.current);
								return;
							}
							saveSchema(ctx.engine, schema => onChangeRef.current(schema));
						},
					}),
				],
				rootComponentName: "Form",
			}),
		[],
	);

	const handleSave = (schema: Record<string, unknown>) => {
		onChangeRef.current(schema);
	};

	return (
		<div className="formily-form-designer">
			{hint ? (
				<Alert className="formily-form-designer__hint" type="info" showIcon message={hint} />
			) : null}
			<div className="formily-form-designer__canvas">
				<Designer engine={engine} position="relative" theme="light">
					<StudioPanel
						logo={<LogoWidget />}
						actions={(
							<ActionsWidget
								onSave={handleSave}
								onSaveDraft={onSaveDraft}
								onPublish={onPublish}
								saving={saving}
								publishing={publishing}
							/>
						)}
						position="relative"
						style={{ height: "100%" }}
					>
						<CompositePanel>
							<CompositePanel.Item title="panels.Component" icon="Component">
								<ResourceWidget title="sources.Inputs" sources={designableInputSources} />
								<ResourceWidget title="sources.Layouts" sources={designableLayoutSources} />
								<ResourceWidget title="sources.Arrays" sources={designableArraySources} />
								<ResourceWidget title="sources.Displays" sources={designableDisplaySources} />
							</CompositePanel.Item>
							<CompositePanel.Item title="panels.OutlinedTree" icon="Outline">
								<OutlineTreeWidget />
							</CompositePanel.Item>
							<CompositePanel.Item title="panels.History" icon="History">
								<HistoryWidget />
							</CompositePanel.Item>
						</CompositePanel>
						<Workspace id={workspaceId}>
							<WorkspacePanel>
								<ToolbarPanel>
									<DesignerToolsWidget use={["HISTORY", "CURSOR", "SCREEN_TYPE"]} />
									<ViewToolsWidget use={["DESIGNABLE", "JSONTREE", "MARKUP", "PREVIEW"]} />
								</ToolbarPanel>
								<ViewportPanel>
									<ViewPanel type="DESIGNABLE">
										{() => (
											<ComponentTreeWidget components={designableComponents} />
										)}
									</ViewPanel>
									<ViewPanel type="JSONTREE" scrollable={false}>
										{(tree, treeOnChange) => (
											<SchemaEditorWidget tree={tree} onChange={treeOnChange} />
										)}
									</ViewPanel>
									<ViewPanel type="MARKUP" scrollable={false}>
										{tree => <MarkupSchemaWidget tree={tree} />}
									</ViewPanel>
									<ViewPanel type="PREVIEW">
										{tree => <PreviewWidget tree={tree} />}
									</ViewPanel>
								</ViewportPanel>
							</WorkspacePanel>
							<DesignableSchemaLoader schema={mergedSchema} />
							<DesignableSchemaSync onChange={onChange} />
							<DesignableDisabledGuard disabled={disabled} />
						</Workspace>
						<SettingsPanel title="panels.PropertySettings">
							<SettingsForm uploadAction="https://www.mocky.io/v2/5cc8019d300000980a055e76" />
						</SettingsPanel>
					</StudioPanel>
				</Designer>
			</div>
		</div>
	);
}

export function FormilyFormDesignerFallback() {
	return (
		<div className="flex h-40 items-center justify-center">
			<Spin description="加载表单设计器..." />
		</div>
	);
}

/** @deprecated 使用 FormilyFormDesigner */
export const TicketTypeFormDesigner = FormilyFormDesigner;

/** @deprecated 使用 FormilyFormDesignerFallback */
export const TicketTypeFormDesignerFallback = FormilyFormDesignerFallback;
