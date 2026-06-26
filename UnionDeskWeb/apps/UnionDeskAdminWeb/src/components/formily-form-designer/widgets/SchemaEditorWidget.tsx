import type { ITreeNode, TreeNode } from "@designable/core";
import { transformToSchema, transformToTreeNode } from "@designable/formily-transformer";
import { MonacoInput } from "@designable/react-settings-form";

export interface SchemaEditorWidgetProps {
	tree: TreeNode
	onChange?: (tree: ITreeNode) => void
}

export function SchemaEditorWidget({ tree, onChange }: SchemaEditorWidgetProps) {
	return (
		<MonacoInput
			value={JSON.stringify(transformToSchema(tree), null, 2)}
			onChange={(value) => {
				onChange?.(transformToTreeNode(JSON.parse(value)));
			}}
			language="json"
		/>
	);
}
