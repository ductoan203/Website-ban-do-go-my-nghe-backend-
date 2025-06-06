package com.example.doan.controller;

import com.example.doan.dto.request.ApiResponse;
import com.example.doan.dto.response.SearchResponse;
import com.example.doan.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    public ApiResponse<SearchResponse> search(@RequestParam("term") String searchTerm) {
        return ApiResponse.<SearchResponse>builder()
                .result(searchService.search(searchTerm))
                .build();
    }
}