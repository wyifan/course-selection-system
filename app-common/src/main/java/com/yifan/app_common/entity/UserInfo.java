package com.yifan.app_common.entity;

public class UserInfo {
    private String userId;
    private String role;

    public UserInfo(String userId, String role) {
        this.userId = userId;
        this.role = role;
    }

    public String getUserId() {
        return userId;
    }

    public String getRole() {
        return role;
    }
}
