package com.yifan.app_common.handler;

import java.time.LocalDateTime;
import java.util.Optional;

import org.apache.ibatis.reflection.MetaObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.yifan.app_common.common.EntityConstants;
import com.yifan.app_common.context.UserContext;

@Component
public class BaseMetaObjectHandler implements MetaObjectHandler {

    @Value("${app.default.user-id:0L}") 
    private Long defaultUserId;

    @Value("${app.default.user-name:admin}")
    private String defaultUserName;

    @Override
    public void insertFill(MetaObject metaObject) {
        Optional.ofNullable(UserContext.getUser())
                .ifPresent(u -> {
                    defaultUserId = u.getUserId();
                    defaultUserName = u.getUserName(); 
                });

        this.strictInsertFill(metaObject, EntityConstants.CREATED_BY, Long.class, defaultUserId);
        this.strictInsertFill(metaObject, EntityConstants.CREATED_BY_NAME, String.class, defaultUserName);
        this.strictInsertFill(metaObject, EntityConstants.CREATED_TIME, LocalDateTime.class, LocalDateTime.now());

        this.strictInsertFill(metaObject, EntityConstants.UPDATED_BY, Long.class, defaultUserId);
        this.strictInsertFill(metaObject, EntityConstants.UPDATED_BY_NAME, String.class, defaultUserName);
        this.strictInsertFill(metaObject, EntityConstants.UPDATED_TIME, LocalDateTime.class, LocalDateTime.now());

        this.strictInsertFill(metaObject, EntityConstants.VERSION, Integer.class, 1);

        this.strictInsertFill(metaObject, EntityConstants.IS_DELETED, Integer.class, 0);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        Optional.ofNullable(UserContext.getUser())
                .ifPresent(u -> {
                    defaultUserId = u.getUserId();
                    defaultUserName = u.getRole();
                });

        this.strictUpdateFill(metaObject, EntityConstants.UPDATED_BY, Long.class, defaultUserId);
        this.strictUpdateFill(metaObject, EntityConstants.UPDATED_BY_NAME, String.class, defaultUserName);
        this.strictUpdateFill(metaObject, EntityConstants.UPDATED_TIME, LocalDateTime.class, LocalDateTime.now());
    }

}
