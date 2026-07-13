# Backend Logging Guidelines

## 日志框架
Spring Boot 默认 Logback，通过 SLF4J 门面调用。

---

## 日志级别使用规范

| 级别 | 用途 | 示例 |
|------|------|------|
| ERROR | 系统异常、外部服务不可用、数据不一致 | DB 连接失败、Redis 宕机、企业微信 API 错误 |
| WARN | 业务异常、降级处理、可恢复的错误 | 参数校验失败、业务规则不满足、重试成功 |
| INFO | 关键业务流程节点、接口调用记录 | 用户登录、订单创建、定时任务执行 |
| DEBUG | 开发调试信息（生产环境关闭） | 方法入参出参、SQL 执行详情 |

---

## 必须记录的日志

```java
// 1. 所有 Controller 入口 — 用 AOP 统一记录
@Aspect
@Component
public class ApiLogAspect {
    @Around("execution(* com.wecorp.controller..*.*(..))")
    public Object logApi(ProceedingJoinPoint pjp) {
        log.info(">>> {} {} | params: {}", request.getMethod(),
                 request.getRequestURI(), pjp.getArgs());
        Object result = pjp.proceed();
        log.info("<<< {} {} | cost: {}ms", request.getMethod(),
                 request.getRequestURI(), cost);
        return result;
    }
}

// 2. 企业微信回调入口
log.info("收到企业微信回调: event={}, userId={}", event, userId);

// 3. 外部 API 调用（WxJava SDK）
log.info("调用企业微信API: method={}, response={}", method, response);

// 4. 异常捕获
log.error("处理企业微信消息失败, msgId={}", msgId, e);
```

---

## 日志格式

```xml
<!-- logback-spring.xml -->
<pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
```

---

## 规则

1. **禁止 `System.out.println`** — 必须使用 SLF4J Logger
2. **禁止日志中包含敏感信息** — 密码、token、手机号需脱敏
3. **生产环境 INFO 级别**，开发环境 DEBUG 级别
4. **日志异步写入** — 使用 `AsyncAppender` 避免阻塞业务线程
5. **多实例部署** — 日志文件名包含实例标识（通过 Spring profile 或 hostname 区分）
