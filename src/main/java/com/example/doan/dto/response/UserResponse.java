package com.example.doan.dto.response;

import com.example.doan.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserResponse {
    private Long userId;
    private String username;
    private String email;
    private String fullname;
    private String phoneNumber;
    private String address;
    private RoleResponse role;
    private Boolean isVerified;

    public static UserResponse fromUser(User user) {
        return UserResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullname(user.getFullname())
                .phoneNumber(user.getPhoneNumber())
                .address(user.getAddress())
                .role(user.getRole() != null ? RoleResponse.fromRole(user.getRole()) : null)
                .isVerified(user.getIsVerified())
                .build();
    }
}
