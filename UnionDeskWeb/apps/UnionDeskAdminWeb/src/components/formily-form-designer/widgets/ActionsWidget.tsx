import { useDesigner } from "@designable/react";
import { observer } from "@formily/reactive-react";
import { Button, Space } from "antd";
import { useState } from "react";

import { runSchemaAction, saveSchema } from "../service";
import type { FormilyFormDesignerActionsProps } from "./types";

export const ActionsWidget = observer(({
	onSave,
	onSaveDraft,
	onPublish,
	saving = false,
	publishing = false,
}: FormilyFormDesignerActionsProps) => {
	const designer = useDesigner();
	const [localSaving, setLocalSaving] = useState(false);
	const [localPublishing, setLocalPublishing] = useState(false);

	const handleSaveDraft = async () => {
		if (onSaveDraft) {
			setLocalSaving(true);
			try {
				await runSchemaAction(designer, onSaveDraft);
			}
			finally {
				setLocalSaving(false);
			}
			return;
		}
		saveSchema(designer, onSave);
	};

	const handlePublish = async () => {
		if (!onPublish) {
			saveSchema(designer, onSave);
			return;
		}
		setLocalPublishing(true);
		try {
			await runSchemaAction(designer, onPublish);
		}
		finally {
			setLocalPublishing(false);
		}
	};

	const draftLoading = saving || localSaving;
	const publishLoading = publishing || localPublishing;

	return (
		<Space style={{ marginRight: 10 }}>
			<Button loading={draftLoading} disabled={publishLoading} onClick={() => void handleSaveDraft()}>
				保存
			</Button>
			<Button type="primary" loading={publishLoading} disabled={draftLoading} onClick={() => void handlePublish()}>
				发布
			</Button>
		</Space>
	);
});
