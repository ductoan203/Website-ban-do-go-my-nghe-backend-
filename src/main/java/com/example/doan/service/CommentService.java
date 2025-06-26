package com.example.doan.service;

import com.example.doan.entity.Comment;
import com.example.doan.entity.Product;
import com.example.doan.entity.User;
import com.example.doan.exception.AppException;
import com.example.doan.exception.ErrorCode;
import com.example.doan.repository.CommentRepository;
import com.example.doan.repository.ProductRepository;
import com.example.doan.repository.UserRepository;
import com.example.doan.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public List<Comment> getCommentsByProductId(Long productId) {
        List<Comment> comments = commentRepository.findByProductIdOrderByCreatedAtDesc(productId);
        for (Comment comment : comments) {
            System.out.println("Debug Comment: ID=" + comment.getId() + ", Content='" + comment.getContent()
                    + "', User=" + comment.getUser());
            if (comment.getUser() != null) {
                System.out.println("Debug User inside Comment: ID=" + comment.getUser().getUserId() + ", Fullname="
                        + comment.getUser().getFullname());
            }
        }
        return comments;
    }

    public Comment addCommentToProduct(Long productId, String content) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        Comment comment = Comment.builder()
                .product(product)
                .user(user)
                .content(content)
                .build();

        return commentRepository.save(comment);
    }

    // Update a comment
    public Comment updateComment(Long commentId, String newContent) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        if (currentUserId == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new AppException(ErrorCode.COMMENT_NOT_FOUND)); // Need COMMENT_NOT_FOUND error code

        // Check if the current user is the author of the comment
        if (!comment.getUser().getUserId().equals(currentUserId)) {
            throw new AppException(ErrorCode.ACCESS_DENIED); // Need ACCESS_DENIED error code
        }

        comment.setContent(newContent);
        return commentRepository.save(comment);
    }

    // Delete a comment
    public void deleteComment(Long commentId) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        if (currentUserId == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new AppException(ErrorCode.COMMENT_NOT_FOUND));

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Nếu là admin thì cho phép xóa bất kỳ bình luận nào
        if (currentUser.getRole() != null &&
                "ADMIN".equalsIgnoreCase(currentUser.getRole().getName())) {
            commentRepository.delete(comment);
            return;
        }

        // Nếu không phải admin, chỉ cho phép xóa bình luận của chính mình
        if (!comment.getUser().getUserId().equals(currentUserId)) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        commentRepository.delete(comment);
    }

    // Get all comments (for admin)
    public List<Comment> getAllComments() {
        return commentRepository.findAll();
    }

    // Admin reply to a comment
    public Comment replyToComment(Long productId, Long parentCommentId, String content) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
        Comment parent = commentRepository.findById(parentCommentId)
                .orElseThrow(() -> new AppException(ErrorCode.COMMENT_NOT_FOUND));
        Comment reply = Comment.builder()
                .product(product)
                .user(user)
                .content(content)
                .parent(parent)
                .build();
        return commentRepository.save(reply);
    }
}