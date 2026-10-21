package com.hrflow.auth;

import com.hrflow.security.CurrentUserService;

import jakarta.validation.Valid;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.authentication.DisabledException;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final CurrentUserService currentUserService;
    private final UserAccountRepository userRepository;

    public AuthController(
            AuthService authService,
            CurrentUserService currentUserService,
            UserAccountRepository userRepository) {
        this.authService = authService;
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        try {
            return authService.login(request);
        } catch (DisabledException exception) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Thông tin đăng nhập không hợp lệ");
        } catch (AuthenticationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Thông tin đăng nhập không hợp lệ");
        }
    }

    @GetMapping("/me")
    public Map<String, String> me() {
        String email = currentUserService.getCurrentEmail();

        UserAccount account = userRepository
                .findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED));

        return Map.of(
                "email", account.getEmail(),
                "role", account.getRole());
    }
}