import { describe, expect, it } from "vitest";

import { HttpRequestError } from "#src/utils/http-request-error";

import { resolveRequestErrorMessage } from "./resolve-request-error";

describe("resolveRequestErrorMessage", () => {
	it("maps known API error codes to friendly Chinese messages", () => {
		expect(resolveRequestErrorMessage(new HttpRequestError(401, "Unauthorized", "10001")))
			.toBe("用户名或密码错误");
		expect(resolveRequestErrorMessage(new HttpRequestError(400, "bad", "10003")))
			.toBe("验证码校验失败，请重新完成滑块验证");
	});

	it("returns friendly message for server errors", () => {
		expect(resolveRequestErrorMessage(new HttpRequestError(500, "Internal Server Error", "50001")))
			.toBe("服务暂时不可用，请稍后重试");
	});
});
