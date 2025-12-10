package com.hsqyz.minip6spy.datasource;

import com.hsqyz.minip6spy.handler.ConnectionInvocationHandler;
import com.hsqyz.minip6spy.listener.SqlListener;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * DataSource 装饰器，拦截 JDBC 调用并推送 SQL 事件。
 * 核心流程：
 * 1) 代理 Connection：拦截 createStatement / prepareStatement / prepareCall，返回被代理的 Statement；
 * 2) 代理 Statement：拦截 execute* / executeBatch / executeLarge* / addBatch，采集 SQL、耗时、批量大小、异常；
 * 3) 事件推送：将 SqlEvent 分发给监听器，监听器异常被吞掉不影响主流程。
 * 设计目标：最小侵入（包装 DataSource 即可）、可组合监听、多场景适配（日志、监控、告警）。
 */
public final class SpyDataSource implements DataSource {

    private final DataSource delegate; // 真实的数据源实现
    private final List<SqlListener> listeners; // 事件监听器集合

    private SpyDataSource(DataSource delegate, List<SqlListener> listeners) { // 构造函数，仅内部使用
        this.delegate = Objects.requireNonNull(delegate, "delegate DataSource"); // 校验并保存真实数据源
        this.listeners = java.util.Collections.unmodifiableList(new ArrayList<>(listeners)); // 监听器防御式拷贝并设为只读
    }

    /**
     * 包装已有 DataSource。
     *
     * @param delegate  真实数据源
     * @param listeners 可选监听器（可多选）
     * @return 代理 DataSource
     */
    public static DataSource wrap(DataSource delegate, SqlListener... listeners) { // 静态工厂，外部入口
        List<SqlListener> safe = listeners == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(listeners)); // 构建可变监听器列表
        return new SpyDataSource(delegate, safe); // 返回包装后的 DataSource
    }

    @Override
    public Connection getConnection() throws SQLException { // 无参获取连接
        return wrap(delegate.getConnection()); // 包装底层连接为代理连接
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException { // 账户密码获取连接
        return wrap(delegate.getConnection(username, password)); // 包装底层连接为代理连接
    }

    /**
     * 为 Connection 创建代理，只关心创建 Statement 的方法，其余方法透传。
     */
    private Connection wrap(Connection connection) { // 构造连接代理
        return (Connection) Proxy.newProxyInstance(
                connection.getClass().getClassLoader(), // 使用同一类加载器
                new Class[]{Connection.class}, // 代理的接口集合
                new ConnectionInvocationHandler(connection, listeners)); // 调用处理器
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException { // 解包到指定接口
        return delegate.unwrap(iface); // 委托真实数据源
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException { // 判断是否包装
        return delegate.isWrapperFor(iface); // 委托真实数据源
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException { // 获取日志输出器
        return delegate.getLogWriter(); // 委托真实数据源
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException { // 设置日志输出器
        delegate.setLogWriter(out); // 委托真实数据源
    }

    @Override
    public int getLoginTimeout() throws SQLException { // 获取登录超时
        return delegate.getLoginTimeout(); // 委托真实数据源
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException { // 设置登录超时
        delegate.setLoginTimeout(seconds); // 委托真实数据源
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException { // 获取父级日志器
        return delegate.getParentLogger(); // 委托真实数据源
    }

}

