package com.hsqyz.minip6spy.handler;

import com.hsqyz.minip6spy.enums.SqlMethodEnum;
import com.hsqyz.minip6spy.event.SqlEvent;
import com.hsqyz.minip6spy.listener.SqlListener;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Statement 代理，拦截 execute* / addBatch 等方法采集信息。
 * - 通过 SqlMethodEnum 统一方法名，覆盖 execute/executeQuery/executeUpdate/executeBatch/executeLarge*。
 * - addBatch 仅计数，方便在 executeBatch 时计算批量大小。
 * - 其余未在枚举内的方法直接透传。
 */
public final class StatementInvocationHandler implements InvocationHandler {
    private final Statement delegate;
    private final String boundSql; // null 表示普通 Statement，需要从 execute 参数中拿 SQL
    private final List<SqlListener> listeners;
    private int batchCount = 0;    // addBatch 调用计数，用于生成批量大小

    public StatementInvocationHandler(Statement delegate, String boundSql, List<SqlListener> listeners) {
        this.delegate = delegate;
        this.boundSql = boundSql;
        this.listeners = listeners;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        return SqlMethodEnum.from(method.getName())
                .map(m -> handleKnownMethod(m, method, args))
                .orElseGet(() -> invokeDirect(method, args));
    }

    /**
     * 针对已支持方法的分派：
     * - ADD_BATCH：计数 + 透传
     * - EXECUTE*：采集耗时/SQL/异常并回调监听器
     * - 其他：直接透传
     */
    private Object handleKnownMethod(SqlMethodEnum m, Method method, Object[] args) {
        switch (m) {
            case ADD_BATCH:
                batchCount++;
                return invokeDirect(method, args);
            case EXECUTE:
            case EXECUTE_QUERY:
            case EXECUTE_UPDATE:
            case EXECUTE_BATCH:
            case EXECUTE_LARGE_UPDATE:
            case EXECUTE_LARGE_BATCH:
                return executeAndCapture(m, method, args);
            default:
                return invokeDirect(method, args);
        }
    }

    /**
     * 执行 execute* 方法并采集事件。
     *
     * @param method        当前枚举方法
     * @param reflectMethod 反射方法
     * @param args          调用参数
     * @return 方法执行结果
     */
    private Object executeAndCapture(SqlMethodEnum method, Method reflectMethod, Object[] args) {
        String sql = resolveSql(args);
        Instant start = Instant.now();
        boolean success = false;
        Throwable err = null;
        try {
            Object result = reflectMethod.invoke(delegate, args);
            success = true;
            return result;
        } catch (InvocationTargetException e) {
            err = e.getTargetException();
            throw wrapIfNeeded(err);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            long elapsed = Duration.between(start, Instant.now()).toMillis();
            int size = method == SqlMethodEnum.EXECUTE_BATCH || method == SqlMethodEnum.EXECUTE_LARGE_BATCH
                    ? Math.max(batchCount, 1) : 1;
            SqlEvent event = SqlEvent.builder(sql)
                    .startedAt(start)
                    .elapsedMs(elapsed)
                    .batchSize(size)
                    .success(success)
                    .error(err)
                    .build();
            notifyListeners(event);
            if (method == SqlMethodEnum.EXECUTE_BATCH || method == SqlMethodEnum.EXECUTE_LARGE_BATCH) {
                batchCount = 0;
            }
        }
    }

    /**
     * 安全通知监听器，吞掉监听器内部的运行时异常。
     */
    private void notifyListeners(SqlEvent event) {
        for (SqlListener listener : listeners) {
            try {
                listener.onEvent(event);
            } catch (RuntimeException ignore) {
                // 监听器异常不影响 JDBC 调用，避免阻塞业务
            }
        }
    }

    /**
     * 从参数或预编译模板提取 SQL；普通 Statement 在 execute*(sql) 时取第 1 个字符串参数。
     */
    /**
     * 提取 SQL：优先使用预编译模板，其次使用 execute*(sql) 的首个字符串参数。
     */
    private String resolveSql(Object[] args) {
        if (boundSql != null) {
            return boundSql;
        }
        if (args != null && args.length > 0 && args[0] instanceof String) {
            return (String) args[0];
        }
        return "<unknown>";
    }

    @SuppressWarnings("unchecked")
    /**
     * 直接透传底层 Statement 方法，封装受检异常。
     */
    private <T> T invokeDirect(Method method, Object[] args) {
        try {
            return (T) method.invoke(delegate, args);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 统一将受检异常包装为 RuntimeException，便于上层流程处理。
     */
    /**
     * 统一包装异常为 RuntimeException，便于上层处理。
     */
    private RuntimeException wrapIfNeeded(Throwable err) {
        if (err instanceof RuntimeException) {
            return (RuntimeException) err;
        }
        return new RuntimeException(err);
    }
}

