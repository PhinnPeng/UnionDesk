/**
 * BlockNote → Markdown 辅助：减轻 lossy 导出导致的空行/换行丢失。
 *
 * BlockNote 的 blocksToMarkdownLossy 会丢掉空段落；段落内硬换行若落成单个 \\n，
 * react-markdown 默认会当成软换行并渲染成空格，预览就会变成同一行。
 */

const EMPTY_PARAGRAPH_PLACEHOLDER = "\u00a0";

type InlineContent = {
	type: string
	text?: string
	styles?: Record<string, unknown>
}

type TemplateBlock = {
	type: string
	content?: InlineContent[] | unknown
	children?: TemplateBlock[]
	[key: string]: unknown
}

function isEmptyParagraph(block: TemplateBlock): boolean {
	if (block.type !== "paragraph") {
		return false;
	}
	const content = block.content;
	if (!Array.isArray(content) || content.length === 0) {
		return true;
	}
	return content.every((item) => {
		if (item.type !== "text") {
			return false;
		}
		const text = item.text ?? "";
		return text.length === 0 || /^[\s\u00a0]*$/.test(text);
	});
}

/** 导出前：空段落写入 NBSP，避免 Markdown 往返时被吃掉。 */
export function preserveEmptyParagraphsForMarkdownExport<T extends TemplateBlock>(blocks: T[]): T[] {
	return blocks.map((block) => {
		const nextChildren = Array.isArray(block.children)
			? preserveEmptyParagraphsForMarkdownExport(block.children)
			: block.children;

		if (isEmptyParagraph(block)) {
			return {
				...block,
				content: [{ type: "text", text: EMPTY_PARAGRAPH_PLACEHOLDER, styles: {} }],
				children: nextChildren,
			};
		}

		if (nextChildren !== block.children) {
			return { ...block, children: nextChildren };
		}
		return block;
	});
}

function isPlainParagraphLine(line: string): boolean {
	const trimmed = line.trim();
	if (!trimmed) {
		return false;
	}
	// 跳过标题 / 列表 / 引用 / 代码围栏 / 表格，避免误伤结构化 Markdown
	if (/^#{1,6}\s/.test(trimmed)) {
		return false;
	}
	if (/^([-*+]|\d+\.)\s/.test(trimmed)) {
		return false;
	}
	if (/^>/.test(trimmed) || /^```/.test(trimmed) || /^\|/.test(trimmed)) {
		return false;
	}
	return true;
}

/**
 * 将「普通文本行之间的单个换行」提升为段落分隔，避免预览把软换行收成空格。
 * 保留 Markdown 硬换行（行尾 \\ 或两个空格）。
 */
export function strengthenSoftLineBreaks(markdown: string): string {
	if (!markdown) {
		return markdown;
	}
	const normalized = markdown.replace(/\r\n/g, "\n");
	const lines = normalized.split("\n");
	const out: string[] = [];

	for (let i = 0; i < lines.length; i += 1) {
		const line = lines[i] ?? "";
		out.push(line);
		if (i >= lines.length - 1) {
			continue;
		}
		const next = lines[i + 1] ?? "";
		const hardBreak = / {2}$/.test(line) || /\\$/.test(line);
		if (
			isPlainParagraphLine(line)
			&& isPlainParagraphLine(next)
			&& !hardBreak
		) {
			out.push("");
		}
	}

	return out.join("\n");
}

export function serializeDescriptionTemplateMarkdown(
	blocksToMarkdown: (blocks: TemplateBlock[]) => string,
	documentBlocks: TemplateBlock[],
): string {
	const preserved = preserveEmptyParagraphsForMarkdownExport(documentBlocks);
	const raw = blocksToMarkdown(preserved);
	return strengthenSoftLineBreaks(raw);
}
