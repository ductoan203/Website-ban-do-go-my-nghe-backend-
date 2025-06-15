package com.example.doan.dto.response;

import com.example.doan.entity.ContactMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactMessageResponse {
    private Long id;
    private String name;
    private String email;
    private String message;
    private String createdAt;
    private boolean isRead;
    private boolean isReplied;

    public static ContactMessageResponse convertToResponse(ContactMessage message) {
        ContactMessageResponse response = new ContactMessageResponse();
        response.setId(message.getId());
        response.setName(message.getName());
        response.setEmail(message.getEmail());
        response.setMessage(message.getMessage());
        response.setCreatedAt(message.getCreatedAt().toString()); // Convert LocalDateTime to String
        response.setRead(message.isRead());
        response.setReplied(message.isReplied());
        return response;
    }
}