package com.hsqyz.minip6spy.wrap;

import com.hsqyz.minip6spy.datasource.SpyDataSource;
import com.hsqyz.minip6spy.listener.P6SpySqlListener;
import com.hsqyz.minip6spy.listener.Slf4jSqlListener;
import com.hsqyz.minip6spy.listener.SqlListener;

import javax.sql.DataSource;

/**
 * 入口工具类，提供便捷的 DataSource 包装能力。
 * - wrap：默认挂载 SLF4J 监听器
 * - wrap(DataSource, listeners...)：自定义监听器组合
 * - wrapWithP6FormatConsole：仿 p6spy 控制台格式监听
 */
public final class MiniP6SpyWrap {

    private MiniP6SpyWrap() {
    }

    /**
     * 包装已有 DataSource，默认使用 SLF4J 监听。
     */
    public static DataSource wrap(DataSource delegate) {
        return wrap(delegate, new Slf4jSqlListener());
    }

    /**
     * 包装已有 DataSource，自定义监听器；可叠加多个监听器做不同用途。
     */
    public static DataSource wrap(DataSource delegate, SqlListener... listeners) {
        return SpyDataSource.wrap(delegate, listeners);
    }

    /**
     * 使用仿 p6spy 格式的控制台输出监听器，便于快速排查 SQL。
     */
    public static DataSource wrapWithP6FormatConsole(DataSource delegate) {
        return wrap(delegate, new P6SpySqlListener());
    }

}

