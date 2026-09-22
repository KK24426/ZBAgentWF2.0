/*
 * 创建日期：2026-09-22
 * 更新日期：2026-09-22
 * 做 成 者：zebiao
 * 版    本：v0.1
 * 功能概要：记录 SQL 操作标识、耗时和失败，不记录 SQL 文本或参数。
 */
package com.kk24426.zbagentwf.agent.persistence;

import java.sql.SQLException;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.ExceptionUtil;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 当前只覆盖框架执行入口，后续真实业务仍需记录业务步骤和已批准标识。 */
@Intercepts({
    @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
    @Signature(type = Executor.class, method = "query",
            args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
    @Signature(type = Executor.class, method = "query",
            args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class}),
    @Signature(type = Executor.class, method = "queryCursor",
            args = {MappedStatement.class, Object.class, RowBounds.class})
})
public class SqlDiagnosticsInterceptor implements Interceptor {
    private static final Logger LOG = LoggerFactory.getLogger(SqlDiagnosticsInterceptor.class);

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        MappedStatement statement = (MappedStatement) invocation.getArgs()[0];
        long start = System.nanoTime();
        boolean success = false;
        try {
            Object result = invocation.proceed();
            success = true;
            return result;
        } catch (Throwable failure) {
            Throwable cause = ExceptionUtil.unwrapThrowable(failure);
            // 异常完整链由统一 encoder 脱敏；不打印 parameterObject 和 BoundSql。
            if (cause instanceof SQLException sql) {
                LOG.error("数据库失败 statement={} sqlState={} errorCode={}",
                        statement.getId(), sql.getSQLState(), sql.getErrorCode(), cause);
            } else {
                LOG.error("数据库失败 statement={}", statement.getId(), cause);
            }
            throw cause;
        } finally {
            LOG.debug("数据库操作 statement={} operation={} success={} elapsedMs={}",
                    statement.getId(), statement.getSqlCommandType(), success,
                    (System.nanoTime() - start) / 1_000_000);
        }
    }
}
