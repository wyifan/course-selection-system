package com.yifan.api_client.interceptor;

import org.springframework.stereotype.Component;

import com.yifan.app_common.common.HeaderInfo;
import com.yifan.app_common.context.UserContext;
import com.yifan.app_common.entity.UserInfo;

import feign.RequestInterceptor;
import feign.RequestTemplate;

@Component 
// @Component to enable this interceptor 但是考虑到有些服务可能不需要传递用户信息，可以选择性配置，不在common中进行侵入式的注册
// 使用的地方需要自行在配置类中声明该 Bean，并设置顺序
public class FeignHeaderInterceptor implements RequestInterceptor {
    @Override
    public void apply(RequestTemplate template) {
        UserInfo user = UserContext.getUser();
        if (user != null) {
            template.header(HeaderInfo.USER_ID, user.getUserId());
            template.header(HeaderInfo.USER_ROLE, user.getRole());
        }
    }
}


