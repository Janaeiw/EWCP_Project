# 系统管理菜单

## Goal

根据需求文档 2.13 系统管理章节，实现系统管理菜单路由，并完成用户管理、角色管理、权限管理三个模块的完整前后端 CRUD 逻辑与页面。

## Requirements

### Phase 1（已完成）
1. 在 asyncRoutes.ts 中添加系统管理路由（含 9 个子菜单）
2. 重新启用 mock server（用于 asyncRoutes 接口）
3. 恢复登录页的 initRouter 调用
4. 创建 9 个系统管理子页面组件（骨架页即可）

### Phase 2（本次任务）

#### 后端

1. **用户管理 CRUD**
   - 表：`t_user`（已有，需新增 `email`、`phone`、`dept_id` 字段）
   - 接口：
     - `GET /api/system/user/page` — 分页查询（支持 username/nickname/status 筛选）
     - `POST /api/system/user` — 新增用户
     - `PUT /api/system/user` — 修改用户
     - `DELETE /api/system/user/{id}` — 删除用户（逻辑删除）
     - `GET /api/system/user/{id}` — 查询单个用户详情
   - 密码使用 BCrypt 加密，新增用户默认密码 `Password123`

2. **角色管理 CRUD**
   - 新建表：`t_role`（id, role_name, role_key, sort, status, remark, deleted, create_time, update_time）
   - 新建表：`t_user_role`（user_id, role_id）关联表
   - 接口：
     - `GET /api/system/role/page` — 分页查询
     - `POST /api/system/role` — 新增角色
     - `PUT /api/system/role` — 修改角色
     - `DELETE /api/system/role/{id}` — 删除角色（逻辑删除）
     - `GET /api/system/role/list` — 获取所有角色列表（下拉选择用）

3. **权限管理 CRUD**
   - 新建表：`t_permission`（id, parent_id, name, permission_key, type（menu/button/api）, sort, status, remark, deleted, create_time, update_time）
   - 新建表：`t_role_permission`（role_id, permission_id）关联表
   - 接口：
     - `GET /api/system/permission/tree` — 树形查询
     - `POST /api/system/permission` — 新增权限
     - `PUT /api/system/permission` — 修改权限
     - `DELETE /api/system/permission/{id}` — 删除权限（逻辑删除）

4. **数据库迁移**
   - 新增 SQL 脚本 `V1.0.1__system_tables.sql`
   - 包含 t_role、t_permission、t_user_role、t_role_permission 建表语句
   - t_user 表 ALTER 添加新字段

#### 前端

1. **用户管理页面** (`src/views/system/user/index.vue`)
   - 搜索栏：username、nickname、status
   - 表格：@pureadmin/table，列：用户名、昵称、邮箱、手机、角色、状态、操作
   - 操作：新增、编辑、删除、重置密码
   - 弹窗表单：el-form 配合 dialog

2. **角色管理页面** (`src/views/system/role/index.vue`)
   - 搜索栏：role_name、status
   - 表格：角色名、角色标识、排序、状态、备注、操作
   - 操作：新增、编辑、删除、权限分配（树形勾选）

3. **权限管理页面** (`src/views/system/permission/index.vue`)
   - 树形表格展示
   - 操作：新增、编辑、删除
   - 弹窗表单：父级权限（树形选择）、类型、名称、标识、排序

4. **API 层**
   - `src/api/system/user.ts`
   - `src/api/system/role.ts`
   - `src/api/system/permission.ts`

## Database Design

```sql
-- 角色表
CREATE TABLE t_role (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  role_name VARCHAR(64) NOT NULL COMMENT '角色名称',
  role_key VARCHAR(64) NOT NULL COMMENT '角色标识',
  sort INT DEFAULT 0 COMMENT '排序',
  status TINYINT DEFAULT 1 COMMENT '状态 1=启用 0=禁用',
  remark VARCHAR(500) COMMENT '备注',
  deleted TINYINT DEFAULT 0,
  create_time DATETIME,
  update_time DATETIME
);

-- 权限表
CREATE TABLE t_permission (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  parent_id BIGINT DEFAULT 0 COMMENT '父权限ID',
  name VARCHAR(64) NOT NULL COMMENT '权限名称',
  permission_key VARCHAR(128) NOT NULL COMMENT '权限标识',
  type VARCHAR(16) NOT NULL COMMENT '类型: menu/button/api',
  sort INT DEFAULT 0,
  status TINYINT DEFAULT 1,
  remark VARCHAR(500),
  deleted TINYINT DEFAULT 0,
  create_time DATETIME,
  update_time DATETIME
);

-- 用户-角色关联
CREATE TABLE t_user_role (
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  PRIMARY KEY (user_id, role_id)
);

-- 角色-权限关联
CREATE TABLE t_role_permission (
  role_id BIGINT NOT NULL,
  permission_id BIGINT NOT NULL,
  PRIMARY KEY (role_id, permission_id)
);
```

## Tech Stack（已确认）

- 后端：Java 17 + Spring Boot 3.3.5 + MyBatis-Plus 3.5.9 + Maven + MySQL
- 前端：Vue 3 + Vite (pure-admin-thin) + TypeScript + Element Plus + Tailwind CSS
- 统一响应格式：`R<T>`，code=0 表示成功
- 逻辑删除：deleted 字段，0=未删除，1=已删除
- 时间自动填充：createTime、updateTime

## Acceptance Criteria

- [ ] t_user 表新增 email、phone、dept_id 字段
- [ ] 用户管理：列表分页查询、新增、编辑、删除、重置密码功能正常
- [ ] 角色管理：列表分页查询、新增、编辑、删除、角色列表获取功能正常
- [ ] 权限管理：树形查询、新增、编辑、删除功能正常
- [ ] 前端页面使用 Element Plus + @pureadmin/table 组件
- [ ] 后端接口返回统一 R<T> 格式
- [ ] 新增数据库迁移脚本
- [ ] 代码符合 .trellis/spec 规范

### Phase 3（本次任务 — 菜单管理）

#### 后端

1. **菜单管理 CRUD**
   - 新建表：`t_menu`（参考 asyncRoutes 的路由字段结构）
   - 接口：
     - `GET /api/system/menu/tree` — 树形查询（参考权限管理的 tree 接口）
     - `POST /api/system/menu` — 新增菜单
     - `PUT /api/system/menu` — 修改菜单
     - `DELETE /api/system/menu/{id}` — 删除菜单（逻辑删除，级联删除子菜单）
     - `GET /api/system/menu/{id}` — 查询单个菜单详情
   - 数据库迁移：`V1.0.7__menu_table.sql`

#### 前端

1. **菜单管理页面** (`src/views/system/menu/index.vue`)
   - 树形表格展示（参考权限管理页面的 tree-props 模式）
   - 表格列：标题、图标、路由路径、组件路径、路由名称、排序、是否显示、状态、操作
   - 操作：新增子菜单、编辑、删除
   - 弹窗表单：父级菜单（树形选择）、类型、路由路径、组件路径、路由名称、图标、排序、角色权限、是否显示、状态

2. **API 层**
   - `src/api/system/menu.ts`

## Database Design — Menu

```sql
CREATE TABLE t_menu (
  id BIGINT NOT NULL AUTO_INCREMENT,
  parent_id BIGINT DEFAULT 0 COMMENT '父菜单ID',
  path VARCHAR(200) NOT NULL COMMENT '路由路径',
  name VARCHAR(100) COMMENT '路由名称',
  component VARCHAR(255) COMMENT '组件路径',
  title VARCHAR(64) NOT NULL COMMENT '菜单标题',
  icon VARCHAR(100) COMMENT '菜单图标',
  rank INT DEFAULT 0 COMMENT '排序',
  roles VARCHAR(255) COMMENT '可访问角色(JSON数组)',
  auths VARCHAR(500) COMMENT '按钮权限(JSON数组)',
  show_link TINYINT DEFAULT 1 COMMENT '是否显示菜单 1=是 0=否',
  status TINYINT DEFAULT 1 COMMENT '状态 1=启用 0=禁用',
  remark VARCHAR(500) COMMENT '备注',
  deleted INT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

## Out of Scope

- 机构管理、日志管理、字典管理、系统参数管理、定时任务管理的 CRUD
- 角色-用户分配 UI
- 权限校验中间件（Spring Security 权限拦截）
