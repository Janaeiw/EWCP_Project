-- t_menu 新增字段
ALTER TABLE t_menu ADD COLUMN menu_type TINYINT DEFAULT 0 COMMENT '菜单类型：0=目录/菜单，1=按钮';
ALTER TABLE t_menu ADD COLUMN permission VARCHAR(100) DEFAULT NULL COMMENT '权限标识，如 system:user:add';

-- t_menu 移除字段（roles 和 auths 已被 menuType + t_role_menu 替代）
ALTER TABLE t_menu DROP COLUMN roles;
ALTER TABLE t_menu DROP COLUMN auths;

-- 新建角色-菜单关联表
CREATE TABLE IF NOT EXISTS t_role_menu (
    role_id     BIGINT   NOT NULL COMMENT '角色ID',
    menu_id     BIGINT   NOT NULL COMMENT '菜单ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (role_id, menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色-菜单关联表';
