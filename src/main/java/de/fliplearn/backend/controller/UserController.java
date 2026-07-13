package de.fliplearn.backend.controller;

import de.fliplearn.backend.dto.CurrentUserResponse;
import de.fliplearn.backend.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.fliplearn.backend.dto.ChangePasswordRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import de.fliplearn.backend.dto.UserProfileResponse;
import de.fliplearn.backend.dto.UpdateProfileRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import de.fliplearn.backend.dto.ChangeEmailRequest;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(
            UserService userService
    ) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public CurrentUserResponse getCurrentUser(
            Authentication authentication
    ) {
        return userService.getCurrentUser(
                authentication.getName()
        );
    }

    @GetMapping("/profile")
    public UserProfileResponse getProfile(
            Authentication authentication
    ) {
        return userService.getProfile(
                authentication.getName()
        );
    }

    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(
            @Valid
            @RequestBody
            ChangePasswordRequest request,
            Authentication authentication
    ) {
        userService.changePassword(
                authentication.getName(),
                request
        );
    }

    @PutMapping("/profile")
    public CurrentUserResponse updateProfile(
            @Valid
            @RequestBody
            UpdateProfileRequest request,
            Authentication authentication
    ) {
        return userService.updateProfile(
                authentication.getName(),
                request
        );
    }

    @PutMapping("/email")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changeEmail(
            @Valid
            @RequestBody
            ChangeEmailRequest request,
            Authentication authentication
    ) {
        userService.changeEmail(
                authentication.getName(),
                request
        );
    }
}