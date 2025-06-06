package com.example.doan.service;

import com.example.doan.dto.request.AuthenticationRequest;
import com.example.doan.dto.request.IntrospectRequest;
import com.example.doan.dto.response.AuthenticationResponse;
import com.example.doan.dto.response.IntrospectRespone;
import com.example.doan.entity.User;
import com.example.doan.exception.AppException;
import com.example.doan.exception.ErrorCode;
import com.example.doan.repository.UserRepository;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.StringJoiner;

@RequiredArgsConstructor
@Slf4j
@Service
public class AuthenticatonService {
    private final UserRepository userRepository;

    @NonFinal
    @Value("${jwt.signerKey}")
    protected String SIGNER_KEY ;
//
//    @NonFinal
//    public static final String SIGNER_KEY = "pHH1DDMlHM9yT4I/s9Fu2qneHTC4u+TaviCWjz7R9bMHezrkfehOK02PsZQwqCn5";



    public IntrospectRespone introspect(IntrospectRequest request)
            throws JOSEException, ParseException {
        var token = request.getToken();

        JWSVerifier verifier = new MACVerifier(SIGNER_KEY.getBytes());
        SignedJWT signedJWT = SignedJWT.parse(token);

        Date expirationTime = signedJWT.getJWTClaimsSet().getExpirationTime();
        boolean verified = signedJWT.verify(verifier);

        // ✅ Lấy role từ claim "roles" trong token
        String role = null;
        if (verified && expirationTime.after(new Date())) {
            var roles = (List<String>) signedJWT.getJWTClaimsSet().getClaim("roles");
            role = (roles != null && !roles.isEmpty()) ? roles.get(0) : null;
        }

        return IntrospectRespone.builder()
                .valid(verified && expirationTime.after(new Date()))
                .role(role)
                .build();
    }


    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Kiểm tra tài khoản có bị khóa không
//        if (!Boolean.TRUE.equals(user.getIsActive())) {
//            throw new AppException(ErrorCode.USER_IS_LOCKED);
//        }

        // Kiểm tra xác minh email cho USER
        if ("USER".equalsIgnoreCase(user.getRole().getName()) && !Boolean.TRUE.equals(user.getIsVerified())) {
            throw new AppException(ErrorCode.USER_NOT_VERIFIED);
        }

        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);
        boolean authenticated = passwordEncoder.matches(request.getPassword(), user.getPassword());

        if (!authenticated) {
            throw new AppException(ErrorCode.INVALID_PASSWORD);
        }

        var token = generateToken(user);

        return AuthenticationResponse.builder()
                .token(token)
                .authenticated(true)
                .build();
    }


    private String generateToken(User user) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);

        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(user.getEmail()) // Sử dụng email làm subject
                .issuer("example.com")
                .issueTime(new Date())
                .expirationTime(new Date(
                        Instant.now().plus(1, ChronoUnit.HOURS).toEpochMilli()
                ))
                .claim("id", user.getUserId()) // Thêm id
                .claim("name", user.getFullname()) // Thêm name
                .claim("email", user.getEmail()) // Thêm email
                .claim("roles", Collections.singletonList(user.getRole().getName()))
                .build();

        Payload payload = new Payload(jwtClaimsSet.toJSONObject());

        JWSObject jwsObject = new  JWSObject(header, payload);

        try {
            jwsObject.sign(new MACSigner(SIGNER_KEY.getBytes()));
            return jwsObject.serialize();
        } catch (JOSEException e) {
            log.error("Cannot create token", e);
            throw new RuntimeException(e);
        }
    }
    private String buildScope(User user){
        if (user.getRole() != null){
            return user.getRole().getName();
        }
        return "";

    }
}
