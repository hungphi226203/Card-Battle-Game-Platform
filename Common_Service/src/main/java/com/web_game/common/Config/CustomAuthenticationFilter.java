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

    private static final List<String> EXCLUDED_PATHS = Arrays.asList(
            "/auth/login", "/auth/register", "/auth/logout",
            "/auth/forgot-password", "/auth/reset-password", "/auth/verify"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        // Skip filtering for public endpoints
        if (EXCLUDED_PATHS.stream().anyMatch(path::startsWith)) {
            log.debug("Bypassing filter for public path: {}", path);
            filterChain.doFilter(request, response);
            return;
        }

        String userId = request.getHeader("X-UserId");
        String username = request.getHeader("X-Username");
        String roles = request.getHeader("X-Roles");

        log.debug("Headers received - UserId: {}, Username: {}, Roles: {}", userId, username, roles);

        if (username != null && userId != null && roles != null) {
            try {
                List<SimpleGrantedAuthority> authorities = Arrays.stream(roles.split(","))
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role.trim()))
                        .collect(Collectors.toList());

                CustomAuthenticationToken authentication = new CustomAuthenticationToken(
                        username, null, authorities, Long.parseLong(userId));

                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("Authentication set successfully for user: {} with authorities: {}", username, authorities);

            } catch (NumberFormatException e) {
                log.error("Invalid userId format: {}", userId);
            } catch (Exception e) {
                log.error("Error creating authentication: ", e);
            }
        } else {
            log.debug("Missing required headers for path: {} - proceeding without authentication", path);
        }

        filterChain.doFilter(request, response);
    }
}