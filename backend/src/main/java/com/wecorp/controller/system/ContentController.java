package com.wecorp.controller.system;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wecorp.common.result.R;
import com.wecorp.common.utils.MediaValidator;
import com.wecorp.common.utils.WxMediaUtils;
import com.wecorp.entity.Content;
import com.wecorp.mapper.ImageMapper;
import com.wecorp.mapper.VideoMapper;
import com.wecorp.service.SystemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.cp.api.WxCpService;
import me.chanjar.weixin.cp.bean.external.WxCpAddMomentTask;
import me.chanjar.weixin.cp.bean.external.msg.Attachment;
import me.chanjar.weixin.cp.bean.external.msg.Image;
import me.chanjar.weixin.cp.bean.external.msg.Link;
import me.chanjar.weixin.cp.bean.external.msg.Text;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Collections;

@Slf4j
@RestController
@RequestMapping("/api/library/content")
@RequiredArgsConstructor
public class ContentController {

    private final SystemService systemService;
    private final WxCpService wxCpService;
    private final WxMediaUtils wxMediaUtils;
    private final ImageMapper imageMapper;
    private final VideoMapper videoMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/page")
    public R<Page<Content>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "12") int pageSize,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Integer status
    ) {
        return R.ok(systemService.getContentPage(pageNum, pageSize, type, title, status));
    }

    @PostMapping
    public R<Void> create(@RequestBody Content content) {
        systemService.createContent(content);
        return R.ok();
    }

    @PutMapping
    public R<Void> update(@RequestBody Content content) {
        systemService.updateContent(content);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        systemService.deleteContent(id);
        return R.ok();
    }

    /**
     * 一键发送内容到企微朋友圈
     * 流程：提取素材 → 上传朋友圈附件获取 media_id → SDK 创建朋友圈任务
     *
     * API 规则：
     *   text.content 与 attachments 不能同时为空
     *   附件类型三选一：image（最多9个）、link（最多1个）、video（最多1个）
     */
    @PostMapping("/send-moment/{id}")
    public R<?> sendMoment(@PathVariable Long id) {
        Content content = systemService.getContentById(id);
        if (content == null) {
            return R.fail("内容不存在");
        }

        String type = content.getType();
        if (!"image".equals(type) && !"tweet".equals(type) && !"article".equals(type) && !"video".equals(type)) {
            return R.fail("不支持的内容类型: " + type);
        }

        String momentText = buildMomentText(content);
        String mediaSource = "video".equals(type) ? content.getVideo() : content.getImage();
        if ((momentText == null || momentText.isBlank()) && (mediaSource == null || mediaSource.isBlank())) {
            return R.fail("文本内容与素材不能同时为空");
        }

        // tweet/article 必须有链接
        if (("tweet".equals(type) || "article".equals(type))
                && (content.getLink() == null || content.getLink().isBlank())) {
            return R.fail("链接不能为空");
        }

        // 上传附件
        String mediaId = null;
        if (mediaSource != null && !mediaSource.isBlank()) {
            byte[] bytes = fetchBytes(mediaSource);
            if (bytes == null) {
                return R.fail("无法获取素材");
            }

            // 朋友圈特有限制校验
            try {
                if ("video".equals(type)) {
                    MediaValidator.validateMomentVideo(bytes);
                } else {
                    MediaValidator.validateMomentImage(bytes);
                }
            } catch (IllegalArgumentException e) {
                return R.fail(e.getMessage());
            }

            boolean isVideo = "video".equals(type);
            String mediaType = isVideo ? "video" : "image";
            String ext = isVideo ? ".mp4" : guessImageExtension(mediaSource);
            try {
                mediaId = uploadAsMomentAttachment(bytes, ext, mediaType);
            } catch (Exception e) {
                return R.fail("上传附件失败: " + e.getMessage());
            }
        }

        // 构造朋友圈任务
        WxCpAddMomentTask task = new WxCpAddMomentTask();
        if (momentText != null && !momentText.isBlank()) {
            Text text = new Text();
            text.setContent(momentText);
            task.setText(text);
        }

        if (mediaId != null) {
            Attachment attachment = new Attachment();
            if ("image".equals(type)) {
                Image image = new Image();
                image.setMediaId(mediaId);
                attachment.setMsgType("image");
                attachment.setImage(image);
            } else if ("video".equals(type)) {
                me.chanjar.weixin.cp.bean.external.msg.Video video = new me.chanjar.weixin.cp.bean.external.msg.Video();
                video.setMediaId(mediaId);
                attachment.setMsgType("video");
                attachment.setVideo(video);
            } else {
                // tweet / article → link 类型
                Link link = new Link();
                link.setTitle(content.getTitle());
                link.setMediaId(mediaId);
                link.setUrl(content.getLink());
                attachment.setMsgType("link");
                attachment.setLink(link);
            }
            task.setAttachments(Collections.singletonList(attachment));
        }

        try {
            Object result = wxCpService.getExternalContactService().addMomentTask(task);
            log.info("朋友圈任务创建成功: contentId={}", id);
            return R.ok(result);
        } catch (WxErrorException e) {
            log.error("创建朋友圈任务失败: contentId={}", id, e);
            return R.fail("创建朋友圈任务失败: " + e.getMessage());
        }
    }

    // ========== 私有方法 ==========

    private String buildMomentText(Content content) {
        String text = content.getTitle();
        if (content.getDescription() != null && !content.getDescription().isBlank()) {
            text += "\n" + content.getDescription();
        }
        return text;
    }

    private String uploadAsMomentAttachment(byte[] bytes, String ext, String mediaType) throws Exception {
        File tempFile = File.createTempFile("wx_upload_", ext);
        try {
            try (OutputStream os = new FileOutputStream(tempFile)) {
                os.write(bytes);
            }
            return wxMediaUtils.uploadAttachment(tempFile, mediaType + ext, mediaType, 1);
        } finally {
            tempFile.delete();
        }
    }

    private byte[] fetchBytes(String source) {
        if (source == null || source.isBlank()) {
            return null;
        }
        try {
            if (source.startsWith("/api/image/")) {
                Long imageId = Long.parseLong(source.substring("/api/image/".length()));
                com.wecorp.entity.Image dbImage = imageMapper.selectById(imageId);
                return (dbImage != null && dbImage.getData() != null) ? dbImage.getData() : null;
            }
            if (source.startsWith("/api/video/")) {
                Long videoId = Long.parseLong(source.substring("/api/video/".length()));
                com.wecorp.entity.Video dbVideo = videoMapper.selectById(videoId);
                return (dbVideo != null && dbVideo.getData() != null) ? dbVideo.getData() : null;
            }
            if (source.startsWith("http://") || source.startsWith("https://")) {
                return restTemplate.getForObject(source, byte[].class);
            }
        } catch (Exception e) {
            log.error("获取素材失败: path={}", source, e);
        }
        return null;
    }

    private static String guessImageExtension(String path) {
        if (path == null) return ".jpg";
        String lower = path.toLowerCase();
        if (lower.contains(".png")) return ".png";
        return ".jpg";
    }
}
