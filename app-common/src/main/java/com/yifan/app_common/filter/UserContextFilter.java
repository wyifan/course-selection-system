package com.yifan.app_common.filter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import java.io.IOException;

import com.yifan.app_common.context.UserContext;
import com.yifan.app_common.entity.UserInfo;
import com.yifan.app_common.common.HeaderInfo;

// @Component to enable this filter 但是考虑到有些服务可能不需要传递用户信息，可以选择性配置，不在common中进行侵入式的注册
public class UserContextFilter {
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        try {
            // 从 Header 读取用户信息
            String userId = httpRequest.getHeader(HeaderInfo.USER_ID);
            String role = httpRequest.getHeader(HeaderInfo.USER_ROLE);

            if (userId != null) {
                UserContext.setUser(new UserInfo(userId, role));
            }

            chain.doFilter(request, response);
        } finally {
            // ⭐ 请求完成后必须清理，否则线程复用会串号
            UserContext.clear();
        }
    }
}
