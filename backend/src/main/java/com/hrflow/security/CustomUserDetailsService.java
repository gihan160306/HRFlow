package com.hrflow.security;

import com.hrflow.auth.UserAccount;
import com.hrflow.auth.UserAccountRepository;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserAccountRepository userRepository;

    public CustomUserDetailsService(
            UserAccountRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        UserAccount account = userRepository
                .findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Không tìm thấy tài khoản: " + email));

        return new AuthUser(
                account.getId(),
                account.getEmployeeId(),
                account.getEmail(),
                account.getPasswordHash(),
                account.getRole(),
                account.isActive()
        );
    }
}