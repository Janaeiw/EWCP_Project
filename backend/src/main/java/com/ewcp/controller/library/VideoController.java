package com.ewcp.controller.library;

import com.ewcp.common.result.R;
import com.ewcp.common.utils.MediaValidator;
import com.ewcp.entity.Video;
import com.ewcp.service.SystemService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/video")
@RequiredArgsConstructor
public class VideoController {

    private final SystemService systemService;

    @PostMapping("/upload")
    public R<Map<String, Object>> upload(@RequestParam("file") MultipartFile file) throws IOException {
        byte[] data = file.getBytes();
        MediaValidator.validateVideo(data, file.getOriginalFilename());
        Long id = systemService.uploadVideo(data, file.getOriginalFilename(), file.getContentType());
        Map<String, Object> result = new HashMap<>();
        result.put("id", id);
        result.put("url", "/api/video/" + id);
        return R.ok(result);
    }

    @GetMapping("/{id}")
    public void getVideo(@PathVariable Long id, HttpServletResponse response) throws IOException {
        Video video = systemService.getVideo(id);
        if (video == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        response.setContentType(video.getType() != null ? video.getType() : "application/octet-stream");
        response.getOutputStream().write(video.getData());
        response.getOutputStream().flush();
    }
}
