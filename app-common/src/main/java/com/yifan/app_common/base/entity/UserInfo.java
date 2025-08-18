package com.yifan.app_common.base.entity;

public class UserInfo {
    private Long userId;
    private String role;
    private String userName;

    public UserInfo(Long userId,String userName, String role) {
        this.userId = userId;
        this.userName = userName;
        this.role = role;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public String getRole() {
        return role;
    }
}
