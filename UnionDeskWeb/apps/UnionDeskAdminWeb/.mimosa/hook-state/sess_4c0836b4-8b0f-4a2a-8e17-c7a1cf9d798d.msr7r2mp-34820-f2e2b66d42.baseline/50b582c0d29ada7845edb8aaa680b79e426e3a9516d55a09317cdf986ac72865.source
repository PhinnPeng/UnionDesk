import type { TicketStatusFlow, TransitionRule } from "@uniondesk/shared";
import { useCallback, useState } from "react";

const DRAFT_KEY_PREFIX = "workflow_draft_";
const EXPIRY_MS = 3 * 24 * 60 * 60 * 1000; // 3 天

interface DraftData {
	savedAt: number;
	statusFlow: TicketStatusFlow;
	rules: TransitionRule[];
}

function getDraftKey(domainId: string, typeId: string): string {
	return `${DRAFT_KEY_PREFIX}${domainId}::${typeId}`;
}

export function useWorkflowDraft(domainId: string, typeId: string) {
	const [hasDraft, setHasDraft] = useState(false);
	const [draftAge, setDraftAge] = useState<number | null>(null);

	const saveDraft = useCallback((statusFlow: TicketStatusFlow, rules: TransitionRule[]) => {
		if (!domainId || !typeId) return;
		const key = getDraftKey(domainId, typeId);
		const data: DraftData = { savedAt: Date.now(), statusFlow, rules };
		localStorage.setItem(key, JSON.stringify(data));
		setHasDraft(true);
	}, [domainId, typeId]);

	const loadDraft = useCallback((): { statusFlow: TicketStatusFlow; rules: TransitionRule[] } | null => {
		if (!domainId || !typeId) return null;
		const key = getDraftKey(domainId, typeId);
		const raw = localStorage.getItem(key);
		if (!raw) return null;
		try {
			const data: DraftData = JSON.parse(raw);
			if (Date.now() - data.savedAt > EXPIRY_MS) {
				localStorage.removeItem(key);
				setHasDraft(false);
				return null;
			}
			setHasDraft(true);
			setDraftAge(Date.now() - data.savedAt);
			return { statusFlow: data.statusFlow, rules: data.rules };
		}
		catch {
			localStorage.removeItem(key);
			return null;
		}
	}, [domainId, typeId]);

	const clearDraft = useCallback(() => {
		if (!domainId || !typeId) return;
		localStorage.removeItem(getDraftKey(domainId, typeId));
		setHasDraft(false);
		setDraftAge(null);
	}, [domainId, typeId]);

	return { hasDraft, draftAge, saveDraft, loadDraft, clearDraft };
}
