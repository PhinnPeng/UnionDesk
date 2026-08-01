import {
	getLoadingGeneration,
	getLoadingShownAt,
	getPendingHideTimer,
	LOADING_FADE_OUT_MS,
	LOADING_MIN_VISIBLE_MS,
	loadingContainerId,
	setPendingHideTimer,
} from "./loading";

function startFadeOut(loadingElement: HTMLElement, generation: number) {
	if (generation !== getLoadingGeneration()) {
		return;
	}
	if (!document.getElementById(loadingContainerId)) {
		return;
	}
	if (loadingElement.dataset.hiding === "1") {
		return;
	}

	loadingElement.dataset.hiding = "1";
	loadingElement.style.visibility = "hidden";
	loadingElement.style.opacity = "0";
	loadingElement.style.transition = `opacity ${LOADING_FADE_OUT_MS}ms ease-out, visibility ${LOADING_FADE_OUT_MS}ms ease-out`;
	loadingElement.addEventListener(
		"transitionend",
		() => {
			if (generation !== getLoadingGeneration()) {
				return;
			}
			loadingElement.remove();
		},
		{ once: true },
	);
}

/**
 * 幂等隐藏全屏加载层。若展示未满最短时长，则延后到满后再淡出。
 */
export function hideLoading() {
	const loadingElement = document.getElementById(loadingContainerId);
	if (!loadingElement) {
		return;
	}
	if (loadingElement.dataset.hiding === "1") {
		return;
	}
	if (getPendingHideTimer() != null) {
		return;
	}

	const generation = getLoadingGeneration();
	const elapsed = Date.now() - getLoadingShownAt();
	const waitMs = Math.max(0, LOADING_MIN_VISIBLE_MS - elapsed);

	const runHide = () => {
		setPendingHideTimer(null);
		startFadeOut(loadingElement, generation);
	};

	if (waitMs > 0) {
		setPendingHideTimer(setTimeout(runHide, waitMs));
		return;
	}
	runHide();
}
