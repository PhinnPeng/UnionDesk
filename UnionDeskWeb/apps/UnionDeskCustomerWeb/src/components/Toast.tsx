import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from "react";

type ToastKind = "info" | "ok" | "err";

type ToastItem = {
	id: number;
	text: string;
	kind: ToastKind;
};

type ToastApi = {
	show: (text: string, kind?: ToastKind) => void;
	success: (text: string) => void;
	error: (text: string) => void;
};

const ToastContext = createContext<ToastApi | null>(null);

let toastSeq = 1;

export function ToastProvider({ children }: { children: ReactNode }) {
	const [items, setItems] = useState<ToastItem[]>([]);

	const show = useCallback((text: string, kind: ToastKind = "info") => {
		const id = toastSeq++;
		setItems(prev => [...prev, { id, text, kind }]);
		window.setTimeout(() => {
			setItems(prev => prev.filter(item => item.id !== id));
		}, 2600);
	}, []);

	const api = useMemo<ToastApi>(() => ({
		show,
		success: text => show(text, "ok"),
		error: text => show(text, "err"),
	}), [show]);

	return (
		<ToastContext.Provider value={api}>
			{children}
			<div className="ud-toast-host" aria-live="polite">
				{items.map(item => (
					<div
						key={item.id}
						className={`ud-toast${item.kind === "ok" ? " ud-toast--ok" : ""}${item.kind === "err" ? " ud-toast--err" : ""}`}
					>
						{item.text}
					</div>
				))}
			</div>
		</ToastContext.Provider>
	);
}

export function useToast(): ToastApi {
	const ctx = useContext(ToastContext);
	if (!ctx) {
		throw new Error("useToast must be used within ToastProvider");
	}
	return ctx;
}
