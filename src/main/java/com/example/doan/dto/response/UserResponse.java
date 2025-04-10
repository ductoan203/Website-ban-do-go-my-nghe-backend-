package com.example.doan.dto.response;

import com.example.doan.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserResponse {
    private Long userId;
    private String username;
//    private String password;
    private String email;
    private String fullname;
    private String phoneNumber;
    private String address;
    private Set<String> role; // Gắn với quyền (Role)
}
