package com.web_game.common.Config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class CustomAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String userId = request.getHeader("X-UserId");
        String username = request.getHeader("X-Username");
        String roles = request.getHeader("X-Roles");

        log.info("Headers received - UserId: {}, Username: {}, Roles: {}", userId, username, roles);

        if (username != null && userId != null && roles != null) {
            try {
                // Parse roles
                List<SimpleGrantedAuthority> authorities = Arrays.stream(roles.split(","))
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role.trim()))
                        .collect(Collectors.toList());

                // Create custom authentication token
                CustomAuthenticationToken authentication = new CustomAuthenticationToken(
                        username, null, authorities, Long.parseLong(userId));

                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.info("Authentication set successfully for user: {} with authorities: {}", username, authorities);

            } catch (Exception e) {
                log.error("Error creating authentication: ", e);
            }
        } else {
            log.warn("Missing required headers - proceeding without authentication");
        }

        filterChain.doFilter(request, response);
    }
}