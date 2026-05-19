import { describe, expect, it } from "vitest";

import { PERMISSION_CODE_LABELS } from "./permission-code-labels";

describe("permissionCodeLabels", () => {
	it("includes organization and platform user permission labels", () => {
		expect(PERMISSION_CODE_LABELS["platform.organization.read"]).toBe("查看组织架构");
		expect(PERMISSION_CODE_LABELS["platform.organization.create"]).toBe("新增组织");
		expect(PERMISSION_CODE_LABELS["platform.organization.update"]).toBe("编辑组织");
		expect(PERMISSION_CODE_LABELS["platform.organization.delete"]).toBe("删除组织");
		expect(PERMISSION_CODE_LABELS["platform.user.import"]).toBe("导入用户");
		expect(PERMISSION_CODE_LABELS["platform.user.disable"]).toBe("平台用户离职");
		expect(PERMISSION_CODE_LABELS["platform.user.reset_password"]).toBe("重置平台用户密码");
		expect(PERMISSION_CODE_LABELS["platform.user.offboard_pool.export"]).toBe("导出离职池");
		expect(PERMISSION_CODE_LABELS["platform.user.offboard_pool.batch_restore"]).toBe("批量恢复");
	});
});
