import { requestBackendJson } from "#src/utils/request";
import { request } from "#src/utils/request";

export type ImportTaskStatus = "pending" | "processing" | "success" | "failed";

export interface ImportTaskErrorRow {
	row: number
	message: string
}

export interface ImportTaskView {
	id: string
	task_type: string
	file_name: string
	status: ImportTaskStatus
	total_count: number
	success_count: number
	fail_count: number
	error_summary: ImportTaskErrorRow[] | null
	created_at: string | null
	finished_at: string | null
}

interface ImportTaskCreateResponse {
	taskId: string
}

/** 上传员工 Excel 并创建异步导入任务 */
export async function uploadStaffImport(file: File): Promise<ImportTaskView> {
	const form = new FormData();
	form.append("file", file);
	const response = await request.post("v1/admin/import-export/staff/import", {
		body: form,
	});
	const created = await response.json<ImportTaskCreateResponse>();
	return fetchImportTask(created.taskId);
}

/** 查询导入任务状态与统计（前端轮询用，失败时不弹全局错误） */
export async function fetchImportTask(taskId: string): Promise<ImportTaskView> {
	return requestBackendJson<ImportTaskView>(`v1/admin/import-export/tasks/${taskId}`, {
		silentError: true,
	});
}

/** 导出员工 CSV（浏览器直接下载） */
export async function exportStaffCsv(): Promise<void> {
	const blob = await request.get("v1/admin/import-export/staff/export").blob();
	const url = URL.createObjectURL(blob);
	const anchor = document.createElement("a");
	anchor.href = url;
	anchor.download = `用户导出_${new Date().toISOString().slice(0, 10)}.csv`;
	anchor.click();
	URL.revokeObjectURL(url);
}
