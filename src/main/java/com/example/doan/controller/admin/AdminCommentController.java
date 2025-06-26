package com.example.doan.controller.admin;

import com.example.doan.dto.request.ApiResponse;
import com.example.doan.entity.Comment;
import com.example.doan.service.CommentService;
import com.example.doan.dto.request.CommentRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.example.doan.dto.response.CommentResponse;
import com.example.doan.dto.response.UserResponse;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;

import java.util.List;

@RestController
@RequestMapping("/admin/comments")
@RequiredArgsConstructor
public class AdminCommentController {

    private final CommentService commentService;

    @GetMapping
    public ApiResponse<List<CommentResponse>> getAllComments() {
        List<Comment> comments = commentService.getAllComments();
        List<CommentResponse> dtos = comments.stream().map(c -> new CommentResponse(
                        c.getId(),
                        c.getContent(),
                        c.getCreatedAt(),
                        new UserResponse(
                                c.getUser().getUserId(),
                                null, null, c.getUser().getFullname(), null, null, null, null),
                        c.getProduct() != null
                                ? new CommentResponse.ProductInfo(c.getProduct().getId(), c.getProduct().getName())
                                : null,
                        c.getParent() != null ? c.getParent().getId() : null))
                .toList();
        return ApiResponse.<List<CommentResponse>>builder()
                .result(dtos)
                .build();
    }

    @PostMapping("/reply")
    public ApiResponse<CommentResponse> replyToComment(@RequestParam Long productId, @RequestParam Long parentCommentId,
                                                       @RequestBody CommentRequest request) {
        Comment comment = commentService.replyToComment(productId, parentCommentId, request.getContent());
        CommentResponse dto = new CommentResponse(
                comment.getId(),
                comment.getContent(),
                comment.getCreatedAt(),
                new UserResponse(
                        comment.getUser().getUserId(),
                        null, null, comment.getUser().getFullname(), null, null, null, null),
                comment.getProduct() != null
                        ? new CommentResponse.ProductInfo(comment.getProduct().getId(), comment.getProduct().getName())
                        : null,
                comment.getParent() != null ? comment.getParent().getId() : null);
        return ApiResponse.<CommentResponse>builder()
                .result(dto)
                .build();
    }

    @PutMapping("/edit")
    public ApiResponse<CommentResponse> editComment(@RequestParam Long commentId, @RequestBody CommentRequest request) {
        Comment comment = commentService.updateComment(commentId, request.getContent());
        CommentResponse dto = new CommentResponse(
                comment.getId(),
                comment.getContent(),
                comment.getCreatedAt(),
                new UserResponse(
                        comment.getUser().getUserId(),
                        null, null, comment.getUser().getFullname(), null, null, null, null),
                comment.getProduct() != null
                        ? new CommentResponse.ProductInfo(comment.getProduct().getId(), comment.getProduct().getName())
                        : null,
                comment.getParent() != null ? comment.getParent().getId() : null);
        return ApiResponse.<CommentResponse>builder()
                .result(dto)
                .build();
    }

    @DeleteMapping("/delete")
    public ApiResponse<Void> deleteComment(@RequestParam Long commentId) {
        commentService.deleteComment(commentId);
        return ApiResponse.<Void>builder().build();
    }
}