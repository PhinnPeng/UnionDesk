import { AuthGuarded } from "#src/components/auth-guarded";
import {
	PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_UPDATE,
} from "#src/pages/platform/domains/platform-domain-permissions";

import { Button, Empty, Spin } from "antd";
import { lazy, Suspense, useCallback, useState } from "react";
import Markdown from "react-markdown";

const DescriptionTemplateEditor = lazy(() =>
	import("./description-template-editor").then(module => ({
		default: module.DescriptionTemplateEditor,
	})),
);

interface TicketTypeSummary {
	id: string;
	name: string;
	description_template_md?: string | null;
}

interface TemplateTabProps {
	loading: boolean;
	ticketType: TicketTypeSummary | null;
	updatePermission?: string;
	onSave: (markdown: string) => Promise<void>;
	saving: boolean;
}

export function TemplateTab({
	loading,
	ticketType,
	updatePermission = PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_UPDATE,
	onSave,
	saving,
}: TemplateTabProps) {
	const [mode, setMode] = useState<"preview" | "edit">("preview");
	const [draftMarkdown, setDraftMarkdown] = useState("");
	/** Seed for BlockNote mount only; must stay stable while editing. */
	const [editSeedMarkdown, setEditSeedMarkdown] = useState("");
	const [editSessionKey, setEditSessionKey] = useState(0);

	const templateMd = ticketType?.description_template_md?.trim() ?? "";

	const handleEnterEdit = useCallback(() => {
		const seed = ticketType?.description_template_md ?? "";
		setDraftMarkdown(seed);
		setEditSeedMarkdown(seed);
		setEditSessionKey(prev => prev + 1);
		setMode("edit");
	}, [ticketType?.description_template_md]);

	const handleCancel = useCallback(() => {
		setDraftMarkdown(ticketType?.description_template_md ?? "");
		setMode("preview");
	}, [ticketType?.description_template_md]);

	const handleSave = useCallback(async () => {
		await onSave(draftMarkdown);
		setMode("preview");
	}, [draftMarkdown, onSave]);

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

	if (mode === "edit") {
		return (
			<div className="flex h-full min-h-[420px] flex-col gap-4">
				<div className="min-h-0 flex-1 overflow-hidden rounded-lg border border-[var(--ant-color-border)] bg-[var(--ant-color-bg-container)]">
					<Suspense
						fallback={(
							<div className="flex h-full min-h-[360px] items-center justify-center">
								<Spin tip="编辑器加载中…" />
							</div>
						)}
					>
						<DescriptionTemplateEditor
							key={`${ticketType.id}-${editSessionKey}`}
							initialMarkdown={editSeedMarkdown}
							onChange={setDraftMarkdown}
						/>
					</Suspense>
				</div>
				<div className="flex shrink-0 items-center justify-end gap-2">
					<Button onClick={handleCancel} disabled={saving}>
						取消
					</Button>
					<AuthGuarded auth={updatePermission} fallback={null}>
						<Button type="primary" loading={saving} onClick={() => void handleSave()}>
							保存
						</Button>
					</AuthGuarded>
				</div>
			</div>
		);
	}

	return (
		<div className="relative flex h-full min-h-[420px] flex-col gap-4">
			<div className="min-h-0 flex-1 overflow-auto rounded-lg border border-[var(--ant-color-border)] bg-[var(--ant-color-bg-container)] p-6">
				{templateMd
					? (
						<div className="prose max-w-none dark:prose-invert">
							<Markdown>{templateMd}</Markdown>
						</div>
					)
					: (
						<div className="flex h-full min-h-[300px] items-center justify-center">
							<Empty description="暂无描述模板，点击左下角「编辑」开始编写" />
						</div>
					)}
			</div>
			<div className="flex shrink-0 items-center justify-start">
				<AuthGuarded auth={updatePermission} fallback={null}>
					<Button type="primary" onClick={handleEnterEdit}>
						编辑
					</Button>
				</AuthGuarded>
			</div>
		</div>
	);
}
