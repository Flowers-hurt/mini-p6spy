package com.hsqyz.minip6spy.event;

import java.time.Instant;

/**
 * 不可变的 SQL 执行事件载体，便于监听器消费。
 * 记录 SQL 文本、耗时、开始时间、成功状态、异常与批量大小等关键信息。
 */
public final class SqlEvent {
    // 原始 SQL；PreparedStatement 记录预编译 SQL，未知时使用占位符
    private final String sql;
    // 执行耗时（毫秒）
    private final long elapsedMs;
    // 执行开始时间戳
    private final Instant startedAt;
    // 是否执行成功
    private final boolean success;
    // 执行异常（成功时为 null）
    private final Throwable error;
    // 批量执行时的条数，普通执行为 1
    private final int batchSize;

    private SqlEvent(Builder builder) {
        this.sql = builder.sql;
        this.elapsedMs = builder.elapsedMs;
        this.startedAt = builder.startedAt;
        this.success = builder.success;
        this.error = builder.error;
        this.batchSize = builder.batchSize;
    }

    /**
     * 创建事件构建器。
     *
     * @param sql 绑定的 SQL 文本，可为 null（会使用占位符）
     */
    public static Builder builder(String sql) {
        return new Builder(sql);
    }

    /**
     * @return 原始 SQL 文本（预编译语句为模板；未知时为占位符）
     */
    public String getSql() {
        return sql;
    }

    /**
     * @return 耗时（毫秒）
     */
    public long getElapsedMs() {
        return elapsedMs;
    }

    /**
     * @return 执行开始时间戳
     */
    public Instant getStartedAt() {
        return startedAt;
    }

    /**
     * @return 是否执行成功（监听器设置 error 时会标记为 false）
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * @return 执行期间的异常，无异常返回 null
     */
    public Throwable getError() {
        return error;
    }

    /**
     * @return 批量大小；非批量场景为 1
     */
    public int getBatchSize() {
        return batchSize;
    }

    public static final class Builder {
        private final String sql;
        private long elapsedMs;
        private Instant startedAt = Instant.now();
        private boolean success = true;
        private Throwable error;
        private int batchSize = 1;

        private Builder(String sql) {
            // 避免空指针，缺省使用 <unknown> 占位
            this.sql = sql == null ? "<unknown>" : sql;
        }

        /**
         * 设置耗时（毫秒）。
         */
        public Builder elapsedMs(long elapsedMs) {
            this.elapsedMs = elapsedMs;
            return this;
        }

        /**
         * 设置开始时间戳。
         */
        public Builder startedAt(Instant startedAt) {
            this.startedAt = startedAt;
            return this;
        }

        /**
         * 设置成功标记。
         */
        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        /**
         * 设置异常并自动将 success 置为 false（仅当异常非空）。
         */
        public Builder error(Throwable error) {
            this.error = error;
            // 仅当存在异常时标记失败，避免将 null 误判为失败
            if (error != null) {
                this.success = false;
            }
            return this;
        }

        /**
         * 设置批量大小。
         */
        public Builder batchSize(int batchSize) {
            this.batchSize = batchSize;
            return this;
        }

        /**
         * 构造不可变事件对象。
         */
        public SqlEvent build() {
            return new SqlEvent(this);
        }
    }

}

