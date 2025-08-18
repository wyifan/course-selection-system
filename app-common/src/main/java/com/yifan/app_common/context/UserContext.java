package com.yifan.app_common.context;

import com.yifan.app_common.entity.UserInfo;

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
        USER_HOLDER.remove(); // 必须清理，防止内存泄漏
    }

}
