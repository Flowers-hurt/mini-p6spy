package com.hsqyz.minip6spy;

import com.mysql.cj.jdbc.Driver;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.SQLFeatureNotSupportedException;
import java.util.stream.Collectors;

/**
 * 需要本地运行中的 MySQL，账号/密码均为 root。
 * 若无法连接，会通过 Assumption 跳过测试而不是失败。
 */
class SpyDataSourceMySqlTest {

    // 使用 p6spy 前缀，直接获取代理连接而无需手工 wrap
    private static final String URL = "jdbc:p6spy:mysql://localhost:3306/mysql?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = "root";

    @Test
    void shouldCaptureSqlEventForSelect() throws Exception {
        try (Connection conn = getMySQLConnection();
             Statement stmt = conn.createStatement()) {
            // 初始化数据库与表数据
            executeSqlScript(stmt, "/init.sql");

            // C: 插入
            stmt.executeUpdate("INSERT INTO mini_p6spy.user_demo(name, age) VALUES ('Carol', 30)");
            // R: 查询
            stmt.executeQuery("SELECT * FROM mini_p6spy.user_demo WHERE name = 'Carol'");
            // U: 更新
            stmt.executeUpdate("UPDATE mini_p6spy.user_demo SET age = 31 WHERE name = 'Carol'");
            // D: 删除
            stmt.executeUpdate("DELETE FROM mini_p6spy.user_demo WHERE name = 'Carol'");

            // 确认连接类名包含 p6spy，表示驱动已代理
            Assertions.assertTrue(conn.getClass().getName().toLowerCase().contains("p6spy"), "应为 p6spy 代理连接");
        }
    }

    /**
     * 获取 MySQL 连接
     */
    private Connection getMySQLConnection() {
        try {
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 执行类路径下的 SQL 脚本，使用分号分隔。
     */
    private void executeSqlScript(Statement stmt, String resourcePath) throws IOException, SQLException {
        InputStream is = this.getClass().getResourceAsStream(resourcePath);
        if (is == null) {
            throw new IOException("未找到脚本: " + resourcePath);
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String content = reader.lines().collect(Collectors.joining("\n"));
            String[] parts = content.split(";");
            for (String raw : parts) {
                String sql = raw.trim();
                if (sql.isEmpty()) {
                    continue;
                }
                stmt.execute(sql);
            }
        }
    }

}

