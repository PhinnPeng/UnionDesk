import { createUseStyles } from "react-jss";

export const useStyles = createUseStyles(({ prefixCls, isDark }) => {
	return {
		basicTable: {
			[`& .menu-tree-table .ant-table-cell-with-append`]: {
				display: "flex",
				"align-items": "center",
				gap: "4px",
			},
			[`& .${prefixCls}-table`]: {
				[`& .${prefixCls}-table-thead > tr > th, & .${prefixCls}-table-thead > tr > td`]: {
					"text-align": "center",
				},
				[`& .${prefixCls}-table-container`]: {
					[`& .${prefixCls}-table-content, & .${prefixCls}-table-body`]: {
						"scrollbar-width": "thin",
						"scrollbar-color": isDark ? "#909399 transparent" : "#eaeaea transparent",
						"scrollbar-gutter": "stable",
					},
				},
			},
		},
	};
});
