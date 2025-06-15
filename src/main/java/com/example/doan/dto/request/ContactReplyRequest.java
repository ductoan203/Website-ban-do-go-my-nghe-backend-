package com.example.doan.dto.request;

import lombok.Data;

@Data
public class ContactReplyRequest {
    private String recipientEmail;
    private String recipientName;
    private String replyContent;
    private Long messageId;

}