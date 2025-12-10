# 手把手：从 0 写一个 mini 版 p6spy（超级详细小白向）

> 你将从空目录开始，一步步写出一个能拦截 SQL 的 mini p6spy。每一步都写明“要做什么 / 为什么 / 验证方式”。

---

## 0. 你需要准备

- 已安装 JDK 8（`java -version` 验证）
- 已安装 Maven（`mvn -v` 验证）
- 本地 MySQL，账号/密码：root/root（可改，记得同步测试配置）
- Node 18+（用于查看本教程站点，可选）

验证 MySQL 可用：
```bash
mysql -uroot -proot -e "select 1"
```

---

## 1. 新建项目骨架

在空目录执行：
```bash
mvn -q -DgroupId=com.hsqyz -DartifactId=mini-p6spy -DarchetypeArtifactId=maven-archetype-quickstart -DinteractiveMode=false
```

进入目录后，替换 `pom.xml` 关键内容（Java 8 + 依赖）：
```xml
<properties>
  <maven.compiler.source>1.8</maven.compiler.source>
  <maven.compiler.target>1.8</maven.compiler.target>
  <junit.version>5.10.2</junit.version>
</properties>
<dependencies>
  <dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-api</artifactId>
    <version>2.0.13</version>
  </dependency>
  <dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <version>8.3.0</version>
  </dependency>
  <dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>${junit.version}</version>
    <scope>test</scope>
  </dependency>
  <dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-simple</artifactId>
    <version>2.0.13</version>
    <scope>test</scope>
  </dependency>
</dependencies>
```

验证能编译（即使还没代码）：
```bash
mvn -q -DskipTests compile
```

---

## 2. 设计核心模型：SQL 事件 + 监听器

**做什么**：定义一个不可变的 `SqlEvent`，记录 SQL 文本、耗时、开始时间、异常、批量大小；定义监听接口 `SqlListener`。

**文件**：`src/main/java/com/hsqyz/minip6spy/event/SqlEvent.java`

关键字段：
- `sql`：原始 SQL（预编译语句存模板，未知用 `<unknown>`）
- `elapsedMs`：耗时
- `startedAt`：开始时间戳
- `success`/`error`：成功标记与异常
- `batchSize`：批量大小（非 batch 为 1）

**监听接口**：`listener/SqlListener.java`
- `void onEvent(SqlEvent event);`
- 监听器异常应被吞掉，避免影响主流程。

**验证**：能成功 `mvn -q -DskipTests compile`。

---

## 3. 规范要拦截的方法

**做什么**：列举所有要拦截的 JDBC 方法，避免硬编码字符串。

**文件**：`enums/SqlMethodEnum.java`
- 包含：`createStatement` / `prepareStatement` / `prepareCall`
- 包含：`execute` / `executeQuery` / `executeUpdate` / `executeBatch` / `executeLargeUpdate` / `executeLargeBatch` / `addBatch`
- 提供 `from(name)` 解析、`isExecute()` 判断。

**验证**：编译通过。

---

## 4. 写拦截 Handler（Connection + Statement）

### 4.1 ConnectionInvocationHandler
**作用**：只拦截创建 Statement/PreparedStatement/CallableStatement 的方法，其他透传。

流程：
1) 判断方法名（用枚举）
2) 拦截 `createStatement` → 包装成代理 Statement（无绑定 SQL）
3) 拦截 `prepareStatement`/`prepareCall` → 读取 SQL 模板 → 包装成代理 Statement

**文件**：`handler/ConnectionInvocationHandler.java`

### 4.2 StatementInvocationHandler
**作用**：拦截 execute*/batch/addBatch，采集耗时、批量大小、异常，发送事件。

流程：
1) `addBatch`：只计数
2) execute* / batch：记录开始时间 → 调用真实方法 → finally 中构造 `SqlEvent` 并通知监听器 → 批量后重置计数
3) 监听器异常被吞掉

**文件**：`handler/StatementInvocationHandler.java`

**验证**：`mvn -q -DskipTests compile`

---

## 5. 数据源装饰器（可选手动 wrap）

**文件**：`datasource/SpyDataSource.java`
- 包装任意 `DataSource`，返回代理 Connection → 代理 Statement。
- 用防御式拷贝保存监听器。
- 设计为最小侵入：仅 wrap 一次即可。

**验证**：编译通过。

---

## 6. 自定义驱动前缀（免手动 wrap）

目标：支持 `jdbc:p6spy:mysql:`，自动代理。

**文件**：`driver/P6SpyDriver.java`

工作流：
1) `acceptsURL`：判断前缀
2) `connect`：剥离前缀 → 委托 `com.mysql.cj.jdbc.Driver` → 对 Connection 再包一层动态代理（挂 `P6SpySqlListener` 等）
3) 返回包装类 `P6ProxyConnection`（类名含 p6spy，方便断言）
4) `META-INF/services/java.sql.Driver` 声明类名，实现无显式 `Class.forName`

**验证**：编译通过。

---

## 7. 监听器实现

- `listener/Slf4jSqlListener`：info 输出成功，warn 输出错误。
- `listener/P6SpySqlListener`：控制台红色 ANSI，格式模仿官方 p6spy（耗时 + 时间 + SQL + 错误）。

---

## 8. 入口包装工具（可选）

- `wrap/MiniP6SpyWrap`：静态方法 `wrap(...)`、`wrapWithP6FormatConsole(...)`，适合需要手工 wrap 的场景（如已有 DataSource）。

---

## 9. MySQL 集成测试（含脚本）

**脚本**：`src/test/resources/init.sql`
- 创建库 `mini_p6spy`
- 建表 `user_demo`
- 插入两条初始数据

**测试**：`src/test/java/com/hsqyz/minip6spy/SpyDataSourceMySqlTest.java`
1) 使用 URL：`jdbc:p6spy:mysql://localhost:3306/mysql?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC`
2) 通过 DriverManager 获取连接（已自动代理）
3) 执行 init.sql
4) 顺序执行 CRUD
5) 断言连接类名包含 `p6spy`（确认被代理）

运行：
```bash
mvn test -Dtest=SpyDataSourceMySqlTest
```

---

## 10. 在你的业务里使用

### 方式一：直接用前缀（最简单）
```
jdbc:p6spy:mysql://host:3306/db?useSSL=false&serverTimezone=UTC
```
在 Spring Boot：
```properties
spring.datasource.url=jdbc:p6spy:mysql://localhost:3306/yourdb
spring.datasource.username=root
spring.datasource.password=root
```

### 方式二：手工 wrap
```java
DataSource wrapped = MiniP6SpyWrap.wrap(originalDataSource, new P6SpySqlListener());
```

---

## 11. 常见问题（FAQ）

1) **为什么不用 Class.forName？**  
已在 `META-INF/services/java.sql.Driver` 声明驱动，DriverManager 会自动加载。

2) **如何确认真的代理了？**  
在测试中检查 `conn.getClass().getName()` 是否包含 `p6spy`，或查看控制台/日志输出的 SQL。

3) **网络装依赖失败？**  
配置 npm/maven 代理后再执行 `npm install` / `mvn dependency:resolve`。

4) **只想看日志格式？**  
使用 `P6SpySqlListener`，控制台红色输出类似官方 p6spy。

---

## 12. 本教程文档站（可选）

在项目根目录：
```bash
cd docs
npm install
npm run docs:dev
```
浏览器访问提示的本地地址（默认 http://localhost:5173/ ）。

---

到这里，你已经从零完成了一个 mini 版 p6spy：既可用驱动前缀“即插即用”，也可对任意 DataSource 手动 wrap，满足 SQL 监控、日志、排障等需求。祝使用愉快！ 

