package com.hsqyz.minip6spy.listener;

import com.hsqyz.minip6spy.event.SqlEvent;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

/**
 * 仿照 MyBatis-Plus 提供的 P6Spy 日志格式实现的监听器。
 * 格式示例：
 * Consume Time：12 ms 2024-12-10 20:00:00.123
 * Execute SQL：select * from user where id = 1
 * 失败时追加 Error 信息；输出为红色 ANSI 以便在控制台突出显示。
 */
public final class P6SpySqlListener implements SqlListener {

    // 合并多余空白，便于一行展示 SQL
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]+");
    // 本地时区的可读时间格式
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneId.systemDefault());
    // 控制台红色输出（ANSI），不支持的终端会原样展示
    private static final String RED = "\u001B[31m";
    private static final String RESET = "\u001B[0m";

    @Override
    public void onEvent(SqlEvent event) {
        String sql = event.getSql();
        if (sql == null || sql.trim().isEmpty()) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(" Consume Time：")
                .append(event.getElapsedMs())
                .append(" ms ")
                .append(formatInstant(event.getStartedAt()));
        sb.append("\n Execute SQL：")
                .append(WHITESPACE.matcher(sql).replaceAll(" "));
        if (!event.isSuccess() && event.getError() != null) {
            sb.append("\n Error：").append(event.getError().getMessage());
        }
        System.out.println(RED + sb + RESET);
    }

    private String formatInstant(Instant instant) {
        if (instant == null) {
            return "<unknown-time>";
        }
        return FORMATTER.format(instant);
    }
}

