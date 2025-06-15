package com.example.doan.controller;

import com.example.doan.dto.request.ContactMessageRequest;
import com.example.doan.dto.response.ContactMessageResponse;
import com.example.doan.dto.request.ContactReplyRequest;
import com.example.doan.service.ContactMessageService;
import com.example.doan.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/contact")
public class ContactMessageController {

    @Autowired
    private ContactMessageService contactMessageService;

    @Autowired
    private EmailService emailService;

    @PostMapping
    public ResponseEntity<ContactMessageResponse> createMessage(@RequestBody ContactMessageRequest request) {
        return ResponseEntity.ok(contactMessageService.createMessage(request));
    }

    @GetMapping
    public ResponseEntity<List<ContactMessageResponse>> getAllMessages() {
        return ResponseEntity.ok(contactMessageService.getAllMessages());
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<ContactMessageResponse> markAsRead(@PathVariable Long id) {
        return ResponseEntity.ok(contactMessageService.markAsRead(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMessage(@PathVariable Long id) {
        contactMessageService.deleteMessage(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reply")
    public ResponseEntity<Void> sendReply(@RequestBody ContactReplyRequest replyRequest) {
        emailService.sendContactReply(
                replyRequest.getRecipientEmail(),
                replyRequest.getRecipientName(),
                replyRequest.getReplyContent());
        contactMessageService.markAsReplied(replyRequest.getMessageId());
        return ResponseEntity.ok().build();
    }
}