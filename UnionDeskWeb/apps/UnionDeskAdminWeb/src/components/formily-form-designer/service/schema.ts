import type { Engine } from "@designable/core";
import { transformToSchema } from "@designable/formily-transformer";

import { mergeSystemFormSchema } from "../form-schema-utils";

export function extractCurrentSchema(designer: Engine): Record<string, unknown> | null {
	const { schema } = transformToSchema(designer.getCurrentTree());
	if (!schema) {
		return null;
	}
	return mergeSystemFormSchema(schema as Record<string, unknown>);
}

export function saveSchema(
	designer: Engine,
	onSave?: (schema: Record<string, unknown>) => void,
) {
	const mergedSchema = extractCurrentSchema(designer);
	if (!mergedSchema) {
		return;
	}
	onSave?.(mergedSchema);
}

export async function runSchemaAction(
	designer: Engine,
	action?: (schema: Record<string, unknown>) => void | Promise<void>,
) {
	const mergedSchema = extractCurrentSchema(designer);
	if (!mergedSchema || !action) {
		return;
	}
	await action(mergedSchema);
}
