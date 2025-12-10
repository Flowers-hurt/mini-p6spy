package com.hsqyz.minip6spy.enums;

import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 统一管理需要拦截的 JDBC 方法，避免散落的字符串字面量：
 * - 覆盖 Connection 上创建 Statement/PreparedStatement/CallableStatement 的入口
 * - 覆盖 Statement 上所有 execute* / executeBatch / executeLarge* 以及 addBatch
 * - 提供 from/name/isExecute，方便 handler 使用
 */
public enum SqlMethodEnum {
    CREATE_STATEMENT("createStatement"),
    PREPARE_STATEMENT("prepareStatement"),
    PREPARE_CALL("prepareCall"),

    ADD_BATCH("addBatch"),

    EXECUTE("execute"),
    EXECUTE_QUERY("executeQuery"),
    EXECUTE_UPDATE("executeUpdate"),
    EXECUTE_BATCH("executeBatch"),
    EXECUTE_LARGE_UPDATE("executeLargeUpdate"),
    EXECUTE_LARGE_BATCH("executeLargeBatch");

    private static final Map<String, SqlMethodEnum> BY_NAME =
            EnumSet.allOf(SqlMethodEnum.class).stream()
                    .collect(Collectors.toMap(SqlMethodEnum::methodName, Function.identity()));

    private final String methodName;

    SqlMethodEnum(String methodName) {
        this.methodName = methodName;
    }

    /**
     * 由方法名解析枚举。
     *
     * @param name 反射得到的方法名
     * @return 对应枚举；未覆盖的方法返回 empty
     */
    public static Optional<SqlMethodEnum> from(String name) {
        return Optional.ofNullable(BY_NAME.get(name));
    }

    /**
     * @return 原始方法名字符串（用于反射比对）
     */
    public String methodName() {
        return methodName;
    }

    /**
     * @return 是否属于 execute* 或批量执行方法
     */
    public boolean isExecute() {
        return this == EXECUTE
                || this == EXECUTE_QUERY
                || this == EXECUTE_UPDATE
                || this == EXECUTE_BATCH
                || this == EXECUTE_LARGE_UPDATE
                || this == EXECUTE_LARGE_BATCH;
    }

}

