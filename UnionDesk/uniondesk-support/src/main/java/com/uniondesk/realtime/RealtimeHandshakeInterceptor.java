package com.uniondesk.realtime;

import com.uniondesk.auth.core.JwtTokenService;
import com.uniondesk.auth.core.UserContext;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

/**
 * WS 握手鉴权：query 携带 ?token={accessToken}（浏览器 WS 无法自定义 Header），
 * 解析 JWT 得到身份（userId/role/默认域）后放入 session attributes，失败 401。
 */
@Component
public class RealtimeHandshakeInterceptor implements HandshakeInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RealtimeHandshakeInterceptor.class);

    static final String ATTR_IDENTITY = "realtime.identity";

    private final JwtTokenService jwtTokenService;

    public RealtimeHandshakeInterceptor(JwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) {
        String token = resolveToken(request);
        if (!StringUtils.hasText(token)) {
            log.warn("WS 握手拒绝：缺少 token");
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        try {
            UserContext context = jwtTokenService.parseAccessToken(token);
            if (context == null) {
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }
            attributes.put(ATTR_IDENTITY, identityOf(context));
            return true;
        } catch (Exception ex) {
            log.warn("WS 握手拒绝：token 无效");
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
        // 无后续处理
    }

    private String resolveToken(ServerHttpRequest request) {
        // 兼容 Sec-WebSocket-Protocol 子协议头与 ?token= 查询参数（子协议优先）
        String protocol = request.getHeaders().getFirst("Sec-WebSocket-Protocol");
        if (StringUtils.hasText(protocol)) {
            for (String part : protocol.split(",")) {
                String candidate = part.trim();
                if (!candidate.isBlank() && !"chat".equals(candidate) && !"graphql-ws".equals(candidate)) {
                    return candidate;
                }
            }
        }
        String query = request.getURI().getQuery();
        if (!StringUtils.hasText(query)) {
            return null;
        }
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2 && "token".equals(kv[0]) && StringUtils.hasText(kv[1])) {
                return java.net.URLDecoder.decode(kv[1], java.nio.charset.StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    private RealtimeSessionRegistry.SessionIdentity identityOf(UserContext context) {
        boolean customer = "customer".equalsIgnoreCase(context.role());
        String actorType = customer ? RealtimeConstants.ACTOR_CUSTOMER : RealtimeConstants.ACTOR_STAFF;
        Long domainId = context.businessDomainId();
        return new RealtimeSessionRegistry.SessionIdentity(actorType, context.userId(), domainId);
    }
}
