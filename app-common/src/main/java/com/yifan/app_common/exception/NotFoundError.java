package com.yifan.app_common.exception;

public class NotFoundError extends BaseException {
    public NotFoundError(String message) {
        super(404, message);
    }
}
