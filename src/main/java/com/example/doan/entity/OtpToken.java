package com.example.doan.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "otp_token", uniqueConstraints = {
        @UniqueConstraint(columnNames = "user_user_id")
})
public class OtpToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;

    private String otpCode;

    private Instant expiredAt;

    @OneToOne
    @JoinColumn(name = "user_user_id", referencedColumnName = "userId", unique = true)
    private User user;
}
