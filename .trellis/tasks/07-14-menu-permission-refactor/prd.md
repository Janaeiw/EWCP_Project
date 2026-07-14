# 菜单权限体系重构 - 分离菜单访问与按钮权限

## Goal

将当前 `t_menu` 表中混杂的 `roles`（可访问角色）和 `auths`（按钮权限）解耦。通过引入 `menu_type` 字段区分菜单与按钮，把按钮权限作为菜单树的叶子节点管理。同时在角色管理中新增权限分配功能（树形勾选），并打通登录时的真实权限查询。

**核心动机**：当前架构下，给角色开页面权限就必然连带按钮权限，无法精细化控制。需要实现"能看页面但不能点新增按钮"这类场景。

## What I already know

- `t_menu` 表无 `menu_type` 字段，按钮权限通过 `auths`（JSON 数组字符串）存储在同一行
- `roles` 字段也是 JSON 数组字符串，控制页面级访问
- 已有 `t_permission` + `t_role_permission` 表但完全未使用（权限管理页面独立存在）
- 角色管理页面无权限分配入口，无权限分配 API
- 登录时权限硬编码：admin 返回 `["*:*:*"]`，其他角色返回空数组 `[]`
- 路由构建：`getRouteTree()` 查 `status=1 && showLink=1` 的菜单
- 前端权限检查：`hasPerms()` 函数 + `v-perms` 指令已就绪，只缺数据源
- Flyway 迁移文件已有 V1.0.7 菜单表、V1.0.1 系统表

## Assumptions (verified)

- `t_permission` 和 `t_role_permission` 表保留不动（权限管理页面不受影响）
- 新建 `t_role_menu` 关联表，用于角色-菜单/按钮权限分配
- `menu_type` 值：0=目录/菜单（默认），1=按钮
- 按钮类型节点 `showLink` 默认 0（不展示在左侧菜单）
- admin 角色登录仍返回 `["*:*:*"]`，不走关联表查询
- 表单中 showLink 字段根据 menuType 显隐（按钮类型隐藏此字段）

## Requirements

### R1: 数据库变更
- `t_menu` 表新增 `menu_type` 字段（TINYINT, 0=菜单/目录, 1=按钮, 默认 0）
- `t_menu` 表新增 `permission` 字段（VARCHAR(100)），存储按钮权限标识如 `system:user:add`
- 创建 `t_role_menu` 关联表（`role_id` + `menu_id` 联合主键）
- `t_permission` 和 `t_role_permission` 表不动（后续单独清理）
- `t_menu` 表移除 `roles`、`auths` 字段（已被 menuType + t_role_menu 替代）
- 迁移脚本通过 Flyway 管理

### R2: 菜单管理页面改造
- 新增/编辑表单增加"菜单类型"字段（select: 目录菜单/按钮）
- 新增/编辑表单增加"权限标识"字段（仅 menuType=button 时显示）
- 移除表单中的"可访问角色"字段（roles 移到角色管理统一分配）
- 移除表单中的"按钮权限"字段（auths 由子级菜单替代）
- "路由路径""组件路径""路由名称""图标""是否显示"字段仅 menuType=菜单 时显示
- 树形表格新增"菜单类型"列
- 按钮类型的子菜单在表格中正常展示

### R3: 后端 Menu 改造
- `Menu.java` 实体新增 `menuType`、`permission` 字段，移除 `roles`、`auths`
- `MenuController` 无需新增端点
- `SystemServiceImpl.getMenuTree()` 包含按钮类型节点（管理用）
- `SystemServiceImpl.getRouteTree()` 仅查询 `menuType != 1` 的节点构建路由
- 路由 meta 保留 roles/auths，由后端从 t_role_menu 动态生成
- 路由构建时 roles = 拥有该菜单访问权限的角色 key 列表
- 路由构建时 auths = 该菜单下已授权给角色的按钮 permission 列表
- admin 角色不走关联表，路由直接放行

### R4: 角色管理 - 权限分配
- 角色列表操作列新增"权限"按钮
- 点击后弹出抽屉/弹窗，展示菜单树（含按钮叶子节点），支持父子联动勾选
- 勾选父节点自动勾选所有子节点，取消子节点可联动取消父节点
- 保存时调用后端接口持久化 `t_role_menu` 关联

### R5: 后端角色权限 API
- `GET /api/system/role/{id}/menus` — 查询角色已分配的菜单 ID 列表
- `PUT /api/system/role/{id}/menus` — 批量保存角色-菜单关联（接收 menuId 列表）
- `RoleController` 新增对应端点
- `SystemServiceImpl` 实现查询和保存逻辑

### R6: 登录权限查询
- `AuthController.login()` 方法中，非 admin 角色从 `t_role_menu` 关联表查询权限
- 查询逻辑：通过 `user → t_user_role → t_role_menu → t_menu` 链路获取权限标识列表
- `t_menu.permission` 字段值汇总为 permissions 列表返回
- admin 角色仍返回 `["*:*:*"]`

### R7: 前端路由结构调整（动态生成 roles/auths）
- 路由 meta 继续保留 `roles` 和 `auths` 字段，结构不变
- `roles` 由后端从 `t_role_menu` 关联表动态生成（哪些角色勾选了该菜单，就出现在 roles 数组中）
- `auths` 由后端从子级按钮权限动态生成（该菜单下的 button 类型子节点中，被角色勾选的 permission 值汇总）
- 路由守卫 `filterNoPermissionTree()` 逻辑不变，仍基于 meta.roles 过滤
- `v-perms` 指令逻辑不变，仍基于 hasPerms() + auths 检查

### R8: 路由构建时 roles/auths 的生成规则
- `buildRouteTree()` 查询 `t_menu` + `t_role_menu` 关联
- 对每个菜单节点：查询 `t_role_menu JOIN t_role` 获取拥有该菜单访问权限的角色 key 列表 → 写入 meta.roles
- 对每个菜单节点：查询其子节点中 `menu_type=1` 的按钮，再与 `t_role_menu` 取交集，得到当前已授权的 permission 列表 → 写入 meta.auths
- admin 角色不参与关联查询，路由仍返回所有菜单（roles 包含 admin 时前端直接放行）

## Acceptance Criteria

- [ ] 菜单管理可以创建菜单类型和按钮类型的子级菜单
- [ ] 按钮类型菜单有权限标识字段，且默认隐藏在左侧导航
- [ ] 菜单管理表单根据菜单类型动态显隐字段
- [ ] 角色管理可以为角色分配菜单和按钮权限，支持树形勾选和父子联动
- [ ] 保存角色权限后数据持久化到 t_role_menu 表
- [ ] 登录时非 admin 用户能获取到真实的 permissions 列表
- [ ] admin 用户登录仍返回 ["*:*:*"]
- [ ] 前端 v-perms 指令基于真实权限数据控制按钮显隐
- [ ] Flyway 迁移脚本正确执行，数据不丢失

## Definition of Done

- 前后端代码改造完成，功能可用
- Flyway 迁移脚本可重复执行
- 现有菜单数据不丢失，自动迁移 menuType=0
- Lint / typecheck 通过

## Out of Scope

- 后端 API 级别的权限拦截（SecurityConfig 仍 permitAll）
- `t_permission` 表和权限管理页面的清理（后续单独处理）
- 按钮权限的国际化
- 菜单导入/导出功能

## Technical Notes

- 项目使用 Spring Boot + MyBatis-Plus + Flyway
- 前端 Vue3 + Element Plus + pure-admin 框架
- 权限检查工具函数 `hasPerms()` 已在 `src/utils/auth.ts` 中实现
- `v-perms` 指令已在 `src/directives/perms/index.ts` 中实现
- 关键文件：
  - 后端：`Menu.java`, `SystemServiceImpl.java`, `AuthController.java`, `RoleController.java`
  - 前端：`src/views/system/menu/index.vue`, `src/views/system/role/index.vue`
  - API：`src/api/system/menu.ts`, `src/api/system/role.ts`
  - 路由：`src/router/utils.ts`
