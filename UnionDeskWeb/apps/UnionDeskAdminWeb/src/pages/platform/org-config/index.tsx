import { BasicContent } from "#src/components/basic-content";

import { SettingOutlined } from "@ant-design/icons";
import { Card, Empty } from "antd";

export default function PlatformOrgConfig() {
	return (
		<BasicContent className="h-full bg-colorBgLayout">
			<Card
				className="h-full"
				title={(
					<span>
						<SettingOutlined className="mr-2" />
						组织配置
					</span>
				)}
			>
				<Empty description="组织配置功能开发中" />
			</Card>
		</BasicContent>
	);
}
