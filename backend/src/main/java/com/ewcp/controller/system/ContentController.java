package com.ewcp.controller.system;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ewcp.common.result.R;
import com.ewcp.common.utils.WxMediaUtils;
import com.ewcp.entity.Content;
import com.ewcp.mapper.ImageMapper;
import com.ewcp.service.SystemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.cp.api.WxCpService;
import me.chanjar.weixin.cp.bean.external.WxCpAddMomentTask;
import me.chanjar.weixin.cp.bean.external.msg.Attachment;
import me.chanjar.weixin.cp.bean.external.msg.Image;
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
     * 流程：提取封面图 → 上传朋友圈附件获取 media_id → SDK 创建朋友圈任务
     */
    @PostMapping("/send-moment/{id}")
    public R<?> sendMoment(@PathVariable Long id) {
        Content content = systemService.getContentById(id);
        if (content == null) {
            return R.fail("内容不存在");
        }

        // 1. 上传封面图为朋友圈附件
        String mediaId = null;
        try {
            mediaId = uploadImageAsMomentAttachment(content.getImage());
        } catch (Exception e) {
            log.warn("上传朋友圈附件失败，将仅发送文字: contentId={}, error={}", id, e.getMessage());
        }

        // 2. 构造朋友圈任务（SDK）
        WxCpAddMomentTask task = new WxCpAddMomentTask();

        Text text = new Text();
        String momentText = content.getTitle();
        if (content.getDescription() != null && !content.getDescription().isBlank()) {
            momentText += "\n" + content.getDescription();
        }
        text.setContent(momentText);
        task.setText(text);

        if (mediaId != null) {
            Image image = new Image();
            image.setMediaId(mediaId);
            Attachment attachment = new Attachment();
            attachment.setMsgType("image");
            attachment.setImage(image);
            task.setAttachments(Collections.singletonList(attachment));
        }

        // 3. 调用企微 SDK 创建朋友圈任务
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

    /**
     * 从 image 字段提取图片并上传为朋友圈附件，返回 media_id
     */
    private String uploadImageAsMomentAttachment(String imagePath) throws Exception {
        byte[] imageBytes = fetchImageBytes(imagePath);
        if (imageBytes == null) {
            return null;
        }

        String ext = guessExtension(imagePath);
        File tempFile = File.createTempFile("wx_upload_", ext);
        try {
            try (OutputStream os = new FileOutputStream(tempFile)) {
                os.write(imageBytes);
            }
            String mediaId = wxMediaUtils.uploadAttachment(tempFile, "image" + ext, "image", 1);
            log.info("朋友圈附件上传成功: mediaId={}", mediaId);
            return mediaId;
        } finally {
            tempFile.delete();
        }
    }

    /**
     * 从内容的 image 字段提取图片字节（支持本地 DB 存储和外部 URL）
     */
    private byte[] fetchImageBytes(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            return null;
        }
        try {
            if (imagePath.startsWith("/api/image/")) {
                Long imageId = Long.parseLong(imagePath.substring("/api/image/".length()));
                com.ewcp.entity.Image dbImage = imageMapper.selectById(imageId);
                return (dbImage != null && dbImage.getData() != null) ? dbImage.getData() : null;
            }
            if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
                return restTemplate.getForObject(imagePath, byte[].class);
            }
        } catch (Exception e) {
            log.error("获取封面图失败: path={}", imagePath, e);
        }
        return null;
    }

    private static String guessExtension(String path) {
        if (path == null) return ".jpg";
        String lower = path.toLowerCase();
        if (lower.contains(".png")) return ".png";
        if (lower.contains(".gif")) return ".gif";
        return ".jpg";
    }
}
