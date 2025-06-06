package com.example.doan.dto.response;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IntrospectRespone {
    private boolean valid;
    private String role;
}
