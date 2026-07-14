package com.wecorp.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("t_menu")
public class Menu {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long parentId;

    private String path;

    private String name;

    private String component;

    private String title;

    private String icon;

    @TableField("`rank`")
    private Integer rank;

    private Integer menuType;

    private String permission;

    private Integer showLink;

    private Integer status;

    private String remark;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private List<Menu> children;
}
