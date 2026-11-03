package com.opspilot.identity.controller;

import com.opspilot.common.response.ApiResponse;
import com.opspilot.common.security.UserPrincipal;
import com.opspilot.common.tenant.TenantContext;
import com.opspilot.identity.dto.AuthResponse;
import com.opspilot.identity.dto.LoginRequest;
import com.opspilot.identity.dto.RegisterRequest;
import com.opspilot.identity.dto.UserDto;
import com.opspilot.identity.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "User registration, login, refresh tokens, and session management")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register user and create tenant organization")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletResponse response) {
        AuthService.AuthResult result = authService.register(request);
        setRefreshCookie(response, result.getRefreshTokenRaw());
        return ResponseEntity.ok(ApiResponse.success("Registration successful", result.getResponse()));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user and issue JWT token")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {
        AuthService.AuthResult result = authService.login(request);
        setRefreshCookie(response, result.getRefreshTokenRaw());
        return ResponseEntity.ok(ApiResponse.success("Login successful", result.getResponse()));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rotate refresh token and issue new access token")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @RequestBody(required = false) Map<String, String> body,
            HttpServletRequest request,
            HttpServletResponse response) {
        String token = extractRefreshToken(request, body);
        AuthService.AuthResult result = authService.refreshToken(token);
        setRefreshCookie(response, result.getRefreshTokenRaw());
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", result.getResponse()));
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke active refresh token session")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestBody(required = false) Map<String, String> body,
            HttpServletRequest request,
            HttpServletResponse response) {
        String token = extractRefreshToken(request, body);
        authService.logout(token);
        clearRefreshCookie(response);
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user profile")
    public ResponseEntity<ApiResponse<UserDto>> getCurrentUser(
            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        UserDto user = authService.getCurrentUser(principal.getId(), TenantContext.getCurrentTenant());
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    private void setRefreshCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie("opspilot_refresh", refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // set to true in production with TLS
        cookie.setPath("/api/v1/auth");
        cookie.setMaxAge(7 * 24 * 60 * 60); // 7 days
        response.addCookie(cookie);
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("opspilot_refresh", "");
        cookie.setHttpOnly(true);
        cookie.setPath("/api/v1/auth");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    private String extractRefreshToken(HttpServletRequest request, Map<String, String> body) {
        if (request.getCookies() != null) {
            var cookieOpt = Arrays.stream(request.getCookies())
                    .filter(c -> "opspilot_refresh".equals(c.getName()))
                    .findFirst();
            if (cookieOpt.isPresent()) {
                return cookieOpt.get().getValue();
            }
        }
        if (body != null && body.containsKey("refreshToken")) {
            return body.get("refreshToken");
        }
        return null;
    }
}
