import "@blocknote/core/fonts/inter.css";
import "@blocknote/mantine/style.css";
import "./description-template-editor.less";

import { zh } from "@blocknote/core/locales";
import { useCreateBlockNote } from "@blocknote/react";
import { BlockNoteView } from "@blocknote/mantine";
import { usePreferences } from "#src/hooks/use-preferences";
import { forwardRef, useEffect, useImperativeHandle, useRef } from "react";

import { serializeDescriptionTemplateMarkdown } from "./description-template-markdown";

export interface DescriptionTemplateEditorHandle {
	/** 从编辑器文档即时序列化，避免仅依赖 onChange 草稿态。 */
	getMarkdown: () => string
}

interface DescriptionTemplateEditorProps {
	/** Markdown loaded once on mount; do not pass a live draft that onChange updates. */
	initialMarkdown: string
	onChange: (markdown: string) => void
}

export const DescriptionTemplateEditor = forwardRef<
	DescriptionTemplateEditorHandle,
	DescriptionTemplateEditorProps
>(function DescriptionTemplateEditor({ initialMarkdown, onChange }, ref) {
	const { isDark } = usePreferences();
	const editor = useCreateBlockNote({
		dictionary: zh,
	});
	const loadedRef = useRef(false);
	const onChangeRef = useRef(onChange);
	onChangeRef.current = onChange;
	const initialMarkdownRef = useRef(initialMarkdown);

	const emitMarkdown = () => {
		const markdown = serializeDescriptionTemplateMarkdown(
			blocks => editor.blocksToMarkdownLossy(blocks as never),
			editor.document as never[],
		);
		onChangeRef.current(markdown);
		return markdown;
	};

	useImperativeHandle(ref, () => ({
		getMarkdown: () => {
			return serializeDescriptionTemplateMarkdown(
				blocks => editor.blocksToMarkdownLossy(blocks as never),
				editor.document as never[],
			);
		},
	}), [editor]);

	useEffect(() => {
		let cancelled = false;
		async function loadMarkdown() {
			loadedRef.current = false;
			const blocks = await editor.tryParseMarkdownToBlocks(initialMarkdownRef.current || "");
			if (cancelled) {
				return;
			}
			editor.replaceBlocks(editor.document, blocks);
			loadedRef.current = true;
			emitMarkdown();
		}
		void loadMarkdown();
		return () => {
			cancelled = true;
		};
		// eslint-disable-next-line react-hooks/exhaustive-deps -- mount-once load; remount via key when seed changes
	}, [editor]);

	return (
		<div className="description-template-editor">
			<BlockNoteView
				editor={editor}
				theme={isDark ? "dark" : "light"}
				className="description-template-editor__view"
				onChange={() => {
					if (!loadedRef.current) {
						return;
					}
					emitMarkdown();
				}}
			/>
		</div>
	);
});
