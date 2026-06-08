package com.uniondesk.auth.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {

    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private final SecretKeySpec signingKey;
    private final String issuer;
    private final Duration accessTokenTtl;
    private final Duration refreshTokenTtl;

    public JwtTokenService(
            ObjectMapper objectMapper,
            @Value("${uniondesk.security.jwt.secret}") String secret,
            @Value("${uniondesk.security.jwt.issuer:uniondesk}") String issuer,
            @Value("${uniondesk.security.jwt.access-token-ttl:PT24H}") Duration accessTokenTtl,
            @Value("${uniondesk.security.jwt.refresh-token-ttl:P7D}") Duration refreshTokenTtl) {
        this.objectMapper = objectMapper;
        this.signingKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        this.issuer = issuer;
        this.accessTokenTtl = accessTokenTtl;
        this.refreshTokenTtl = refreshTokenTtl;
    }

    public String issueAccessToken(UserContext context) {
        return issueToken(context, accessTokenTtl, "access");
    }

    public String issueRefreshToken(UserContext context) {
        return issueToken(context, refreshTokenTtl, "refresh");
    }

    public UserContext parseAccessToken(String token) {
        return parseToken(token, "access");
    }

    public UserContext parseRefreshToken(String token) {
        return parseToken(token, "refresh");
    }

    public Duration accessTokenTtl() {
        return accessTokenTtl;
    }

    private String issueToken(UserContext context, Duration ttl, String tokenType) {
        Instant now = Instant.now();
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("iss", issuer);
        claims.put("sub", Long.toString(context.userId()));
        claims.put("uid", context.userId());
        claims.put("sid", context.sessionId());
        claims.put("role", context.role());
        claims.put("cid", context.clientCode());
        claims.put("typ", tokenType);
        if (context.businessDomainId() != null) {
            claims.put("bd", context.businessDomainId());
        }
        claims.put("iat", now.getEpochSecond());
        claims.put("exp", now.plus(ttl).getEpochSecond());
        return sign(claims);
    }

    private UserContext parseToken(String token, String expectedType) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("invalid access token");
        }
        verifySignature(parts[0], parts[1], parts[2]);
        Map<String, Object> claims = decodeClaims(parts[1]);
        if (!issuer.equals(stringClaim(claims, "iss"))) {
            throw new IllegalArgumentException("invalid access token");
        }
        if (!expectedType.equals(stringClaim(claims, "typ"))) {
            throw new IllegalArgumentException("invalid access token");
        }
        long expiresAt = longClaim(claims, "exp");
        if (Instant.now().getEpochSecond() >= expiresAt) {
            throw new IllegalArgumentException("access token expired");
        }
        long userId = longClaim(claims, "uid");
        String role = stringClaim(claims, "role");
        String sessionId = stringClaim(claims, "sid");
        String clientCode = stringClaim(claims, "cid");
        Long businessDomainId = optionalLongClaim(claims, "bd");
        return new UserContext(userId, role, businessDomainId, sessionId, clientCode);
    }

    private String sign(Map<String, Object> claims) {
        try {
            String header = encodedJson(Map.of("alg", "HS256", "typ", "JWT"));
            String payload = encodedJson(claims);
            String signature = encode(sign(signingInput(header, payload)));
            return header + "." + payload + "." + signature;
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("unable to serialize jwt claims", ex);
        }
    }

    private void verifySignature(String header, String payload, String signature) {
        byte[] expectedBytes = sign(signingInput(header, payload));
        byte[] actualBytes = decode(signature);
        if (!MessageDigest.isEqual(expectedBytes, actualBytes)) {
            throw new IllegalArgumentException("invalid access token");
        }
    }

    private byte[] signingInput(String header, String payload) {
        return (header + "." + payload).getBytes(StandardCharsets.UTF_8);
    }

    private String encodedJson(Object value) throws JsonProcessingException {
        return encode(objectMapper.writeValueAsBytes(value));
    }

    private Map<String, Object> decodeClaims(String encodedClaims) {
        try {
            byte[] payload = decode(encodedClaims);
            return objectMapper.readValue(payload, MAP_TYPE);
        } catch (Exception ex) {
            throw new IllegalArgumentException("invalid access token", ex);
        }
    }

    private String stringClaim(Map<String, Object> claims, String key) {
        Object value = claims.get(key);
        if (value == null) {
            throw new IllegalArgumentException("invalid access token");
        }
        String result = value.toString();
        if (result.isBlank()) {
            throw new IllegalArgumentException("invalid access token");
        }
        return result;
    }

    private long longClaim(Map<String, Object> claims, String key) {
        Object value = claims.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("invalid access token", ex);
            }
        }
        throw new IllegalArgumentException("invalid access token");
    }

    private Long optionalLongClaim(Map<String, Object> claims, String key) {
        Object value = claims.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("invalid access token", ex);
            }
        }
        throw new IllegalArgumentException("invalid access token");
    }

    private String encode(byte[] value) {
        return ENCODER.encodeToString(value);
    }

    private byte[] decode(String value) {
        return DECODER.decode(value);
    }

    private byte[] sign(byte[] value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(signingKey);
            return mac.doFinal(value);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("HmacSHA256 is unavailable", ex);
        } catch (java.security.InvalidKeyException ex) {
            throw new IllegalStateException("jwt signing key is invalid", ex);
        }
    }
}
