package cn.wubo.spring.ai.loom.agent.user;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
public class AuthenticationFilter implements Filter {

    private final IUser user;

    public AuthenticationFilter(IUser user) {
        this.user = user;
    }

    @Override
    public void doFilter(jakarta.servlet.ServletRequest servletRequest, jakarta.servlet.ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        String path = request.getRequestURI();

        // 白名单：不需要认证的路径
        if (path.startsWith("/spring/ai/loom/user/login") ||
                path.startsWith("/spring/ai/loom/user/isAutoLogin") ||
                path.equals("/spring/ai/loom") ||
                path.startsWith("/spring/ai/loom/index.html") ||
                path.startsWith("/spring/ai/loom/app.js") ||
                path.startsWith("/spring/ai/loom/style.css")) {
            chain.doFilter(request, response);
            return;
        }

        // 检查认证头信息
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        // 验证token并设置用户上下文，在 chain.doFilter 之前完成
        String username = user.getUsernameByAuthentication(authHeader);
        UserContextHolder.setCurrentUser(username);
        try {
            chain.doFilter(request, response);
        } finally {
            UserContextHolder.clear();
        }
    }
}
