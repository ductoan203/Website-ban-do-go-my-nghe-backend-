package com.example.doan.controller;


import com.example.doan.dto.request.ApiResponse;
import com.example.doan.dto.request.AuthenticationRequest;
import com.example.doan.dto.request.IntrospectRequest;
import com.example.doan.dto.response.AuthenticationResponse;
import com.example.doan.dto.response.IntrospectRespone;
import com.example.doan.service.AuthenticatonService;

import com.nimbusds.jose.JOSEException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.ParseException;

@RestController
@RequestMapping("/auth")

public class AuthenticationController {
    @Autowired
    AuthenticatonService authenticatonService;

    @PostMapping("/login")
    ApiResponse<AuthenticationResponse> authenticate(@RequestBody AuthenticationRequest request) {
        var authenticated = authenticatonService.authenticate(request);
        return ApiResponse.<AuthenticationResponse>builder()
                .result(authenticated)
                .build();
    }

    @PostMapping("/introspect")
    ApiResponse<IntrospectRespone> authenticate(@RequestBody IntrospectRequest request)
            throws ParseException, JOSEException {
        var result = authenticatonService.introspect(request);
        return ApiResponse.<IntrospectRespone>builder()
                .result(result)
                .build();
    }
}