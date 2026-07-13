package com.wecorp.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_image")
public class Image {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String type;

    private byte[] data;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
