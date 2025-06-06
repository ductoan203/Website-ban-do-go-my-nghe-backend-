package com.example.doan.service;

import com.example.doan.dto.request.UserCreateRequest;
import com.example.doan.dto.request.UserUpdateRequest;
import com.example.doan.dto.response.UserResponse;
import com.example.doan.entity.OtpToken;
import com.example.doan.entity.Role;
import com.example.doan.entity.User;
import com.example.doan.exception.AppException;
import com.example.doan.exception.ErrorCode;
import com.example.doan.repository.OtpTokenRepository;
import com.example.doan.repository.RoleRepository;
import com.example.doan.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    private final PasswordEncoder passwordEncoder;
    private final OtpTokenRepository otpTokenRepository;
    private final OtpService otpService;

    public User createUser(UserCreateRequest request) {
        // Kiểm tra email đã tồn tại
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_EXISTS);
        }

        // Lấy role USER
        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));

        // Tạo user mới
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullname(request.getFullname())
                .phoneNumber(request.getPhoneNumber())
                .address(request.getAddress())
                .role(userRole)
                .isActive(true)
                .isVerified(false)
                .build();

        // Lưu user
        User savedUser = userRepository.save(user);

        // Gửi OTP xác minh
        otpService.sendOtpToUser(savedUser);

        return savedUser;
    }

    @PreAuthorize("hasRole('ADMIN')")
    public List<User> getAllUsers(@RequestParam(required = false) String searchTerm) {
        log.info("In method get Users with searchTerm: {}", searchTerm);
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            // Nếu có searchTerm, gọi phương thức tìm kiếm mới
            return userRepository.findByFullnameOrEmailOrPhoneNumberContainingIgnoreCase(searchTerm);
        } else {
            // Nếu không có searchTerm, trả về tất cả người dùng
            return userRepository.findAll();
        }
    }

    @PreAuthorize("returnObject.username == authentication.name")
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found!"));
    }

    public UserResponse getMyInfo() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        return UserResponse.fromEntity(user);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public User updateUserByAdmin(Long userId, UserUpdateRequest request) {
        User user = getUserById(userId);
        return updateUserFields(user, request);
    }

    // USER chỉ được sửa chính mình
    public User updateMyInfo(UserUpdateRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        return updateUserFields(user, request);
    }

    private User updateUserFields(User user, UserUpdateRequest request) {
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        user.setFullname(request.getFullname());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setAddress(request.getAddress());

        return userRepository.save(user);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteUser(Long userId) {
        User user = getUserById(userId);
        otpTokenRepository.deleteByUser(user);
        userRepository.delete(user);
        log.info("Deleted user with ID: {}", userId);
    }

}