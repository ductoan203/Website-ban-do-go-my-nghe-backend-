package com.example.doan.repository;

import com.example.doan.entity.OtpToken;
import com.example.doan.entity.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {
    Optional<OtpToken> findByEmailAndOtpCode(String email, String otpCode);
    void deleteByEmail(String email);
    @Modifying
    @Transactional
    @Query("DELETE FROM OtpToken o WHERE o.user = :user")
    void deleteByUser(@Param("user") User user);


}
