package com.agri.platform.controller;

import com.agri.platform.common.Result;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 通用文件上传（图片 / PDF 等）。
 * 公开接口（WebConfig 已放行 /upload）；返回 { url, filename }。
 * 文件保存到运行目录下的 uploads/，经 /uploads/** 静态映射访问。
 * 返回的 url 带 /api 前缀，前端可直接用于 <img :src>（经 Vite 代理转发到后端）。
 */
@RestController
public class UploadController {

    @PostMapping("/upload")
    public Result<Map<String, String>> upload(@RequestParam("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return Result.error("上传文件为空");
        }
        String origin = file.getOriginalFilename();
        String ext = "";
        if (origin != null && origin.lastIndexOf('.') >= 0) {
            ext = origin.substring(origin.lastIndexOf('.'));
        }
        // 用 UUID 防止重名覆盖
        String filename = UUID.randomUUID().toString().replace("-", "") + ext;

        File dir = new File("uploads");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new RuntimeException("创建上传目录失败");
        }
        file.transferTo(new File(dir.getAbsolutePath(), filename));

        Map<String, String> data = new HashMap<>();
        data.put("url", "/api/uploads/" + filename);
        data.put("filename", filename);
        return Result.success(data);
    }
}
