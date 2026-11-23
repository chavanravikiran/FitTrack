package com.fittrack.auth.controller;

import com.fittrack.auth.RefreshCookieFactory;
import com.fittrack.auth.dto.AuthDtos.AuthResponse;
import com.fittrack.auth.dto.AuthDtos.AuthResult;
import com.fittrack.auth.dto.AuthDtos.ChangePasswordRequest;
import com.fittrack.auth.dto.AuthDtos.ForgotPasswordRequest;
import com.fittrack.auth.dto.AuthDtos.LoginRequest;
import com.fittrack.auth.dto.AuthDtos.RegisterRequest;
import com.fittrack.auth.dto.AuthDtos.ResetPasswordRequest;
import com.fittrack.auth.dto.AuthDtos.UserSummary;
import com.fittrack.auth.dto.AuthDtos.VerifyEmailRequest;
import com.fittrack.auth.service.AuthService;
import com.fittrack.auth.service.EmailVerificationService;
import com.fittrack.auth.service.PasswordResetService;
import com.fittrack.common.api.ApiResponse;
import com.fittrack.common.exception.UnauthorizedException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentication")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final EmailVerificationService emailVerificationService;
    private final RefreshCookieFactory cookieFactory;

    @Operation(summary = "Create an account and sign in")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent) {
        AuthResult result = authService.register(request, userAgent);
        return withCookie(HttpStatus.CREATED, result, "Account created");
    }

    @Operation(summary = "Sign in with email and password")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent) {
        return withCookie(HttpStatus.OK, authService.login(request, userAgent), "Signed in");
    }

    @Operation(summary = "Exchange the refresh cookie for a new access token (rotates the cookie)")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @CookieValue(name = RefreshCookieFactory.COOKIE_NAME, required = false) String refreshToken,
            @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new UnauthorizedException("No active session");
        }
        try {
            return withCookie(HttpStatus.OK, authService.refresh(refreshToken, userAgent), null);
        } catch (UnauthorizedException e) {
            // Clear the dead cookie so the browser stops sending it.
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .header(HttpHeaders.SET_COOKIE, cookieFactory.clear().toString())
                    .body(ApiResponse.error(e.getMessage(), null, "/api/v1/auth/refresh"));
        }
    }

    @Operation(summary = "Sign out (revokes the refresh token)")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @CookieValue(name = RefreshCookieFactory.COOKIE_NAME, required = false) String refreshToken) {
        authService.logout(refreshToken);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieFactory.clear().toString())
                .body(ApiResponse.message("Signed out"));
    }

    @Operation(summary = "Request a password reset email")
    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestReset(request.email());
        return ApiResponse.message("If an account exists for that email, a reset link has been sent.");
    }

    @Operation(summary = "Set a new password using a reset token")
    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.token(), request.newPassword());
        return ApiResponse.message("Password updated. Please sign in with your new password.");
    }

    @Operation(summary = "Change password for the signed-in user (signs out other devices)")
    @PutMapping("/change-password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return ApiResponse.message("Password changed");
    }

    @Operation(summary = "Confirm an email address using the token from the verification email")
    @PostMapping("/verify-email")
    public ApiResponse<Void> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        emailVerificationService.verify(request.token());
        return ApiResponse.message("Email verified. Thank you!");
    }

    @Operation(summary = "Send a new verification email to the signed-in user")
    @PostMapping("/resend-verification")
    public ApiResponse<Void> resendVerification() {
        emailVerificationService.resendForCurrentUser();
        return ApiResponse.message("Verification email sent. Please check your inbox.");
    }

    @Operation(summary = "Current signed-in user")
    @GetMapping("/me")
    public ApiResponse<UserSummary> me() {
        return ApiResponse.ok(authService.currentUser());
    }

    private ResponseEntity<ApiResponse<AuthResponse>> withCookie(HttpStatus status, AuthResult result, String message) {
        return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE, cookieFactory.create(result.refreshToken()).toString())
                .body(ApiResponse.ok(result.response(), message));
    }
}
