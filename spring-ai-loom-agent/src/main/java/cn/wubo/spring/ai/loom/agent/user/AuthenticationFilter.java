package cn.wubo.spring.ai.loom.agent.user;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

public class AuthenticationFilter implements WebFilter {

    private final IUser user;

    public AuthenticationFilter(IUser user) {
        this.user = user;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().toString();

        // 排除不需要认证的路径
        if (path.startsWith("/spring/ai/chat/public/") ||
                path.equals("/spring/ai/user/login")) {
            return chain.filter(exchange);
        }

        // 检查认证头信息
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader == null) {
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }

        try {
            // 根据Authorization获取用户名并设置到线程上下文
            String username = user.getUsernameByAuthentication(authHeader);
            UserContextHolder.setCurrentUser(username);

            // 继续处理请求，并在完成后清理上下文
            return chain.filter(exchange)
                    .doFinally(signalType -> UserContextHolder.clear());
        } catch (Exception e) {
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }
    }
}
