package com.hsqyz.minip6spy.handler;

import com.hsqyz.minip6spy.enums.SqlMethodEnum;
import com.hsqyz.minip6spy.listener.SqlListener;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

/**
 * Connection 代理，拦截创建 Statement/PreparedStatement/CallableStatement。
 * 仅在创建语句对象时介入，其余方法全量透传。
 * 使用 {@link SqlMethodEnum} 避免硬编码字符串。
 */
public final class ConnectionInvocationHandler implements InvocationHandler {

    private final Connection delegate;
    private final List<SqlListener> listeners;

    public ConnectionInvocationHandler(Connection delegate, List<SqlListener> listeners) {
        this.delegate = delegate;
        this.listeners = listeners;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        return SqlMethodEnum.from(method.getName())
                .map(m -> handleKnownMethod(m, method, args))
                .orElseGet(() -> invokeDirect(method, args));
    }

    /**
     * 根据已支持的方法枚举分派逻辑：
     * - createStatement：包装为代理 Statement
     * - prepareStatement / prepareCall：记录 SQL 模板再包装
     * - 其它：直接透传
     */
    private Object handleKnownMethod(SqlMethodEnum m, Method method, Object[] args) {
        switch (m) {
            case CREATE_STATEMENT: {
                Statement stmt = invokeDirect(method, args);
                return wrapStatement(stmt, null);
            }
            case PREPARE_STATEMENT:
            case PREPARE_CALL: {
                String sql = args != null && args.length > 0 && args[0] instanceof String
                        ? (String) args[0] : "<unknown>";
                Statement prepared = invokeDirect(method, args);
                return wrapStatement(prepared, sql);
            }
            default:
                return invokeDirect(method, args);
        }
    }

    /**
     * 直接透传底层 Connection 方法，封装受检异常。
     */
    @SuppressWarnings("unchecked")
    private <T> T invokeDirect(Method method, Object[] args) {
        try {
            return (T) method.invoke(delegate, args);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 根据实际 Statement 类型决定需要实现的接口集合，便于调用方继续向下转型。
     * 普通 Statement 不绑定 SQL，预编译/存储过程会携带原始 SQL 模板。
     */
    private Object wrapStatement(Statement stmt, String sql) {
        Class<?>[] interfaces;
        if (stmt instanceof CallableStatement) {
            interfaces = new Class[]{CallableStatement.class, PreparedStatement.class, Statement.class};
        } else if (stmt instanceof PreparedStatement) {
            interfaces = new Class[]{PreparedStatement.class, Statement.class};
        } else {
            interfaces = new Class[]{Statement.class};
        }
        return Proxy.newProxyInstance(
                stmt.getClass().getClassLoader(),
                interfaces,
                new StatementInvocationHandler(stmt, sql, listeners));
    }
}

