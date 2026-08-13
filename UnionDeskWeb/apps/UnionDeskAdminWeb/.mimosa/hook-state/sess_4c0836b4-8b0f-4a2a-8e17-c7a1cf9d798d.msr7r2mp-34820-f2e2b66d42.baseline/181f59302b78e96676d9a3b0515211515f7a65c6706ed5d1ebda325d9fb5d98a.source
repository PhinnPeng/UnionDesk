import { AppstoreOutlined, TeamOutlined } from "@ant-design/icons";
import { Segmented } from "antd";
import { useTranslation } from "react-i18next";

export type MenuScope = "platform" | "business";

interface MenuScopeFilterProps {
	value: MenuScope
	onChange: (value: MenuScope) => void
}

export function MenuScopeFilter({ value, onChange }: MenuScopeFilterProps) {
	const { t } = useTranslation();

	return (
		<Segmented
			value={value}
			onChange={nextValue => onChange(nextValue as MenuScope)}
			size="large"
			className="w-full"
			block
			options={[
				{
					label: (
						<div className="flex items-center gap-2 px-4 py-2 w-full justify-center">
							<AppstoreOutlined />
							<div className="flex flex-col items-center text-center">
								<span className="font-medium">{t("system.menu.platformScope")}</span>
							</div>
						</div>
					),
					value: "platform",
				},
				{
					label: (
						<div className="flex items-center gap-2 px-4 py-2 w-full justify-center">
							<TeamOutlined />
							<div className="flex flex-col items-center text-center">
								<span className="font-medium">{t("system.menu.businessScope")}</span>
							</div>
						</div>
					),
					value: "business",
				},
			]}
		/>
	);
}
