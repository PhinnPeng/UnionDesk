import { GlobalRegistry } from "@designable/core";
import * as designerIcons from "@designable/react/dist/icons";

let iconsRegistered = false;

/** 注册 Designable 内置 SVG 图标（工具栏、左侧面板、组件库） */
export function registerDesignableIcons() {
	if (iconsRegistered) {
		return;
	}
	GlobalRegistry.registerDesignerIcons(designerIcons);
	iconsRegistered = true;
}

registerDesignableIcons();
