package com.yifan.app_common.context;

import com.yifan.app_common.base.entity.UserInfo;

public class UserContext {
    private static final ThreadLocal<UserInfo> USER_HOLDER = new ThreadLocal<>();

    // Private constructor to prevent instantiation
    private UserContext() {
    }

    public static void setUser(UserInfo userInfo) {
        USER_HOLDER.set(userInfo);
    }

    public static UserInfo getUser() {
        return USER_HOLDER.get();
    }

    public static void clear() {
        USER_HOLDER.remove(); 
    }

}
