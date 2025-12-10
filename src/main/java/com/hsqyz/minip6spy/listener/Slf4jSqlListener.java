package com.hsqyz.minip6spy.listener;

import com.hsqyz.minip6spy.event.SqlEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 基于 SLF4J 的默认监听器：
 * - 成功时 info 级别输出耗时、批量大小和 SQL
 * - 失败时 warn 级别，同时打印错误消息，便于快速定位
 * 适合生产环境直接落盘或接入统一日志体系。
 */
public final class Slf4jSqlListener implements SqlListener {
    // 使用固定 logger 名称，方便在日志配置中单独开关/定级
    private static final Logger log = LoggerFactory.getLogger("mini-p6spy");

    @Override
    public void onEvent(SqlEvent event) {
        if (event.isSuccess()) {
            log.info("[sql] {} ms | batch={} | {}", event.getElapsedMs(), event.getBatchSize(), event.getSql());
        } else {
            log.warn("[sql] {} ms | batch={} | {} | error={}",
                    event.getElapsedMs(),
                    event.getBatchSize(),
                    event.getSql(),
                    event.getError() != null ? event.getError().getMessage() : "<unknown>");
        }
    }

}

