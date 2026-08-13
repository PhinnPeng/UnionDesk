import { createCaptchaChallenge, verifyCaptcha } from "#src/api/auth";

import { SliderCaptcha } from "@uniondesk/shared";
import type { SliderCaptchaVerifier, TrackPoint } from "@uniondesk/shared";

import { useCallback, useRef, useState } from "react";

interface LoginCaptchaProps {
	enabled: boolean
	hint?: string
	disabled?: boolean
	onVerified: (captchaToken: string) => void
	onError: (message: string) => void
}

const CHALLENGE_EXPIRATION_BUFFER_MS = 5_000;

type CachedCaptchaChallenge = {
	challengeId: string
	expiresAt: number
};

export function LoginCaptcha({
	enabled,
	hint,
	disabled = false,
	onVerified,
	onError,
}: LoginCaptchaProps) {
	const challengeRef = useRef<CachedCaptchaChallenge | null>(null);
	const [challengeLoading, setChallengeLoading] = useState(false);

	const loadChallenge = useCallback(async () => {
		const cachedChallenge = challengeRef.current;
		if (cachedChallenge && cachedChallenge.expiresAt - Date.now() > CHALLENGE_EXPIRATION_BUFFER_MS) {
			return cachedChallenge.challengeId;
		}

		setChallengeLoading(true);
		try {
			const challenge = await createCaptchaChallenge();
			challengeRef.current = {
				challengeId: challenge.challengeId,
				expiresAt: Date.now() + challenge.expiresInSeconds * 1000,
			};
			return challenge.challengeId;
		}
		finally {
			setChallengeLoading(false);
		}
	}, []);

	const handleVerify: SliderCaptchaVerifier = useCallback(async (track: TrackPoint[]) => {
		try {
			const challengeId = await loadChallenge();
			const result = await verifyCaptcha({
				challengeId,
				track,
			});
			challengeRef.current = null;
			onVerified(result.captchaToken);
			return {
				success: true,
				token: result.captchaToken,
			};
		}
		catch (error) {
			challengeRef.current = null;
			const message = error instanceof Error ? error.message : "验证码校验失败";
			onError(message);
			return {
				success: false,
				message,
			};
		}
	}, [loadChallenge, onError, onVerified]);

	if (!enabled) {
		return null;
	}

	return (
		<div className="space-y-2">
			{hint ? (
				<p className="text-sm text-colorTextSecondary">
					{hint}
				</p>
			) : null}
			<SliderCaptcha
				verifier={handleVerify}
				disabled={disabled || challengeLoading}
			/>
		</div>
	);
}
