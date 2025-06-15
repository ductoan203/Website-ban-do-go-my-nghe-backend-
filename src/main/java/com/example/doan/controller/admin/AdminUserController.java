package com.example.doan.controller.admin;

import com.example.doan.dto.request.ApiResponse;
import com.example.doan.dto.request.UserCreateRequest;
import com.example.doan.dto.request.UserUpdateRequest;
import com.example.doan.dto.response.UserResponse;
import com.example.doan.entity.User;
import com.example.doan.service.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/admin/user")
// @PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;

    @Autowired
    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    ApiResponse<User> createUser(@RequestBody @Valid UserCreateRequest request) {
        return ApiResponse.<User>builder()
                .result(userService.createCustomer(request))
                .build();
    }

    @PostMapping("/customers")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserResponse> createCustomer(@RequestBody @Valid UserCreateRequest request) {
        User newUser = userService.createCustomer(request);
        return ApiResponse.<UserResponse>builder()
                .result(UserResponse.fromUser(newUser))
                .build();
    }

    @GetMapping("/allUsers")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<UserResponse>> getAllUsers() {
        return ApiResponse.<List<UserResponse>>builder()
                .result(userService.getAllUsers(null).stream()
                        .map(UserResponse::fromUser)
                        .collect(Collectors.toList()))
                .build();
    }

    @GetMapping("/customers")
    public ApiResponse<List<UserResponse>> getAllCustomers(@RequestParam(required = false) String searchTerm) {
        List<UserResponse> users = userService.getAllUsers(searchTerm).stream()
                .map(UserResponse::fromUser)
                .filter(userResponse -> userResponse.getRole() != null
                        && "USER".equalsIgnoreCase(userResponse.getRole().getName()))
                .collect(Collectors.toList());

        return ApiResponse.<List<UserResponse>>builder()
                .result(users)
                .build();
    }

    @GetMapping("/{userId}")
    public ApiResponse<User> getUserById(@PathVariable Long userId) {
        return ApiResponse.<User>builder()
                .result(userService.getUserById(userId))
                .build();
    }

    // @GetMapping("/myInfo")
    // ApiResponse<UserResponse> getMyInfo() {
    // return ApiResponse.<UserResponse>builder()
    // .result(userService.getMyInfo())
    // .build();
    // }

    @PutMapping("/{userId}")
    public ApiResponse<User> updateUser(@PathVariable Long userId, @RequestBody UserUpdateRequest request) {
        return ApiResponse.<User>builder()
                .result(userService.updateUserByAdmin(userId, request))
                .build();
    }

    @DeleteMapping("{userId}")
    public ApiResponse<String> deleteUser(@PathVariable Long userId) {
        userService.deleteUser(userId);
        return ApiResponse.<String>builder()
                .result("User deleted successfully")
                .build();
    }
}
