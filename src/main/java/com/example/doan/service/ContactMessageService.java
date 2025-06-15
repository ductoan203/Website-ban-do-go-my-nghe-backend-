package com.example.doan.service;

import com.example.doan.dto.request.ContactMessageRequest;
import com.example.doan.dto.response.ContactMessageResponse;
import com.example.doan.entity.ContactMessage;
import com.example.doan.repository.ContactMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ContactMessageService {

    @Autowired
    private ContactMessageRepository contactMessageRepository;

    public ContactMessageResponse createMessage(ContactMessageRequest request) {
        ContactMessage message = new ContactMessage();
        message.setName(request.getName());
        message.setEmail(request.getEmail());
        message.setMessage(request.getMessage());

        ContactMessage savedMessage = contactMessageRepository.save(message);
        return convertToResponse(savedMessage);
    }

    public List<ContactMessageResponse> getAllMessages() {
        return contactMessageRepository.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ContactMessageResponse markAsRead(Long id) {
        ContactMessage message = contactMessageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Message not found"));
        message.setRead(true);
        return convertToResponse(contactMessageRepository.save(message));
    }

    @Transactional
    public void markAsReplied(Long id) {
        ContactMessage message = contactMessageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Message not found"));
        message.setReplied(true);
        contactMessageRepository.save(message);
    }

    public void deleteMessage(Long id) {
        if (!contactMessageRepository.existsById(id)) {
            throw new RuntimeException("Message not found");
        }
        contactMessageRepository.deleteById(id);
    }

    private ContactMessageResponse convertToResponse(ContactMessage message) {
        ContactMessageResponse response = new ContactMessageResponse();
        response.setId(message.getId());
        response.setName(message.getName());
        response.setEmail(message.getEmail());
        response.setMessage(message.getMessage());
        response.setCreatedAt(message.getCreatedAt().toString());
        response.setRead(message.isRead());
        response.setReplied(message.isReplied());
        return response;
    }
}