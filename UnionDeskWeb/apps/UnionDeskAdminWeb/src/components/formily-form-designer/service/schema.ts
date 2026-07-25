import type { Engine } from "@designable/core";
import { transformToSchema } from "@designable/formily-transformer";
import { message } from "antd";

import { mergeSystemFormSchema, validateFormSchemaForSave } from "../form-schema-utils";

export function extractCurrentSchema(designer: Engine): Record<string, unknown> | null {
	const { schema } = transformToSchema(designer.getCurrentTree());
	if (!schema) {
		message.warning("无法提取表单 schema，请检查设计器内容");
		return null;
	}
	return mergeSystemFormSchema(schema as Record<string, unknown>);
}

export function prepareSchemaForSave(designer: Engine): Record<string, unknown> | null {
	const mergedSchema = extractCurrentSchema(designer);
	if (!mergedSchema) {
		return null;
	}
	const validationError = validateFormSchemaForSave(mergedSchema);
	if (validationError) {
		message.error(validationError);
		return null;
	}
	return mergedSchema;
}

export function saveSchema(
	designer: Engine,
	onSave?: (schema: Record<string, unknown>) => void,
) {
	const mergedSchema = prepareSchemaForSave(designer);
	if (!mergedSchema) {
		return;
	}
	onSave?.(mergedSchema);
}

export async function runSchemaAction(
	designer: Engine,
	action?: (schema: Record<string, unknown>) => void | Promise<void>,
) {
	const mergedSchema = prepareSchemaForSave(designer);
	if (!mergedSchema || !action) {
		return;
	}
	await action(mergedSchema);
}
