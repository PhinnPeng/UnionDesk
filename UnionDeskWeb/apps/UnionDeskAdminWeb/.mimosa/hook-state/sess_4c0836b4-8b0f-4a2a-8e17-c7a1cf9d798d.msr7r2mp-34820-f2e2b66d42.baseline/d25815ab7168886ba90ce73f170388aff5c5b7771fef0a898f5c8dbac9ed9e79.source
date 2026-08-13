import { HttpRequestError } from "#src/utils/http-request-error";

const API_ERROR_MESSAGES: Record<string, string> = {
	"10001": "用户名或密码错误",
	"10003": "验证码校验失败，请重新完成滑块验证",
	"40101": "登录已过期，请重新登录",
	"40102": "客户端标识缺失，请刷新页面后重试",
	"40301": "无操作权限",
	"40401": "请求的资源不存在",
	"40001": "提交的信息不完整或格式不正确，请检查后重试",
	"40002": "请求参数错误，请检查后重试",
	"50001": "服务暂时不可用，请稍后重试",
};

function normalizeNetworkMessage(message: string) {
	if (/failed to fetch|networkerror|load failed/i.test(message)) {
		return "网络连接失败，请检查网络或确认后端服务已启动";
	}
	return message;
}

export function resolveRequestErrorMessage(error: unknown, fallback = "操作失败，请稍后重试"): string {
	if (error instanceof HttpRequestError) {
		const code = error.code?.trim();
		if (code && API_ERROR_MESSAGES[code]) {
			return API_ERROR_MESSAGES[code];
		}
		if (error.status === 401) {
			return API_ERROR_MESSAGES["40101"] ?? "登录已过期，请重新登录";
		}
		if (error.status >= 500) {
			return API_ERROR_MESSAGES["50001"] ?? "服务暂时不可用，请稍后重试";
		}
		if (error.message.trim()) {
			return normalizeNetworkMessage(error.message.trim());
		}
		return fallback;
	}

	if (error instanceof Error && error.message.trim()) {
		return normalizeNetworkMessage(error.message.trim());
	}

	return fallback;
}
