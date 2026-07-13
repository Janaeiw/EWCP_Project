# 内容库重构 — PRD

## 背景
当前内容库实现中，弹窗的字段对所有类型是统一的（标题、摘要、封面URL、正文textarea）。用户要求按类型区分字段，并支持图片上传到数据库。

## 数据模型变更

### t_content 表
新增 `link` VARCHAR(500) 字段（推文链接，仅推文类型使用）。

### 新增 t_image 表
```sql
CREATE TABLE IF NOT EXISTS `t_image` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `name`        VARCHAR(255) NOT NULL COMMENT '文件名',
    `type`        VARCHAR(64)  DEFAULT NULL COMMENT 'MIME类型',
    `data`        LONGBLOB     NOT NULL COMMENT '图片二进制数据',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图片存储表';
```

图片访问接口：`GET /api/library/image/{id}` — 返回二进制图片，设置正确的 Content-Type。

## 前端弹窗 — 按类型区分字段

### 推文（post）
| 字段 | 控件 | 必填 | 说明 |
|------|------|------|------|
| 推文链接 | el-input | 是 | URL校验 |
| 推文标题 | el-input | 是 | |
| 推文描述 | el-input textarea | 否 | |
| 推文封面 | 图片上传组件 | 否 | 支持文件上传或粘贴图片链接 |

### 文章（article）
| 字段 | 控件 | 必填 | 说明 |
|------|------|------|------|
| 文章标题 | el-input | 是 | |
| 文章描述 | el-input textarea | 否 | |
| 文章封面 | 图片上传组件 | 否 | 仅文件上传 |
| 正文 | 富文本编辑器 | 否 | wangeditor |

### 通用字段
- 状态：上架/下架（el-radio）

## 图片上传组件
- 使用 el-upload，action 指向 `/api/library/image/upload`（multipart/form-data）
- 上传成功后返回 `{ id, url }`，url 格式为 `/api/library/image/{id}`
- 推文封面支持两种模式：文件上传 + 输入链接（el-input）
- 文章封面仅文件上传
- 封面显示：优先用上传的图片URL，否则用输入的链接

## 富文本编辑器
- 使用 `@wangeditor/editor-for-vue`（WangEditor 5 Vue 版）
- 仅文章类型显示
- 工具栏精简：标题、加粗、斜体、列表、链接、图片上传（走同一上传接口）

## 后端接口

### 图片上传
`POST /api/library/image/upload`
- 参数：`MultipartFile file`
- 返回：`{ code: 0, data: { id: Long, url: String } }`
- 存入 t_image 表的 data 字段（LONGBLOB）

### 图片访问
`GET /api/library/image/{id}`
- 返回图片二进制，Content-Type 根据 t_image.type 设置
- 无需认证（SecurityConfig permitAll）

### 内容库 CRUD
Content 实体新增 `link` 字段，其余接口不变。

## 文件清单

### 后端（backend/src/main/java/com/wecorp/）
- entity/Content.java — 新增 link 字段
- entity/Image.java — 新实体
- mapper/ImageMapper.java — 新 Mapper
- controller/library/ImageController.java — 图片上传+访问
- service/SystemService.java — 新增 uploadImage / getImage 方法
- service/ServiceImpl/SystemServiceImpl.java — 实现
- resources/db/migration/V1.0.2__content_and_image.sql — link 列 + t_image 表
- config/SecurityConfig.java — permitAll /api/library/image/**

### 前端（frontend/）
- src/api/library/content.ts — ContentItem 新增 link 字段
- src/api/library/image.ts — 新 API 文件（uploadImage）
- src/views/library/content/index.vue — 重构弹窗（按类型区分字段、图片上传、富文本）

### 前端依赖
需要安装 `@wangeditor/editor` 和 `@wangeditor/editor-for-vue`

## 验收标准
1. 推文弹窗显示：推文链接、推文标题、推文描述、推文封面（上传+链接）、状态
2. 文章弹窗显示：文章标题、文章描述、文章封面（上传）、正文（富文本）、状态
3. 图片上传后存入数据库，访问 /api/library/image/{id} 能正确返回图片
4. 封面在卡片列表中正常显示
5. `mvn compile` 和 `pnpm build` 通过
