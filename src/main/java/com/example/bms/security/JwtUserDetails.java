package com.example.bms.security;

public class JwtUserDetails {
    private String username;
    private Long userId;

    public JwtUserDetails(String username, Long userId) {
        this.username = username;
        this.userId = userId;
    }

    public String getUsername() { return username; }
    public Long getUserId() { return userId; }
}
