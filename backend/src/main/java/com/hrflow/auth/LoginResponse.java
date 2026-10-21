package com.hrflow.auth;

public record LoginResponse(
        String token,
        String tokenType,
        long expiresIn,
        UserInfo user
) {
    public record UserInfo(
            String email,
            String role
    ) {
    }
}