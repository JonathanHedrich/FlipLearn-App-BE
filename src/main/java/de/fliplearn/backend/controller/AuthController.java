package de.fliplearn.backend.controller;

import de.fliplearn.backend.dto.GoogleLoginRequest;
import de.fliplearn.backend.dto.LoginRequest;
import de.fliplearn.backend.dto.LoginResponse;
import de.fliplearn.backend.dto.RegisterRequest;
import de.fliplearn.backend.dto.RegisterResponse;
import de.fliplearn.backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(
            @Valid @RequestBody RegisterRequest request
    ) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public LoginResponse login(
            @Valid @RequestBody LoginRequest request
    ) {
        return authService.login(request);
    }

    @PostMapping("/google")
    public LoginResponse googleLogin(
            @Valid @RequestBody GoogleLoginRequest request
    ) {
        return authService.googleLogin(request);
    }
}