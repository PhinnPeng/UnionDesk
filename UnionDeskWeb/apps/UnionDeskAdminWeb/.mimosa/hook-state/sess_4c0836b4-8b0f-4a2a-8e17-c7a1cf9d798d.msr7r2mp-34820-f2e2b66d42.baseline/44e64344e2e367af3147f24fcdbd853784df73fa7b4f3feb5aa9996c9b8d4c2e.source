import type { QueryFilterProps } from "@ant-design/pro-components";

import { QueryFilter } from "@ant-design/pro-components";

import {
	TABLE_SEARCH_FORM_SEARCH_DEFAULTS,
	TABLE_SEARCH_FORM_SPAN,
	tableSearchFormCollapseRender,
} from "./constants";

export {
	TABLE_SEARCH_FORM_SEARCH_DEFAULTS,
	TABLE_SEARCH_FORM_SPAN,
	tableSearchFormCollapseRender,
} from "./constants";

export type TableSearchFormProps<T = Record<string, unknown>> = QueryFilterProps<T> & {
	loading?: boolean;
};

export function TableSearchForm<T = Record<string, unknown>>({
	loading,
	span,
	submitter,
	collapseRender,
	...props
}: TableSearchFormProps<T>) {
	return (
		<QueryFilter<T>
			{...TABLE_SEARCH_FORM_SEARCH_DEFAULTS}
			span={span ?? TABLE_SEARCH_FORM_SPAN}
			collapseRender={collapseRender ?? tableSearchFormCollapseRender}
			submitter={{
				submitButtonProps: { loading },
				...submitter,
			}}
			{...props}
		/>
	);
}
