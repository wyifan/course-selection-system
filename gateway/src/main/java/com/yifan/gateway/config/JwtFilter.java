
package com.yifan.gateway.config;

import com.example.common.util.JwtUtil;  // 从common-module导入
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.util.List;

@Component
@Slf4j
public class JwtFilter extends OncePerRequestFilter {


    // 排除登录路径（不需JWT）
    private final String loginMatcher = "/auth/login";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        // 跳过登录路径
        if (loginMatcher.matches(request)) {
            chain.doFilter(request, response);
            return;
        }

        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            try {
                if (JwtUtil.validateToken(token)) {
                    Claims claims = JwtUtil.parseToken(token);
                    // 注入Header（使用wrapper修改请求）
                    HttpServletRequest wrappedRequest = new MutableHttpServletRequestWrapper(request);
                    wrappedRequest.addHeader("X-User-Id", claims.get("userId").toString());
                    wrappedRequest.addHeader("X-Tenant-Id", claims.get("tenantId").toString());
                    wrappedRequest.addHeader("X-Roles", String.join(",", (List<String>) claims.get("roles")));
                    log.info("JWT validated for user: {}", claims.get("username"));

                    chain.doFilter(wrappedRequest, response);
                    return;
                }
            } catch (Exception e) {
                log.error("JWT validation failed: {}", e.getMessage());
            }
        }

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.getWriter().write("Unauthorized");
    }

    // 自定义Wrapper以支持addHeader
    private static class MutableHttpServletRequestWrapper extends ContentCachingRequestWrapper {
        public MutableHttpServletRequestWrapper(HttpServletRequest request) {
            super(request);
        }

        public void addHeader(String name, String value) {
            super.addHeader(name, value);
        }
    }
}
