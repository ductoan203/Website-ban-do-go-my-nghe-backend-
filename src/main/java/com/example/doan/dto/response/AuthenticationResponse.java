package com.example.doan.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class AuthenticationResponse {

    String token;

    private boolean authenticated;












    // Private constructor to enforce the use of the builder
//    private AuthenticationResponse(Builder builder) {
//        this.authenticated = builder.authenticated;
//    }
//
//    // Static inner Builder class
//    public static class Builder {
//        private boolean authenticated;
//        private String token;
//
//        public Builder authenticated(boolean authenticated) {
//            this.authenticated = authenticated;
//            return this;
//        }
//
//        public AuthenticationResponse build() {
//            return new AuthenticationResponse(this);
//        }
//
//        public Builder token(String token) {
//            this.token = token;
//            return this;
//        }
//
//
//    }
//
//    // Static method to get a new Builder instance
//    public static Builder builder() {
//        return new Builder();
//    }
//
//    // Getter for authenticated
//    public boolean isAuthenticated() {
//        return authenticated;
//    }
//
//    public String getToken() {
//        return token;
//    }
//
//    public void setToken(String token) {
//        this.token = token;
//    }
//
//    public void setAuthenticated(boolean authenticated) {
//        this.authenticated = authenticated;
//    }
}

