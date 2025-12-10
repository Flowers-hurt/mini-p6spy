# mini-p6spy

轻量版 p6spy，支持 `jdbc:p6spy:mysql:` 前缀即插即用，也可对任意 DataSource 手动 wrap。内置 SQL 事件模型、监听接口、SLF4J/仿 p6spy 输出，多监听器可叠加，默认吞掉监听异常保障主流程。附 MySQL CRUD 集成测试与从零实现的详细文档（VitePress）。

## 功能特性
- JDBC 前缀代理：`jdbc:p6spy:mysql:` 自动剥离并代理，无需手工包装。
- 数据源包装：`SpyDataSource` 可手动 wrap 任意 DataSource。
- 监听扩展：`SqlListener` 接口，可多监听器叠加；内置 `Slf4jSqlListener`、`P6SpySqlListener`。
- 事件模型：`SqlEvent` 记录 SQL、耗时、时间、异常、批量大小。
- 方法枚举：`SqlMethodEnum` 统一管理 execute*/batch/addBatch 等拦截点。
- 测试样例：MySQL CRUD 集成测试，附初始化脚本 `init.sql`。
- 文档站点：VitePress 编写的从 0 手搓教程（`docs/index.md`）。

## 环境要求
- JDK 8+
- Maven 3.6+
- MySQL（默认账号/密码 root/root，库名 `mini_p6spy`，可改）
- （可选）Node 18+ 用于本地预览文档

## 快速开始

### 1) 使用自定义前缀（最简）
```properties
jdbc:p6spy:mysql://localhost:3306/yourdb?useSSL=false&serverTimezone=UTC
```
在 Spring Boot：
```properties
spring.datasource.url=jdbc:p6spy:mysql://localhost:3306/yourdb
spring.datasource.username=root
spring.datasource.password=root
```

### 2) 手动 wrap 现有 DataSource
```java
DataSource wrapped = MiniP6SpyWrap.wrap(originalDataSource, new P6SpySqlListener());
```

### 3) 运行集成测试（需本地 MySQL）
```bash
mvn test -Dtest=SpyDataSourceMySqlTest
```
测试流程：执行 `init.sql` 初始化库表 → 依次执行 CRUD → 断言连接已被代理。

## 核心模块结构
- `event/SqlEvent`：SQL 事件模型（SQL、耗时、时间、异常、批量大小）
- `listener/*`：监听接口与默认实现（SLF4J、仿 p6spy 控制台）
- `enums/SqlMethodEnum`：拦截方法枚举（create/prepare/execute*/batch/addBatch）
- `handler/*`：Connection/Statement 动态代理拦截逻辑
- `datasource/SpyDataSource`：手动包装 DataSource 的装饰器
- `driver/P6SpyDriver`：自定义 JDBC 驱动前缀代理，自动注册（SPI）
- `wrap/MiniP6SpyWrap`：静态入口（可选）
- `docs/`：VitePress 文档站，含超详细从零教程

## 文档站点（可选）
```bash
cd docs
npm install
npm run docs:dev
```
打开终端提示的本地地址（默认 http://localhost:5173/ ）。

## 开发生命令
```bash
mvn -q -DskipTests compile   # 编译
mvn test -Dtest=SpyDataSourceMySqlTest   # 运行集成测试
```

## 常见问题
- **为何无需 Class.forName？** 已在 `META-INF/services/java.sql.Driver` 声明驱动，DriverManager 会自动加载。
- **如何确认真的代理？** 检查连接类名是否包含 `p6spy`，或看控制台/日志输出的 SQL。
- **日志样式？** 控制台仿 p6spy 样式用 `P6SpySqlListener`，生产日志用 `Slf4jSqlListener`。
- **网络装依赖失败？** 配置 npm/maven 代理后重试。

## 许可证
MIT

