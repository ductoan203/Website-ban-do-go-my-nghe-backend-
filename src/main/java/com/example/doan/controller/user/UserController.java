package com.example.doan.controller.user;

import com.example.doan.dto.request.ApiResponse;
import com.example.doan.dto.request.UserUpdateRequest;
import com.example.doan.dto.response.UserResponse;
import com.example.doan.entity.User;
import com.example.doan.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@PreAuthorize("hasRole('USER')")
@RequestMapping("/user")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ApiResponse<UserResponse> getMyInfo() {
        return ApiResponse.<UserResponse>builder()
                .result(userService.getMyInfo())
                .build();
    }

    @PutMapping
    public ApiResponse<User> updateMyInfo(@RequestBody UserUpdateRequest request) {
        return ApiResponse.<User>builder()
                .result(userService.updateMyInfo(request))
                .build();
    }

}
