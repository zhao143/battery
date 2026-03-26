package com.example.bms.security;

import com.example.bms.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        System.out.println("=== Request: " + request.getMethod() + " " + request.getRequestURI() + ", Auth: " + authHeader);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            boolean valid = jwtUtil.validateToken(token);
            System.out.println("=== Token valid: " + valid);
            if (valid) {
                System.out.println("=== Token valid, username from token: " + jwtUtil.getUsernameFromToken(token));
                String username = jwtUtil.getUsernameFromToken(token);
                Long userId = jwtUtil.getUserIdFromToken(token);
                
                List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                
                if ("admin".equals(username)) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                    authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
                } else {
                    authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
                }
                
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        new JwtUserDetails(username, userId), null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);
                System.out.println("=== SecurityContext set: " + SecurityContextHolder.getContext().getAuthentication());
            }
        } else {
            System.out.println("=== No Bearer token found");
        }
        filterChain.doFilter(request, response);
    }
}
