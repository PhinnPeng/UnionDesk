export class HttpRequestError extends Error {
	readonly status: number;

	readonly code?: string;

	constructor(status: number, message: string, code?: string) {
		super(message);
		this.name = "HttpRequestError";
		this.status = status;
		this.code = code;
	}
}

export function isHttpRequestError(error: unknown): error is HttpRequestError {
	return error instanceof HttpRequestError;
}

export function isUnauthorizedHttpError(error: unknown) {
	if (isHttpRequestError(error)) {
		return error.status === 401;
	}

	if (typeof error !== "object" || error === null) {
		return false;
	}

	if ("status" in error && (error as { status?: number }).status === 401) {
		return true;
	}

	const response = "response" in error ? (error as { response?: { status?: number } }).response : undefined;
	return response?.status === 401;
}
