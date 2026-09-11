package cn.jongwong.controller;

import cn.jongwong.dto.ApiResponse;
import cn.jongwong.dto.AuthResponse;
import cn.jongwong.dto.ForgotPasswordRequest;
import cn.jongwong.dto.LoginRequest;
import cn.jongwong.dto.RegisterRequest;
import cn.jongwong.dto.RegisterResponse;
import cn.jongwong.dto.RefreshTokenRequest;
import cn.jongwong.dto.ResendVerificationRequest;
import cn.jongwong.dto.ResetPasswordRequest;
import cn.jongwong.dto.UserDTO;
import cn.jongwong.dto.VerifyEmailRequest;
import cn.jongwong.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentication", description = "Authentication API endpoints")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "User login", description = "Authenticate user and return JWT token")
    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        return ApiResponse.ok("Login successful", authService.login(loginRequest));
    }

    @Operation(summary = "User registration", description = "Register a new user and send an email verification code")
    @PostMapping("/register")
    public ApiResponse<RegisterResponse> register(@Valid @RequestBody RegisterRequest registerRequest) {
        RegisterResponse result = authService.register(registerRequest);
        return ApiResponse.ok(
                result.isRequiresVerification() ? "Verification email sent" : "Registered",
                result);
    }

    @Operation(summary = "Verify email", description = "Activate a new account with the emailed code")
    @PostMapping("/verify-email")
    public ApiResponse<AuthResponse> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        return ApiResponse.ok("Email verified", authService.verifyEmail(request.getEmail(), request.getCode()));
    }

    @Operation(summary = "Resend verification email")
    @PostMapping("/resend-verification")
    public ApiResponse<Void> resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        authService.resendVerification(request.getEmail());
        return ApiResponse.ok("If the account needs verification, a new code was sent", null);
    }

    @Operation(summary = "Refresh token", description = "Get a new access token using refresh token")
    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.ok("Token refreshed", authService.refreshToken(request.getRefreshToken()));
    }

    @Operation(summary = "Logout", description = "Logout current user and revoke refresh token")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.getRefreshToken());
        return ApiResponse.ok("Logout successful", null);
    }

    @Operation(summary = "Get current user", description = "Return current authenticated user info")
    @GetMapping("/me")
    public ApiResponse<UserDTO> getCurrentUser() {
        return ApiResponse.ok("Current user retrieved", authService.getCurrentUser());
    }

    @Operation(summary = "Forgot password", description = "Send password reset instructions to email (placeholder implementation)")
    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.initiatePasswordReset(request.getEmail());
        return ApiResponse.ok("Password reset instructions sent if email exists", null);
    }

    @Operation(summary = "Reset password", description = "Reset password using token (placeholder implementation)")
    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.getToken(), request.getNewPassword());
        return ApiResponse.ok("Password reset successful", null);
    }
}
