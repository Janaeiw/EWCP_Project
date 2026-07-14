# Research: WxJava SDK weixin-java-cp 模块 — 朋友圈 API 与配置

- **Query**: WxJava SDK (weixin-java-cp) 的 Maven 依赖、WxCpService 配置、朋友圈 Moments API
- **Scope**: external (GitHub source code)
- **Date**: 2026-07-01

---

## 1. Maven 依赖

**最新版本**: `4.8.4.B` (来自 master 分支 pom.xml)

```xml
<dependency>
    <groupId>com.github.binarywang</groupId>
    <artifactId>weixin-java-cp</artifactId>
    <version>4.8.4.B</version>
</dependency>
```

> GitHub Releases 页面最新 tag 是 `v4.8.0` (2026-01-03)，但 master 分支已迭代到 `4.8.4.B`。建议用 SNAPSHOT 或等正式发布。

---

## 2. WxCpService 配置

### 配置存储接口

`WxCpConfigStorage` — 位于 `weixin-java-cp/src/main/java/me/chanjar/weixin/cp/config/WxCpConfigStorage.java`

### 默认实现

`WxCpDefaultConfigImpl` — 位于 `weixin-java-cp/src/main/java/me/chanjar/weixin/cp/config/impl/WxCpDefaultConfigImpl.java`

### 核心配置属性

| 属性 | 类型 | 说明 | 行号 (WxCpDefaultConfigImpl) |
|------|------|------|-----|
| `corpId` | `String` | 企业ID | L42, setter L252 |
| `corpSecret` | `String` | 应用Secret | L43, setter L266 |
| `token` | `String` | 回调Token | L44, setter L280 |
| `aesKey` | `String` | 回调EncodingAESKey | L45, setter L318 |
| `agentId` | `Integer` | 应用AgentId | L33, setter L332 |

### 配置代码模式

```java
WxCpDefaultConfigImpl config = new WxCpDefaultConfigImpl();
config.setCorpId("your-corp-id");
config.setCorpSecret("your-corp-secret");
config.setToken("your-callback-token");
config.setAesKey("your-aes-key");
config.setAgentId(1000112);  // 应用的agentId

// 然后注入到 WxCpService
WxCpService cpService = new WxCpServiceImpl();
cpService.setWxCpConfigStorage(config);
```

### 其他可用配置实现

| 类 | 用途 |
|----|------|
| `WxCpRedisConfigImpl` | Redis 存储 token/ticket |
| `WxCpJedisConfigImpl` | 基于 Jedis 的 Redis 存储 |
| `WxCpRedissonConfigImpl` | 基于 Redisson 的 Redis 存储 |
| `WxCpRedisTemplateConfigImpl` | Spring RedisTemplate 存储 |
| `WxCpTpDefaultConfigImpl` | 第三方应用 (ISV) 默认配置 |

---

## 3. 朋友圈 Moments API

朋友圈 API 不是独立 Service，而是集成在 `WxCpExternalContactService` 中。

**接口文件**: `weixin-java-cp/src/main/java/me/chanjar/weixin/cp/api/WxCpExternalContactService.java`

**实现文件**: `weixin-java-cp/src/main/java/me/chanjar/weixin/cp/api/impl/WxCpExternalContactServiceImpl.java`

### 3.1 API 方法清单

#### 创建朋友圈

```java
// 创建发表任务 (异步)
WxCpAddMomentResult addMomentTask(WxCpAddMomentTask task) throws WxErrorException;

// 获取创建任务结果 (轮询)
WxCpGetMomentTaskResult getMomentTaskResult(String jobId) throws WxErrorException;

// 取消/停止企业朋友圈
WxCpBaseResp cancelMomentTask(String momentId) throws WxErrorException;
```

#### 获取朋友圈列表

```java
// 获取企业全部的发表列表
WxCpGetMomentList getMomentList(
    Long startTime,    // Unix时间戳
    Long endTime,      // Unix时间戳
    String creator,    // 创建人userid (可选)
    Integer filterType, // 0:企业发表 1:个人发表 2:所有
    String cursor,     // 分页游标
    Integer limit      // 最大100, 默认100
) throws WxErrorException;

// 获取某个朋友圈的企业发表成员列表
WxCpGetMomentTask getMomentTask(
    String momentId,
    String cursor,
    Integer limit      // 最大1000, 默认500
) throws WxErrorException;
```

#### 获取朋友圈详情

```java
// 获取朋友圈可见范围（选择了哪些客户）
WxCpGetMomentCustomerList getMomentCustomerList(
    String momentId,
    String userId,     // 企业发表成员userid
    String cursor,
    Integer limit      // 最大1000, 默认500
) throws WxErrorException;

// 获取朋友圈发表后的可见客户列表
WxCpGetMomentSendResult getMomentSendResult(
    String momentId,
    String userId,
    String cursor,
    Integer limit      // 最大5000, 默认3000
) throws WxErrorException;

// 获取朋友圈互动数据（评论+点赞）
WxCpGetMomentComments getMomentComments(
    String momentId,
    String userId
) throws WxErrorException;
```

---

## 4. 朋友圈数据类

### 顶层 Bean（位于 `bean/external/`）

| 类名 | 用途 | 关键字段 |
|------|------|----------|
| `WxCpAddMomentTask` | 创建朋友圈请求体 | `visibleRange`, `text`, `attachments` |
| `WxCpAddMomentResult` | 创建任务返回 | `jobId` |
| `WxCpGetMomentTaskResult` | 任务执行结果 | `status`, `type`, `result.momentId`, `invalidSenderList`, `invalidExternalContactList` |
| `WxCpGetMomentList` | 朋友圈列表 | `nextCursor`, `momentList` (List<MomentInfo>) |
| `WxCpGetMomentTask` | 企业发表成员列表 | `nextCursor`, `taskList` (List<MomentTaskItem>) |
| `WxCpGetMomentCustomerList` | 可见客户列表 | `nextCursor`, `customerList` (List<CustomerItem>) |
| `WxCpGetMomentSendResult` | 已发送客户列表 | `nextCursor`, `customerList` (List<CustomerItem>) |
| `WxCpGetMomentComments` | 互动数据 | `commentList`, `likeList` (List<CommentLikeItem>) |

### 子包 Bean（位于 `bean/external/moment/`）

| 类名 | 用途 | 关键字段 |
|------|------|----------|
| `MomentInfo` | 朋友圈详情 | `momentId`, `creator`, `createTime`, `createType`, `visibleType`, `text`, `image`, `video`, `link`, `location` |
| `VisibleRange` | 可见范围 | `senderList`, `externalContactList` |
| `SenderList` | 发送者列表 | `userList` (List<String>), `departmentList` (List<String>) |
| `ExternalContactList` | 外部联系人标签 | `tagList` (List<String>) |
| `CustomerItem` | 客户项 | `externalUserId`, `userId` |

### 附件类型（位于 `bean/external/msg/`）

`WxCpAddMomentTask.attachments` 使用 `Attachment` 类，支持以下 msgType:

- `image` — 图片（需要先上传素材获取 media_id）
- `link` — 图文链接
- `video` — 视频
- `miniprogram` — 小程序
- `file` — 文件

`Attachment` 提供链式 setter: `setImage()`, `setLink()`, `setVideo()`, `setMiniProgram()`, `setFile()`

### WxCpAddMomentTask 完整结构

```json
{
  "visible_range": {
    "sender_list": {
      "user_list": ["user1", "user2"],
      "department_list": ["dept1"]
    },
    "external_contact_list": {
      "tag_list": ["tag1", "tag2"]
    }
  },
  "text": {
    "content": "朋友圈文字内容"
  },
  "attachments": [
    {
      "msgtype": "image",
      "image": {
        "media_id": "MEDIA_ID"
      }
    }
  ]
}
```

---

## 5. 企业微信官方 API 文档链接

创建发表任务: https://open.work.weixin.qq.com/api/doc/90000/90135/95094

获取发表列表: https://open.work.weixin.qq.com/api/doc/90000/90135/93333

获取互动数据: https://developer.work.weixin.qq.com/document/path/97612

---

## 文件路径汇总

```
weixin-java-cp/src/main/java/me/chanjar/weixin/cp/
├── api/
│   ├── WxCpExternalContactService.java          # 朋友圈 API 方法定义
│   └── impl/
│       └── WxCpExternalContactServiceImpl.java   # 朋友圈 API 实现
├── bean/external/
│   ├── WxCpAddMomentTask.java                    # 创建朋友圈请求
│   ├── WxCpAddMomentResult.java                  # 创建结果 (jobId)
│   ├── WxCpGetMomentList.java                    # 朋友圈列表
│   ├── WxCpGetMomentTask.java                    # 企业发表成员列表
│   ├── WxCpGetMomentTaskResult.java              # 任务执行结果
│   ├── WxCpGetMomentCustomerList.java            # 可见客户列表
│   ├── WxCpGetMomentSendResult.java              # 发送客户列表
│   ├── WxCpGetMomentComments.java                # 评论和点赞
│   ├── moment/
│   │   ├── MomentInfo.java                       # 朋友圈详情
│   │   ├── VisibleRange.java                     # 可见范围
│   │   ├── SenderList.java                       # 发送者列表
│   │   ├── ExternalContactList.java              # 外部联系人标签
│   │   └── CustomerItem.java                     # 客户项
│   └── msg/
│       └── Attachment.java                       # 附件类型
├── config/
│   ├── WxCpConfigStorage.java                    # 配置接口
│   └── impl/
│       └── WxCpDefaultConfigImpl.java            # 默认配置实现
```

---

## Caveats

1. 朋友圈 API 在 `WxCpExternalContactService` 中，不存在独立的 `WxCpMomentService`
2. 创建朋友圈是**异步操作**：`addMomentTask()` 返回 `jobId`，需轮询 `getMomentTaskResult(jobId)` 获取结果
3. `getMomentComments` 只接受 `momentId` + `userId`，不支持分页
4. 版本号 `4.8.4.B` 来自 master 分支，可能尚未发布到 Maven Central；稳定版本是 `4.8.0`
