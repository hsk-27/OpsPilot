package com.opspilot.identity.dto;

import com.opspilot.organization.dto.OrganizationDto;

public class AuthResponse {
    private String accessToken;
    private String tokenType = "Bearer";
    private long expiresInMs;
    private UserDto user;
    private OrganizationDto organization;

    public AuthResponse() {}

    public AuthResponse(String accessToken, long expiresInMs, UserDto user, OrganizationDto organization) {
        this.accessToken = accessToken;
        this.tokenType = "Bearer";
        this.expiresInMs = expiresInMs;
        this.user = user;
        this.organization = organization;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public long getExpiresInMs() {
        return expiresInMs;
    }

    public void setExpiresInMs(long expiresInMs) {
        this.expiresInMs = expiresInMs;
    }

    public UserDto getUser() {
        return user;
    }

    public void setUser(UserDto user) {
        this.user = user;
    }

    public OrganizationDto getOrganization() {
        return organization;
    }

    public void setOrganization(OrganizationDto organization) {
        this.organization = organization;
    }
}
