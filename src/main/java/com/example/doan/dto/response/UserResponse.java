package com.example.doan.dto.response;

import com.example.doan.entity.Role;
import com.example.doan.entity.User;
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
    //private String username;
    private String password;
    private String email;
    private String fullname;
    private String phoneNumber;
    private String address;
    private Boolean verified;
    //private String role; // Gắn với quyền (Role)
    private Boolean active;

    public static UserResponse fromEntity(User user) {
        return UserResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .fullname(user.getFullname())
                .phoneNumber(user.getPhoneNumber())
                .address(user.getAddress())
                .verified(Boolean.TRUE.equals(user.getIsVerified()))
                .active(Boolean.TRUE.equals(user.getIsActive()))
                .build();
    }


}
