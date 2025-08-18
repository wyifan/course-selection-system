package com.yifan.app_common.filter;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.util.Optional;

import com.yifan.app_common.context.UserContext;

import lombok.extern.slf4j.Slf4j;

import com.yifan.app_common.base.entity.UserInfo;
import com.yifan.app_common.common.HeaderInfo;

@Slf4j
public class UserContextFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        try {
         
            Optional.ofNullable(httpRequest.getHeader(HeaderInfo.USER_ID))
                    .map(Long::valueOf)
                    .ifPresent(uid -> {
                        String userName = httpRequest.getHeader(HeaderInfo.USER_NAME);
                        String role = httpRequest.getHeader(HeaderInfo.USER_ROLE);
                        UserContext.setUser(new UserInfo(uid, userName, role));
                    });

            chain.doFilter(request, response);
        } finally {          
            UserContext.clear();
        }
    }
}
