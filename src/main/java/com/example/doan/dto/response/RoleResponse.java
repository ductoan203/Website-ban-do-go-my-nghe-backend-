package com.example.doan.dto.response;

import com.example.doan.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleResponse {
    private Long roleId;
    private String name;
    private String description;

    public static RoleResponse fromRole(Role role) {
        return RoleResponse.builder()
                .roleId(role.getRoleId())
                .name(role.getName())
                .description(role.getDescription())
                .build();
    }
}