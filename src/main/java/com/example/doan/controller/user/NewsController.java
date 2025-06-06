package com.example.doan.controller.user;

import com.example.doan.dto.request.ApiResponse;
import com.example.doan.entity.News;
import com.example.doan.service.NewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/news")
@RequiredArgsConstructor
public class NewsController {
    private final NewsService newsService;

    @GetMapping
    public ApiResponse<List<News>> getAll() {
        return ApiResponse.<List<News>>builder().result(newsService.getAll()).build();
    }

    @GetMapping("/{slug}")
    public ApiResponse<News> getBySlug(@PathVariable String slug) {
        return ApiResponse.<News>builder().result(newsService.getBySlug(slug)).build();
    }
}