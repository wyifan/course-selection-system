package com.yifan.app_common.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.plugin.*;

import java.sql.Connection;
import java.util.Properties;

@Intercepts({
        @Signature(
                type = StatementHandler.class,
                method = "prepare",
                args = {Connection.class, Integer.class}
        )
})
@Slf4j
public class SqlDebugInterceptor implements Interceptor {
    // 这里可以添加SQL调试相关的逻辑
    // 例如，记录SQL执行时间、打印SQL语句等
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
        BoundSql boundSql = statementHandler.getBoundSql();

        String sql = boundSql.getSql();

        log.info("========== SQL Debug Interceptor Begin =========");
        log.info("SQL: {}", sql);  // 打印SQL语句
        log.info("========== SQL Debug Interceptor End =========");

        // 在这里可以添加拦截逻辑
        return invocation.proceed();
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        // 可以在这里设置一些属性
    }
}
