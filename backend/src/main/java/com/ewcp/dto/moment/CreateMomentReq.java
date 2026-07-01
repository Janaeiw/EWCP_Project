package com.ewcp.dto.moment;

import lombok.Data;

import java.util.List;

@Data
public class CreateMomentReq {

    private String text;

    private List<AttachmentItem> attachments;

    private VisibleRange visibleRange;

    @Data
    public static class AttachmentItem {
        /** 附件类型: image, link, video, miniprogram */
        private String msgType;
        private String mediaId;
        private String title;
        private String url;
        private String thumbMediaId;
    }

    @Data
    public static class VisibleRange {
        private List<String> userList;
        private List<String> departmentList;
        private List<String> tagList;
    }
}
