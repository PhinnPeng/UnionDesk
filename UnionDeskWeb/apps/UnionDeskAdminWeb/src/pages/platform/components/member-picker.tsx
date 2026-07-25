import { fetchDomainMembersPage, type DomainMember } from "@uniondesk/shared";
import { Select, Spin } from "antd";
import { useEffect, useMemo, useState } from "react";

export type MemberOption = {
	value: number;
	label: string;
};

interface MemberPickerProps {
	domainId?: string | number | null;
	/** auto/domain 使用域成员；platform 暂复用同一接口（需 domain 上下文） */
	scopeMode?: "auto" | "domain" | "platform";
	multiple?: boolean;
	value?: number | number[] | null;
	placeholder?: string;
	disabled?: boolean;
	style?: React.CSSProperties;
	onChange?: (value: number | number[] | null) => void;
}

function toOption(item: DomainMember): MemberOption | null {
	const id = Number(item.staff_account_id);
	if (!Number.isFinite(id) || id <= 0) return null;
	const name = item.real_name?.trim()
		|| item.nickname?.trim()
		|| item.username?.trim()
		|| `员工 #${id}`;
	return { value: id, label: name };
}

/** 域/平台员工选择（单选或多选） */
export function MemberPicker({
	domainId,
	scopeMode = "auto",
	multiple = false,
	value,
	placeholder,
	disabled,
	style,
	onChange,
}: MemberPickerProps) {
	const [loading, setLoading] = useState(false);
	const [options, setOptions] = useState<MemberOption[]>([]);
	const resolvedDomainId = domainId == null || domainId === "" ? null : String(domainId);

	useEffect(() => {
		if (!resolvedDomainId) {
			setOptions([]);
			return;
		}
		let cancelled = false;
		setLoading(true);
		void fetchDomainMembersPage({
			domainId: resolvedDomainId,
			page: 1,
			page_size: 200,
			status: "active",
		})
			.then(page => {
				if (cancelled) return;
				const next = (page.list ?? [])
					.map(toOption)
					.filter((item): item is MemberOption => item != null);
				setOptions(next);
			})
			.catch(() => {
				if (!cancelled) setOptions([]);
			})
			.finally(() => {
				if (!cancelled) setLoading(false);
			});
		return () => {
			cancelled = true;
		};
	}, [resolvedDomainId, scopeMode]);

	const selectValue = useMemo(() => {
		if (multiple) {
			return Array.isArray(value) ? value : value == null ? [] : [value];
		}
		return Array.isArray(value) ? (value[0] ?? null) : (value ?? null);
	}, [multiple, value]);

	return (
		<Select
			mode={multiple ? "multiple" : undefined}
			allowClear
			showSearch
			optionFilterProp="label"
			loading={loading}
			disabled={disabled || !resolvedDomainId}
			placeholder={placeholder ?? (multiple ? "选择关注人" : "选择处理人")}
			options={options}
			value={selectValue as number | number[] | null}
			style={{ width: "100%", ...style }}
			notFoundContent={loading ? <Spin size="small" /> : "暂无可选成员"}
			onChange={next => {
				if (multiple) {
					onChange?.(Array.isArray(next) ? next.map(Number) : []);
					return;
				}
				onChange?.(next == null ? null : Number(next));
			}}
		/>
	);
}
