package com.web_game.Authentication_Service.Service.ServiceImpl;

import com.web_game.Authentication_Service.Repository.RoleRepository;
import com.web_game.Authentication_Service.Repository.UserRepository;
import com.web_game.Authentication_Service.Repository.UserRoleRepository;
import com.web_game.common.Entity.Role;
import com.web_game.common.Entity.User;
import com.web_game.common.Entity.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service("customUserDetailsService")
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.info("Loading user by username: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("User not found: {}", username);
                    return new UsernameNotFoundException("User not found: " + username);
                });

        List<UserRole> userRoles = userRoleRepository.findByUserId(user.getUserId());
        Collection<? extends GrantedAuthority> authorities = userRoles.stream()
                .map(userRole -> {
                    Role role = roleRepository.findById(userRole.getRoleId())
                            .orElseThrow(() -> new RuntimeException("Role not found"));
                    return new SimpleGrantedAuthority("ROLE_" + role.getRoleName().name());
                })
                .collect(Collectors.toList());

        log.info("User {} loaded with authorities: {}", username, authorities);

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPasswordHash())
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(false)
                .build();
    }
}