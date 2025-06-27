package com.example.doan.config;

import com.example.doan.enums.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

@EnableTransactionManagement
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${jwt.signerKey}")
    private String signerKey;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity.csrf(AbstractHttpConfigurer::disable);
        httpSecurity.cors(Customizer.withDefaults());

        httpSecurity.authorizeHttpRequests(request -> request
                // ✅ Public - không cần đăng nhập
                .requestMatchers(HttpMethod.POST, "/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/search/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/contact").permitAll()
                .requestMatchers(HttpMethod.GET, "/contact").permitAll()
                .requestMatchers(HttpMethod.GET, "/news/**").permitAll() // Cho phép public
                .requestMatchers(HttpMethod.POST, "/payment/payos/create").permitAll()
                .requestMatchers(HttpMethod.POST, "/doan/payment/payos/webhook").permitAll()
                .requestMatchers(HttpMethod.POST, "/payment/payos/webhook").permitAll()
                .requestMatchers(HttpMethod.POST, "/payment/payos/**").permitAll()
                .requestMatchers("/uploads/**").permitAll()
                .requestMatchers("doan/admin/dashboard/top-products").permitAll()
                .requestMatchers(HttpMethod.POST, "/payment/checkout").permitAll()
                .requestMatchers(HttpMethod.POST, "/payment/momo/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/payment/momo/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/payment/vnpay/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/payment/vnpay/**").permitAll()
                .requestMatchers("/doan/payment/vnpay/ipn").permitAll()
                .requestMatchers(HttpMethod.POST, "/admin/products/upload-image").permitAll()
                .requestMatchers(HttpMethod.GET, "/products/**", "/categories/**", "/doan/products/**",
                        "/doan/categories/**", "/auth/test-email")
                .permitAll()

                // ✅ Trang quản trị - cần ADMIN
                .requestMatchers("/admin/**").hasRole(Role.ADMIN.name())

                // ✅ Trang cá nhân người dùng
                .requestMatchers("/user/**", "/orders/checkout", "/orders/me")
                .hasAnyRole(Role.USER.name(), Role.ADMIN.name())

                // ✅ Mặc định: phải xác thực
                .anyRequest().authenticated()

        );

        httpSecurity.oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                        .decoder(jwtDecoder())
                        .jwtAuthenticationConverter(jwtAuthenticationConverter()))
                .authenticationEntryPoint(new JwtAuthenticationEntryPoint()));

        return httpSecurity.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");
        grantedAuthoritiesConverter.setAuthoritiesClaimName("roles"); // Đặt đúng tên claim chứa role

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        return jwtAuthenticationConverter;
    }

    @Bean
    JwtDecoder jwtDecoder() {
        SecretKeySpec secretKeySpec = new SecretKeySpec(signerKey.getBytes(), "HS512");
        return NimbusJwtDecoder
                .withSecretKey(secretKeySpec)
                .macAlgorithm(MacAlgorithm.HS512)
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
}