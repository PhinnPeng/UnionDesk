declare module "react-icons-selector" {
	import type { FC } from "react";

	export interface IconData {
		name: string;
		library: string;
	}

	export interface LanguageConfig {
		homeText?: string;
		headerText?: string;
		noIconsFoundText?: string;
		homeSearchText?: string;
		buttonText?: string;
	}

	export interface ReactIconsSelectorProps {
		value?: IconData | null;
		onChange: (icon: IconData) => void;
		icons?: string[];
		language?: LanguageConfig;
		buttonStyle?: React.CSSProperties;
		buttonClassName?: string;
		iconSize?: number;
		onSvgExport?: (svg: string, icon: IconData) => void;
	}

	const ReactIconsSelector: FC<ReactIconsSelectorProps>;
	export default ReactIconsSelector;
}
