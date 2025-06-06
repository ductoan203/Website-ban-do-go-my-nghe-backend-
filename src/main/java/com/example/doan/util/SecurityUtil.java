package com.example.doan.util;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

public class SecurityUtil {
    public static Long getCurrentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        Object principal = auth.getPrincipal();
        if (principal instanceof Jwt jwt) {
            Object idObj = jwt.getClaim("id");
            return idObj != null ? Long.parseLong(idObj.toString()) : null;
        }
        return null;
    }
}
