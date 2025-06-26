package com.example.doan.controller.admin;

import com.example.doan.dto.request.ApiResponse;
import com.example.doan.entity.News;
import com.example.doan.service.NewsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.List;

@RestController
@RequestMapping("/admin/news")
@RequiredArgsConstructor
public class AdminNewsController {
    private final NewsService newsService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<News> create(
            @RequestPart("title") String title,
            @RequestPart("content") String content,
            @RequestPart("slug") String slug,
            @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail) {
        News news = new News();
        news.setTitle(title);
        news.setContent(content);
        news.setSlug(slug);
        if (thumbnail != null && !thumbnail.isEmpty()) {
            try {
                String filename = System.currentTimeMillis() + "_"
                        + StringUtils.cleanPath(thumbnail.getOriginalFilename());
                Path uploadPath = Paths.get("uploads/" + filename);
                Files.createDirectories(uploadPath.getParent());
                Files.copy(thumbnail.getInputStream(), uploadPath);
                news.setThumbnailUrl("/uploads/" + filename);
            } catch (Exception e) {
                throw new RuntimeException("Lỗi upload ảnh: " + e.getMessage());
            }
        }
        return ApiResponse.<News>builder().result(newsService.create(news)).build();
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<News> update(
            @PathVariable Long id,
            @RequestPart("title") String title,
            @RequestPart("content") String content,
            @RequestPart("slug") String slug,
            @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail) {
        News oldNews = newsService.getById(id);
        News updatedNews = new News();
        updatedNews.setTitle(title);
        updatedNews.setContent(content);
        updatedNews.setSlug(slug);
        if (thumbnail != null && !thumbnail.isEmpty()) {
            try {
                String filename = System.currentTimeMillis() + "_"
                        + StringUtils.cleanPath(thumbnail.getOriginalFilename());
                Path uploadPath = Paths.get("uploads/" + filename);
                Files.createDirectories(uploadPath.getParent());
                Files.copy(thumbnail.getInputStream(), uploadPath);
                updatedNews.setThumbnailUrl("/uploads/" + filename);
            } catch (Exception e) {
                throw new RuntimeException("Lỗi upload ảnh: " + e.getMessage());
            }
        } else {
            updatedNews.setThumbnailUrl(oldNews.getThumbnailUrl());
        }
        return ApiResponse.<News>builder().result(newsService.update(id, updatedNews)).build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> delete(@PathVariable Long id) {
        newsService.delete(id);
        return ApiResponse.<String>builder().result("Xoá tin tức thành công").build();
    }

    @GetMapping
    public ApiResponse<List<News>> getAll() {
        return ApiResponse.<List<News>>builder().result(newsService.getAll()).build();
    }

    @GetMapping("/{id}")
    public ApiResponse<News> getById(@PathVariable Long id) {
        return ApiResponse.<News>builder().result(newsService.getById(id)).build();
    }
}