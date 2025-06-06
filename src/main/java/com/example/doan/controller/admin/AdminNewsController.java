package com.example.doan.controller.admin;

import com.example.doan.dto.request.ApiResponse;
import com.example.doan.entity.News;
import com.example.doan.service.NewsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/news")
@RequiredArgsConstructor
public class AdminNewsController {
    private final NewsService newsService;

    @PostMapping
    public ApiResponse<News> create(@RequestBody @Valid News news) {
        return ApiResponse.<News>builder().result(newsService.create(news)).build();
    }

    @PutMapping("/{id}")
    public ApiResponse<News> update(@PathVariable Long id, @RequestBody @Valid News news) {
        return ApiResponse.<News>builder().result(newsService.update(id, news)).build();
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