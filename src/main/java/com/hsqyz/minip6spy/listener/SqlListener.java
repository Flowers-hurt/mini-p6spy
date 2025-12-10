package com.hsqyz.minip6spy.listener;

import com.hsqyz.minip6spy.event.SqlEvent;

/**
 * SQL 执行后的回调接口。
 * 调用时机：每次 execute* 或 executeBatch 完成（无论成功或异常）。
 * 场景示例：日志输出、链路埋点、慢查询告警、指标采集等。
 */
@FunctionalInterface
public interface SqlListener {
    /**
     * 处理一条 SQL 事件，方法内部抛出的异常不会影响 JDBC 主流程。
     */
    void onEvent(SqlEvent event);
}

