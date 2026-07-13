# Backend Directory Structure

## Tech Stack
- Java 17, Spring Boot 3.x, MyBatis-Plus, Maven, MySQL
- 单体架构，必须支持多实例部署

---

## Maven 模块布局

```
src/
├── main/
│   ├── java/com/wecorp/
│   │   ├── config/           # Spring @Configuration 类
│   │   │   ├── RedisConfig.java
│   │   │   ├── WxJavaConfig.java
│   │   │   ├── MybatisPlusConfig.java
│   │   │   └── WebMvcConfig.java
│   │   │
│   │   ├── controller/       # REST 控制器（薄层，委托给 service）
│   │   │   ├── admin/        # 后台管理接口
│   │   │   ├── api/          # 前台业务接口
│   │   │   └── wx/           # 企业微信回调接口
│   │   │
│   │   ├── service/          # 业务逻辑层
│   │   │   ├── impl/         # Service 实现类
│   │   │   └── dto/          # Service 层 DTO（输入/输出）
│   │   │
│   │   ├── mapper/           # MyBatis-Plus Mapper 接口
│   │   │   └── xml/          # MyBatis XML（仅复杂 SQL 使用）
│   │   │
│   │   ├── entity/           # 数据库实体类（@TableName）
│   │   │
│   │   ├── enums/            # 业务枚举
│   │   │
│   │   ├── common/           # 公共模块
│   │   │   ├── result/       # R<T> 统一响应封装
│   │   │   ├── exception/    # 自定义异常类
│   │   │   ├── constant/     # 业务常量
│   │   │   └── utils/        # 工具类（无 Spring 依赖）
│   │   │
│   │   ├── aspect/           # AOP 切面（日志、鉴权等）
│   │   │
│   │   ├── interceptor/      # HTTP 拦截器
│   │   │
│   │   └── listener/         # 应用事件监听器
│   │
│   └── resources/
│       ├── application.yml           # 主配置
│       ├── application-dev.yml       # 开发环境
│       ├── application-prod.yml      # 生产环境
│       ├── mapper/                   # MyBatis XML（与 mapper/java 结构一致）
│       └── db/migration/             # SQL migration 脚本
│
├── test/
│   └── java/com/wecorp/
│       ├── controller/       # Controller 测试（@WebMvcTest）
│       ├── service/          # Service 测试（@SpringBootTest）
│       └── mapper/           # Mapper 测试（@MybatisTest）
│
pom.xml
```

---

## 分层规则

1. **Controller 层禁止业务逻辑** — 只做参数校验、调用 service、返回 `R<T>`
2. **Service 层是核心** — 所有业务逻辑在 `service/impl` 中，接口定义在 `service/` 下
3. **Entity 类对应数据库表** — 使用 `@TableName` 注解，字段驼峰映射
4. **禁止跨层调用** — controller 不能直接调 mapper，service 不能调 controller
5. **DTO 职责单一** — 查询用 `QueryDTO`，新增/修改用 `SaveDTO`，返回用 `VO`

---

## 命名规范

| 类型 | 规范 | 示例 |
|------|------|------|
| Controller | `XxxController` | `WxUserController` |
| Service 接口 | `XxxService` | `CustomerService` |
| Service 实现 | `XxxServiceImpl` | `CustomerServiceImpl` |
| Mapper 接口 | `XxxMapper` | `CustomerMapper` |
| Entity | `Xxx`（单数） | `Customer` |
| DTO（输入） | `XxxSaveDTO` / `XxxQueryDTO` | `CustomerSaveDTO` |
| VO（输出） | `XxxVO` | `CustomerVO` |
| 枚举 | `XxxEnum` | `CustomerStatusEnum` |
