package com.uniondesk.auth.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuthCaptchaServiceTests {

    private MutableClock clock;
    private AuthCaptchaService authCaptchaService;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-04-29T08:00:00Z"));
        authCaptchaService = new AuthCaptchaService(clock);
    }

    @Test
    void verifyRejectsShortTrack() {
        AuthCaptchaService.CaptchaChallenge challenge = authCaptchaService.createChallenge();

        assertThatThrownBy(() -> authCaptchaService.verify(
                challenge.challengeId(),
                List.of(new AuthCaptchaService.TrackPoint(0, 0))))
                .isInstanceOf(AuthCaptchaException.class)
                .hasMessage("滑动轨迹过短");
    }

    @Test
    void verifyRejectsAbnormalSpeed() {
        AuthCaptchaService.CaptchaChallenge challenge = authCaptchaService.createChallenge();

        assertThatThrownBy(() -> authCaptchaService.verify(
                challenge.challengeId(),
                List.of(
                        new AuthCaptchaService.TrackPoint(0, 0),
                        new AuthCaptchaService.TrackPoint(320, 50))))
                .isInstanceOf(AuthCaptchaException.class)
                .hasMessage("滑动速度过快");
    }

    @Test
    void consumeTokenAllowsOnlyOnce() {
        AuthCaptchaService.CaptchaChallenge challenge = authCaptchaService.createChallenge();
        AuthCaptchaService.CaptchaToken token = authCaptchaService.verify(
                challenge.challengeId(),
                List.of(
                        new AuthCaptchaService.TrackPoint(0, 0),
                        new AuthCaptchaService.TrackPoint(120, 450),
                        new AuthCaptchaService.TrackPoint(320, 900)));

        authCaptchaService.consumeToken(token.captchaToken());

        assertThatThrownBy(() -> authCaptchaService.consumeToken(token.captchaToken()))
                .isInstanceOf(AuthCaptchaException.class)
                .hasMessage("验证码已失效");
    }

    @Test
    void verifyRejectsExpiredChallenge() {
        AuthCaptchaService.CaptchaChallenge challenge = authCaptchaService.createChallenge();
        clock.plus(Duration.ofMinutes(3));

        assertThatThrownBy(() -> authCaptchaService.verify(
                challenge.challengeId(),
                List.of(
                        new AuthCaptchaService.TrackPoint(0, 0),
                        new AuthCaptchaService.TrackPoint(320, 900))))
                .isInstanceOf(AuthCaptchaException.class)
                .hasMessage("验证码已失效");
    }

    @Test
    void verifyIssuesNonBlankTokenForValidTrack() {
        AuthCaptchaService.CaptchaChallenge challenge = authCaptchaService.createChallenge();

        AuthCaptchaService.CaptchaToken token = authCaptchaService.verify(
                challenge.challengeId(),
                List.of(
                        new AuthCaptchaService.TrackPoint(0, 0),
                        new AuthCaptchaService.TrackPoint(80, 260),
                        new AuthCaptchaService.TrackPoint(180, 620),
                        new AuthCaptchaService.TrackPoint(320, 1100)));

        assertThat(token.captchaToken()).isNotBlank();
        assertThat(token.expiresInSeconds()).isPositive();
    }

    private static final class MutableClock extends Clock {

        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void plus(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
