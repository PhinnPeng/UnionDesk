import "@blocknote/core/fonts/inter.css";
import "@blocknote/mantine/style.css";
import "./description-template-editor.less";

import { zh } from "@blocknote/core/locales";
import { useCreateBlockNote } from "@blocknote/react";
import { BlockNoteView } from "@blocknote/mantine";
import { usePreferences } from "#src/hooks/use-preferences";
import { useEffect, useRef } from "react";

interface DescriptionTemplateEditorProps {
	/** Markdown loaded once on mount; do not pass a live draft that onChange updates. */
	initialMarkdown: string;
	onChange: (markdown: string) => void;
}

export function DescriptionTemplateEditor({
	initialMarkdown,
	onChange,
}: DescriptionTemplateEditorProps) {
	const { isDark } = usePreferences();
	const editor = useCreateBlockNote({
		dictionary: zh,
	});
	const loadedRef = useRef(false);
	const onChangeRef = useRef(onChange);
	onChangeRef.current = onChange;
	const initialMarkdownRef = useRef(initialMarkdown);

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
		}
		void loadMarkdown();
		return () => {
			cancelled = true;
		};
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
					const markdown = editor.blocksToMarkdownLossy(editor.document);
					onChangeRef.current(markdown);
				}}
			/>
		</div>
	);
}
