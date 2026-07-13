package com.wecorp.controller.library;

import com.wecorp.common.result.R;
import com.wecorp.common.utils.MediaValidator;
import com.wecorp.entity.Image;
import com.wecorp.service.SystemService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/image")
@RequiredArgsConstructor
public class ImageController {

    private final SystemService systemService;

    @PostMapping("/upload")
    public R<Map<String, Object>> upload(@RequestParam("file") MultipartFile file) throws IOException {
        byte[] data = file.getBytes();
        MediaValidator.validateImage(data, file.getOriginalFilename());
        Long id = systemService.uploadImage(data, file.getOriginalFilename(), file.getContentType());
        Map<String, Object> result = new HashMap<>();
        result.put("id", id);
        result.put("url", "/api/image/" + id);
        return R.ok(result);
    }

    @GetMapping("/{id}")
    public void getImage(@PathVariable Long id, HttpServletResponse response) throws IOException {
        Image image = systemService.getImage(id);
        if (image == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        response.setContentType(image.getType() != null ? image.getType() : "application/octet-stream");
        response.getOutputStream().write(image.getData());
        response.getOutputStream().flush();
    }
}
