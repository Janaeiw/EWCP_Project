package com.ewcp.controller.wx;

import com.ewcp.common.result.R;
import com.ewcp.dto.moment.CreateMomentReq;
import com.ewcp.dto.moment.MomentPageReq;
import lombok.RequiredArgsConstructor;
import me.chanjar.weixin.cp.api.WxCpService;
import me.chanjar.weixin.cp.bean.external.WxCpAddMomentTask;
import me.chanjar.weixin.cp.bean.external.msg.Attachment;
import me.chanjar.weixin.cp.bean.external.msg.Image;
import me.chanjar.weixin.cp.bean.external.msg.Link;
import me.chanjar.weixin.cp.bean.external.msg.MiniProgram;
import me.chanjar.weixin.cp.bean.external.msg.Text;
import me.chanjar.weixin.cp.bean.external.msg.Video;
import me.chanjar.weixin.cp.bean.external.moment.ExternalContactList;
import me.chanjar.weixin.cp.bean.external.moment.SenderList;
import me.chanjar.weixin.cp.bean.external.moment.VisibleRange;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/wx/moment")
@RequiredArgsConstructor
public class MomentController {

    private final WxCpService wxCpService;

    /**
     * 创建朋友圈任务（异步，返回 jobId）
     */
    @PostMapping
    public R<?> create(@RequestBody CreateMomentReq req) {
        try {
            WxCpAddMomentTask task = new WxCpAddMomentTask();

            // 设置文本内容
            Text text = new Text();
            text.setContent(req.getText());
            task.setText(text);

            // 转换 attachments
            if (req.getAttachments() != null) {
                List<Attachment> attachments = new ArrayList<>();
                for (CreateMomentReq.AttachmentItem item : req.getAttachments()) {
                    Attachment attachment = new Attachment();
                    switch (item.getMsgType()) {
                        case "image" -> {
                            Image image = new Image();
                            image.setMediaId(item.getMediaId());
                            attachment.setImage(image);
                        }
                        case "link" -> {
                            Link link = new Link();
                            link.setTitle(item.getTitle());
                            link.setUrl(item.getUrl());
                            link.setMediaId(item.getMediaId());
                            attachment.setLink(link);
                        }
                        case "video" -> {
                            Video video = new Video();
                            video.setMediaId(item.getMediaId());
                            attachment.setVideo(video);
                        }
                        case "miniprogram" -> {
                            MiniProgram miniProgram = new MiniProgram();
                            miniProgram.setTitle(item.getTitle());
                            miniProgram.setAppid(item.getMediaId());
                            miniProgram.setPage(item.getUrl());
                            miniProgram.setPicMediaId(item.getThumbMediaId());
                            attachment.setMiniProgram(miniProgram);
                        }
                        default -> {
                        }
                    }
                    attachments.add(attachment);
                }
                task.setAttachments(attachments);
            }

            // 设置可见范围（发送人 + 外部联系人标签）
            if (req.getVisibleRange() != null) {
                CreateMomentReq.VisibleRange reqRange = req.getVisibleRange();
                VisibleRange visibleRange = new VisibleRange();

                // 发送人列表（含部门）
                SenderList senderList = new SenderList();
                senderList.setUserList(reqRange.getUserList());
                senderList.setDepartmentList(reqRange.getDepartmentList());
                visibleRange.setSenderList(senderList);

                // 外部联系人标签
                if (reqRange.getTagList() != null) {
                    ExternalContactList externalContactList = new ExternalContactList();
                    externalContactList.setTagList(reqRange.getTagList());
                    visibleRange.setExternalContactList(externalContactList);
                }

                task.setVisibleRange(visibleRange);
            }

            Object result = wxCpService.getExternalContactService().addMomentTask(task);
            return R.ok(result);
        } catch (WxErrorException e) {
            return R.fail("创建朋友圈任务失败: " + e.getMessage());
        }
    }

    /**
     * 查询朋友圈任务结果
     */
    @GetMapping("/{jobId}/result")
    public R<?> getTaskResult(@PathVariable String jobId) {
        try {
            Object result = wxCpService.getExternalContactService().getMomentTaskResult(jobId);
            return R.ok(result);
        } catch (WxErrorException e) {
            return R.fail("查询任务结果失败: " + e.getMessage());
        }
    }

    /**
     * 获取朋友圈列表（分页，游标翻页）
     */
    @GetMapping("/list")
    public R<?> list(MomentPageReq req) {
        try {
            Object result = wxCpService.getExternalContactService().getMomentList(
                    req.getStartTime(), req.getEndTime(),
                    req.getCreator(), req.getFilterType(),
                    req.getCursor(), req.getLimit()
            );
            return R.ok(result);
        } catch (WxErrorException e) {
            return R.fail("获取朋友圈列表失败: " + e.getMessage());
        }
    }

    /**
     * 取消朋友圈任务
     */
    @PostMapping("/{momentId}/cancel")
    public R<?> cancel(@PathVariable String momentId) {
        try {
            wxCpService.getExternalContactService().cancelMomentTask(momentId);
            return R.ok();
        } catch (WxErrorException e) {
            return R.fail("取消朋友圈任务失败: " + e.getMessage());
        }
    }
}
