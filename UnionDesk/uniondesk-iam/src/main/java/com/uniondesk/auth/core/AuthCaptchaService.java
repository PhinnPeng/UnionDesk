package com.uniondesk.auth.core;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AuthCaptchaService {

    private static final Duration CHALLENGE_TTL = Duration.ofMinutes(2);
    private static final Duration TOKEN_TTL = Duration.ofMinutes(2);
    private static final int MIN_TRACK_POINTS = 2;
    private static final long MIN_DURATION_MS = 100L;
    private static final double MAX_AVERAGE_SPEED = 5D;
    private static final int MIN_FINAL_PROGRESS = 95;

    private final Clock clock;
    private final Map<String, Instant> challenges = new ConcurrentHashMap<>();
    private final Map<String, Instant> tokens = new ConcurrentHashMap<>();

    public AuthCaptchaService(Clock clock) {
        this.clock = clock;
    }

    public CaptchaChallenge createChallenge() {
        clearExpired();
        String challengeId = UUID.randomUUID().toString();
        challenges.put(challengeId, now().plus(CHALLENGE_TTL));
        return new CaptchaChallenge(challengeId, CHALLENGE_TTL.toSeconds());
    }

    public CaptchaToken verify(String challengeId, List<TrackPoint> track) {
        clearExpired();
        requireActiveChallenge(challengeId);
        verifyTrack(track);
        challenges.remove(challengeId);

        String captchaToken = UUID.randomUUID().toString();
        tokens.put(captchaToken, now().plus(TOKEN_TTL));
        return new CaptchaToken(captchaToken, TOKEN_TTL.toSeconds());
    }

    public void consumeToken(String captchaToken) {
        clearExpired();
        if (!StringUtils.hasText(captchaToken)) {
            throw new AuthCaptchaException("请先完成滑块验证");
        }
        Instant expiresAt = tokens.remove(captchaToken);
        if (expiresAt == null || !expiresAt.isAfter(now())) {
            throw new AuthCaptchaException("验证码已失效");
        }
    }

    private void requireActiveChallenge(String challengeId) {
        if (!StringUtils.hasText(challengeId)) {
            throw new AuthCaptchaException("验证码已失效");
        }
        Instant expiresAt = challenges.get(challengeId);
        if (expiresAt == null || !expiresAt.isAfter(now())) {
            challenges.remove(challengeId);
            throw new AuthCaptchaException("验证码已失效");
        }
    }

    private void verifyTrack(List<TrackPoint> track) {
        if (track == null || track.size() < MIN_TRACK_POINTS) {
            throw new AuthCaptchaException("滑动轨迹过短");
        }
        List<TrackPoint> sortedTrack = track.stream()
                .sorted(Comparator.comparingLong(TrackPoint::t))
                .toList();
        TrackPoint first = sortedTrack.get(0);
        TrackPoint last = sortedTrack.get(sortedTrack.size() - 1);
        long duration = last.t() - first.t();
        if (duration < MIN_DURATION_MS) {
            throw new AuthCaptchaException("滑动速度过快");
        }
        double distance = Math.max(0, last.x() - first.x());
        double averageSpeed = distance / duration;
        if (averageSpeed > MAX_AVERAGE_SPEED) {
            throw new AuthCaptchaException("滑动速度异常");
        }
        if (last.x() < MIN_FINAL_PROGRESS) {
            throw new AuthCaptchaException("未滑动到最右边");
        }
    }

    private void clearExpired() {
        Instant now = now();
        challenges.entrySet().removeIf(entry -> !entry.getValue().isAfter(now));
        tokens.entrySet().removeIf(entry -> !entry.getValue().isAfter(now));
    }

    private Instant now() {
        return Instant.now(clock);
    }

    public record CaptchaChallenge(String challengeId, long expiresInSeconds) {
    }

    public record CaptchaToken(String captchaToken, long expiresInSeconds) {
    }

    public record TrackPoint(int x, long t) {
    }
}
