package com.example.doan.service;

import com.example.doan.entity.News;
import com.example.doan.repository.NewsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NewsService {
    private final NewsRepository newsRepository;

    public List<News> getAll() {
        return newsRepository.findAll();
    }

    public News getById(Long id) {
        return newsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("News not found"));
    }

    public News getBySlug(String slug) {
        return newsRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("News not found"));
    }

    public News create(News news) {
        news.setCreatedAt(LocalDateTime.now());
        news.setUpdatedAt(LocalDateTime.now());
        return newsRepository.save(news);
    }

    public News update(Long id, News updatedNews) {
        News news = getById(id);
        news.setTitle(updatedNews.getTitle());
        news.setSlug(updatedNews.getSlug());
        news.setContent(updatedNews.getContent());
        news.setThumbnailUrl(updatedNews.getThumbnailUrl());
        news.setUpdatedAt(LocalDateTime.now());
        return newsRepository.save(news);
    }

    public void delete(Long id) {
        newsRepository.deleteById(id);
    }
}