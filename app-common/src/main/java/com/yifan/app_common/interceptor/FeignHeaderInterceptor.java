package com.yifan.app_common.interceptor;

import org.springframework.stereotype.Component;

import com.yifan.app_common.base.entity.UserInfo;
import com.yifan.app_common.common.HeaderInfo;
import com.yifan.app_common.context.UserContext;

import feign.RequestInterceptor;
import feign.RequestTemplate;

@Component
public class FeignHeaderInterceptor implements RequestInterceptor {
    @Override
    public void apply(RequestTemplate template) {
        UserInfo user = UserContext.getUser();
        if (user != null) {
            template.header(HeaderInfo.USER_ID, user.getUserId().toString());
            template.header(HeaderInfo.USER_ROLE, user.getRole());
        }
    }
}
