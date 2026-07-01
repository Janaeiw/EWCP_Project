-- 字典类型表
CREATE TABLE IF NOT EXISTS `t_dict` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `dict_code`   VARCHAR(64)  NOT NULL COMMENT '字典编码',
    `dict_name`   VARCHAR(64)  NOT NULL COMMENT '字典名称',
    `remark`      VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1=启用 0=禁用',
    `deleted`     INT          NOT NULL DEFAULT 0 COMMENT '逻辑删除 0=正常 1=删除',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_dict_code` (`dict_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典类型表';

-- 字典数据表
CREATE TABLE IF NOT EXISTS `t_dict_data` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `dict_code`   VARCHAR(64)  NOT NULL COMMENT '所属字典编码',
    `label`       VARCHAR(100) NOT NULL COMMENT '显示文本',
    `value`       VARCHAR(100) NOT NULL COMMENT '字典值',
    `sort`        INT          DEFAULT 0 COMMENT '排序',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 1=启用 0=禁用',
    `deleted`     INT          NOT NULL DEFAULT 0 COMMENT '逻辑删除 0=正常 1=删除',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_dict_code` (`dict_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典数据表';
