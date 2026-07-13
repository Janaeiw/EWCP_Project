package com.wecorp.dto.moment;

import lombok.Data;

@Data
public class MomentPageReq {

    private Long startTime;

    private Long endTime;

    private String creator;

    private Integer filterType;

    private String cursor;

    private Integer limit = 100;
}
