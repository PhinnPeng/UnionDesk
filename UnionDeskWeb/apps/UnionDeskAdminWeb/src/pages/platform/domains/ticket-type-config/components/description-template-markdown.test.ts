import { describe, expect, it } from "vitest";

import {
	preserveEmptyParagraphsForMarkdownExport,
	serializeDescriptionTemplateMarkdown,
	strengthenSoftLineBreaks,
} from "./description-template-markdown";

describe("description-template-markdown", () => {
	it("strengthens single newlines between non-empty lines into paragraph breaks", () => {
		expect(strengthenSoftLineBreaks("反馈内容：\n期望的结果：")).toBe(
			"反馈内容：\n\n期望的结果：",
		);
	});

	it("keeps existing paragraph breaks", () => {
		expect(strengthenSoftLineBreaks("反馈内容：\n\n期望的结果：\n")).toBe(
			"反馈内容：\n\n期望的结果：\n",
		);
	});

	it("does not alter markdown hard breaks", () => {
		expect(strengthenSoftLineBreaks("反馈内容：\\\n期望的结果：")).toBe(
			"反馈内容：\\\n期望的结果：",
		);
		expect(strengthenSoftLineBreaks("反馈内容：  \n期望的结果：")).toBe(
			"反馈内容：  \n期望的结果：",
		);
	});

	it("does not insert breaks between list items", () => {
		expect(strengthenSoftLineBreaks("- a\n- b")).toBe("- a\n- b");
	});

	it("fills empty paragraphs before markdown export", () => {
		const blocks = [
			{ type: "paragraph", content: [{ type: "text", text: "反馈内容：", styles: {} }], children: [] },
			{ type: "paragraph", content: [], children: [] },
			{ type: "paragraph", content: [{ type: "text", text: "期望的结果：", styles: {} }], children: [] },
		];
		const preserved = preserveEmptyParagraphsForMarkdownExport(blocks);
		expect(preserved[1]?.content).toEqual([
			{ type: "text", text: "\u00a0", styles: {} },
		]);
	});

	it("serializes with empty-paragraph preservation and soft-break strengthening", () => {
		const blocks = [
			{ type: "paragraph", content: [{ type: "text", text: "反馈内容：", styles: {} }], children: [] },
			{ type: "paragraph", content: [], children: [] },
			{ type: "paragraph", content: [{ type: "text", text: "期望的结果：", styles: {} }], children: [] },
		];
		const markdown = serializeDescriptionTemplateMarkdown(
			() => "反馈内容：\n期望的结果：\n",
			blocks,
		);
		expect(markdown).toBe("反馈内容：\n\n期望的结果：\n");
	});
});
