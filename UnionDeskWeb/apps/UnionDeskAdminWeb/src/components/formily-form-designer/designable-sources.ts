import {
	ArrayCards,
	ArrayTable,
	Card,
	Cascader,
	Checkbox,
	DatePicker,
	Field,
	Form,
	FormCollapse,
	FormGrid,
	FormLayout,
	FormTab,
	Input,
	NumberPicker,
	ObjectContainer,
	Password,
	Radio,
	Rate,
	Select,
	Slider,
	Space,
	Switch,
	Text,
	TimePicker,
	Transfer,
	TreeSelect,
	Upload,
} from "@designable/formily-antd";
import { AllLocales } from "@designable/formily-antd";
import { createBehavior, GlobalRegistry } from "@designable/core";
import type { IResourceLike } from "@designable/core";
import type { IDesignerComponents } from "@designable/react";

import "./designable-icons";

const systemFieldBehavior = createBehavior({
	name: "SystemField",
	extends: ["Field"],
	selector: node => node.props?.["x-system-field"] === true,
	designerProps: {
		deletable: false,
		cloneable: false,
		draggable: false,
	},
});

GlobalRegistry.registerDesignerBehaviors({
	Form,
	Field,
	Input,
	Select,
	TreeSelect,
	Cascader,
	NumberPicker,
	Transfer,
	Password,
	Switch,
	Checkbox,
	Radio,
	DatePicker,
	TimePicker,
	Upload,
	Slider,
	Rate,
	Card,
	Space,
	FormGrid,
	FormTab,
	FormCollapse,
	FormLayout,
	ArrayCards,
	ArrayTable,
	ObjectContainer,
	Text,
	SystemField: { Behavior: systemFieldBehavior },
});

GlobalRegistry.registerDesignerLocales(AllLocales);
GlobalRegistry.registerDesignerLocales({
	"zh-CN": {
		sources: {
			Inputs: "输入控件",
			Layouts: "布局组件",
			Arrays: "自增组件",
			Displays: "展示组件",
		},
	},
	"en-US": {
		sources: {
			Inputs: "Inputs",
			Layouts: "Layouts",
			Arrays: "Arrays",
			Displays: "Displays",
		},
	},
});

/** 固定使用 Designable 简体中文，不随浏览器语言漂移 */
GlobalRegistry.setDesignerLanguage("zh-CN");

export const designableComponents: IDesignerComponents = {
	Form,
	Field,
	Input,
	Select,
	TreeSelect,
	Cascader,
	Radio,
	Checkbox,
	Slider,
	Rate,
	NumberPicker,
	Transfer,
	Password,
	DatePicker,
	TimePicker,
	Upload,
	Switch,
	Text,
	Card,
	ArrayCards,
	ArrayTable,
	Space,
	FormTab,
	FormCollapse,
	FormGrid,
	FormLayout,
	ObjectContainer,
};

export const designableInputSources: IResourceLike[] = [
	Input,
	Password,
	NumberPicker,
	Rate,
	Slider,
	Select,
	TreeSelect,
	Cascader,
	Transfer,
	Checkbox,
	Radio,
	DatePicker,
	TimePicker,
	Upload,
	Switch,
	ObjectContainer,
];

export const designableLayoutSources: IResourceLike[] = [
	Card,
	FormGrid,
	FormTab,
	FormLayout,
	FormCollapse,
	Space,
];

export const designableArraySources: IResourceLike[] = [
	ArrayCards,
	ArrayTable,
];

export const designableDisplaySources: IResourceLike[] = [
	Text,
];
