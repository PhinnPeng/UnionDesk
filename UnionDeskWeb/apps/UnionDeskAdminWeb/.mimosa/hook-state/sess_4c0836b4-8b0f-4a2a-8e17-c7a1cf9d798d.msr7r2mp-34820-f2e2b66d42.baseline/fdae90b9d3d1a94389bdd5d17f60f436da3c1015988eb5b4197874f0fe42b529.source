import type { PopconfirmProps } from "antd";

import { EnterOutlined } from "@ant-design/icons";
import { Popconfirm } from "antd";
import { useEffect, useRef, useCallback, useState } from "react";

interface ConfirmPopoverProps extends PopconfirmProps {
	/** 是否启用回车键确认，默认 true */
	enterToConfirm?: boolean;
	/** 是否显示快捷键提示，默认 true */
	showShortcutTip?: boolean;
}

/**
 * 增强版 Popconfirm，支持回车键确认和快捷键提示
 */
export function ConfirmPopover(props: ConfirmPopoverProps) {
	const {
		children,
		onConfirm,
		enterToConfirm = true,
		showShortcutTip = true,
		okText = "确认",
		cancelText = "取消",
		title,
		...restProps
	} = props;

	const popoverRef = useRef<HTMLDivElement>(null);
	const onConfirmRef = useRef(onConfirm);
	const [open, setOpen] = useState(false);

	// 保持最新回调引用
	useEffect(() => {
		onConfirmRef.current = onConfirm;
	}, [onConfirm]);

	// document 级键盘监听，弹窗打开时按 Enter 触发确认
	useEffect(() => {
		if (!open || !enterToConfirm) return;

		const handleKeyDown = (e: KeyboardEvent) => {
			if (e.key === "Enter" && !e.repeat) {
				e.preventDefault();
				e.stopPropagation();
				onConfirmRef.current?.();
			}
		};

		document.addEventListener("keydown", handleKeyDown);
		return () => document.removeEventListener("keydown", handleKeyDown);
	}, [open, enterToConfirm]);

	const handleOpenChange = useCallback((nextOpen: boolean) => {
		setOpen(nextOpen);
	}, []);

	// 确认按钮带 Enter 图标提示
	const okButtonProps = showShortcutTip
		? { icon: <EnterOutlined style={{ fontSize: 12 }} />, iconPosition: "end" as const }
		: undefined;

	return (
		<div ref={popoverRef}>
			<Popconfirm
				title={title}
				onConfirm={onConfirm}
				okText={okText}
				cancelText={cancelText}
				okButtonProps={okButtonProps}
				onOpenChange={handleOpenChange}
				{...restProps}
			>
				{children}
			</Popconfirm>
		</div>
	);
}

export default ConfirmPopover;
