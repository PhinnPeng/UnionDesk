import { usePreferencesStore } from "#src/store/preferences";
import { isDarkTheme } from "#src/utils/is-dark-theme";

export const loadingId = "loading-e8a3a985";
export const loadingContainerId = "loading-container-e8a3a985";
export const loadingMessageId = "loading-message-e8a3a985";

/** 递增以取消进行中的 hide 淡出移除 */
let loadingGeneration = 0;
/** 最近一次真正展示全屏层的时间戳（用于最短展示时长） */
let loadingShownAt = 0;
let pendingHideTimer: ReturnType<typeof setTimeout> | null = null;

/** 全屏层至少展示多久再开始淡出，避免切端过快时一闪而过 */
export const LOADING_MIN_VISIBLE_MS = 560;
/** 淡出动画时长 */
export const LOADING_FADE_OUT_MS = 600;

export function bumpLoadingGeneration() {
	loadingGeneration += 1;
	return loadingGeneration;
}

export function getLoadingGeneration() {
	return loadingGeneration;
}

export function getLoadingShownAt() {
	return loadingShownAt;
}

export function clearPendingHideTimer() {
	if (pendingHideTimer != null) {
		clearTimeout(pendingHideTimer);
		pendingHideTimer = null;
	}
}

export function setPendingHideTimer(timer: ReturnType<typeof setTimeout> | null) {
	pendingHideTimer = timer;
}

export function getPendingHideTimer() {
	return pendingHideTimer;
}

function resolveBackgroundColor() {
	return isDarkTheme(usePreferencesStore.getState().theme)
		? "#181818"
		: "rgba(255, 255, 255, 0.88)";
}

function resolveMessageColor() {
	return isDarkTheme(usePreferencesStore.getState().theme)
		? "rgba(255, 255, 255, 0.65)"
		: "rgba(0, 0, 0, 0.65)";
}

function buildLoadingMarkup() {
	const messageHtml = `<div id="${loadingMessageId}" style="display:none;margin-top:16px;font-size:14px;line-height:22px;color:${resolveMessageColor()};text-align:center;"></div>`;

	return `
<style>
#${loadingContainerId} {
	position: fixed;
	inset: 0;
	z-index: 9999999;
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	height: 100vh;
	width: 100vw;
	background-color: ${resolveBackgroundColor()};
	overflow: hidden;
}
#${loadingId},
#${loadingId}::before,
#${loadingId}::after {
	width: 2.5em;
	height: 2.5em;
	border-radius: 50%;
	animation: animation-loader 1.8s infinite ease-in-out;
	animation-fill-mode: both;
}

#${loadingId} {
	position: relative;
	top: 0;
	margin: 0 auto;
	font-size: 10px;
	color: ${usePreferencesStore.getState().themeColorPrimary};
	text-indent: -9999em;
	transform: translateZ(0);
	transform: translate(-50%, 0);
	animation-delay: -0.16s;
}

#${loadingId}::before,
#${loadingId}::after {
	position: absolute;
	top: 0;
	content: "";
}

#${loadingId}::before {
	left: -3.5em;
	animation-delay: -0.32s;
}

#${loadingId}::after {
	left: 3.5em;
}

@keyframes animation-loader {
	0%,
	80%,
	100% {
		box-shadow: 0 2.5em 0 -1.3em;
	}

	40% {
		box-shadow: 0 2.5em 0 0;
	}
}
</style>
<div id="${loadingId}"></div>
${messageHtml}
`;
}

function setLoadingMessage(container: HTMLElement, message?: string) {
	const messageEl = container.querySelector<HTMLElement>(`#${loadingMessageId}`);
	if (!messageEl) {
		return;
	}
	if (message == null || message === "") {
		messageEl.style.display = "none";
		messageEl.textContent = "";
		return;
	}
	messageEl.style.display = "block";
	messageEl.style.color = resolveMessageColor();
	messageEl.textContent = message;
}

function restoreLoadingVisibility(container: HTMLElement) {
	container.dataset.hiding = "0";
	container.style.visibility = "visible";
	container.style.opacity = "1";
	container.style.transition = "";
	container.style.backgroundColor = resolveBackgroundColor();
}

/**
 * 幂等展示全屏加载层。已存在则恢复可见并可更新文案；无文案参数时保留原有文案。
 */
export function showLoading(message?: string) {
	bumpLoadingGeneration();
	clearPendingHideTimer();
	const existing = document.getElementById(loadingContainerId);
	if (existing) {
		const wasHiding = existing.dataset.hiding === "1";
		restoreLoadingVisibility(existing);
		if (wasHiding || loadingShownAt <= 0) {
			loadingShownAt = Date.now();
		}
		if (message != null) {
			setLoadingMessage(existing, message);
		}
		return;
	}

	loadingShownAt = Date.now();
	const loadingDiv = document.createElement("div");
	loadingDiv.id = loadingContainerId;
	loadingDiv.dataset.hiding = "0";
	loadingDiv.style.opacity = "0";
	loadingDiv.innerHTML = `<!-- Page transition loading overlay -->${buildLoadingMarkup()}`;
	if (message != null) {
		setLoadingMessage(loadingDiv, message);
	}

	const app = document.getElementById("root");
	if (app) {
		app.before(loadingDiv);
		requestAnimationFrame(() => {
			const el = document.getElementById(loadingContainerId);
			if (!el || el.dataset.hiding === "1") {
				return;
			}
			el.style.transition = "opacity 0.22s ease-out";
			el.style.opacity = "1";
		});
	}
}

/**
 * Preview loading page / 兼容别名。
 * https://github.com/user-attachments/assets/110701a8-2cf4-4e5f-a07e-b832da4e1586
 */
export function setupLoading() {
	showLoading();
}
