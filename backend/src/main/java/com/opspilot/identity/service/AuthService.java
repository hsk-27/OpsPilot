package com.opspilot.identity.service;

import com.opspilot.common.exception.ApiException;
import com.opspilot.common.exception.ResourceNotFoundException;
import com.opspilot.common.exception.UnauthorizedException;
import com.opspilot.common.security.JwtTokenProvider;
import com.opspilot.identity.dto.AuthResponse;
import com.opspilot.identity.dto.LoginRequest;
import com.opspilot.identity.dto.RegisterRequest;
import com.opspilot.identity.dto.UserDto;
import com.opspilot.identity.entity.RefreshTokenSession;
import com.opspilot.identity.entity.Role;
import com.opspilot.identity.entity.User;
import com.opspilot.identity.repository.RefreshTokenSessionRepository;
import com.opspilot.identity.repository.UserRepository;
import com.opspilot.organization.dto.OrganizationDto;
import com.opspilot.organization.entity.Organization;
import com.opspilot.organization.entity.OrganizationMembership;
import com.opspilot.organization.repository.OrganizationMembershipRepository;
import com.opspilot.organization.repository.OrganizationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationMembershipRepository membershipRepository;
    private final RefreshTokenSessionRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthService(
            UserRepository userRepository,
            OrganizationRepository organizationRepository,
            OrganizationMembershipRepository membershipRepository,
            RefreshTokenSessionRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.membershipRepository = membershipRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @Transactional
    public AuthResult register(RegisterRequest request) {
        String normalizedEmail = request.getEmail().toLowerCase().trim();
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ApiException("User already exists with email: " + normalizedEmail, HttpStatus.CONFLICT, "USER_ALREADY_EXISTS");
        }

        // 1. Create User
        User user = new User(
                normalizedEmail,
                passwordEncoder.encode(request.getPassword()),
                request.getFirstName().trim(),
                request.getLastName().trim()
        );
        user = userRepository.save(user);

        // 2. Create Organization
        String baseSlug = request.getOrganizationName().toLowerCase().replaceAll("[^a-z0-9]", "-").replaceAll("-+", "-");
        String slug = baseSlug;
        int counter = 1;
        while (organizationRepository.existsBySlug(slug)) {
            slug = baseSlug + "-" + counter++;
        }

        Organization org = new Organization(request.getOrganizationName().trim(), slug);
        org = organizationRepository.save(org);

        // 3. Create Membership as OWNER
        OrganizationMembership membership = new OrganizationMembership(org.getId(), user.getId(), Role.OWNER);
        membershipRepository.save(membership);

        // 4. Generate Tokens
        String accessToken = tokenProvider.generateAccessToken(user.getId(), user.getEmail(), org.getId(), Role.OWNER.name());
        String refreshTokenRaw = UUID.randomUUID().toString();
        createRefreshTokenSession(user.getId(), refreshTokenRaw);

        AuthResponse authResponse = new AuthResponse(
                accessToken,
                900000L,
                UserDto.from(user, Role.OWNER, org.getId()),
                OrganizationDto.from(org)
        );

        return new AuthResult(authResponse, refreshTokenRaw);
    }

    @Transactional
    public AuthResult login(LoginRequest request) {
        String normalizedEmail = request.getEmail().toLowerCase().trim();
        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password", "INVALID_CREDENTIALS"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password", "INVALID_CREDENTIALS");
        }

        List<OrganizationMembership> memberships = membershipRepository.findByUserId(user.getId());
        if (memberships.isEmpty()) {
            throw new ApiException("User has no organization memberships", HttpStatus.FORBIDDEN, "NO_ORGANIZATION_MEMBERSHIP");
        }

        OrganizationMembership selectedMembership;
        if (request.getOrganizationId() != null) {
            selectedMembership = memberships.stream()
                    .filter(m -> m.getOrganizationId().equals(request.getOrganizationId()))
                    .findFirst()
                    .orElseThrow(() -> new UnauthorizedException("User is not a member of the requested organization", "INVALID_ORGANIZATION"));
        } else {
            selectedMembership = memberships.get(0);
        }

        Organization org = organizationRepository.findById(selectedMembership.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Organization", selectedMembership.getOrganizationId()));

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        String accessToken = tokenProvider.generateAccessToken(
                user.getId(),
                user.getEmail(),
                org.getId(),
                selectedMembership.getRole().name()
        );
        String refreshTokenRaw = UUID.randomUUID().toString();
        createRefreshTokenSession(user.getId(), refreshTokenRaw);

        AuthResponse authResponse = new AuthResponse(
                accessToken,
                900000L,
                UserDto.from(user, selectedMembership.getRole(), org.getId()),
                OrganizationDto.from(org)
        );

        return new AuthResult(authResponse, refreshTokenRaw);
    }

    @Transactional
    public AuthResult guestLogin() {
        User user = userRepository.findByEmailIgnoreCase("admin@opspilot.io")
                .or(() -> userRepository.findByEmailIgnoreCase("guest@opspilot.io"))
                .orElseGet(() -> {
                    Organization org = organizationRepository.findAll().stream().findFirst()
                            .orElseGet(() -> organizationRepository.save(new Organization("Demo Organization", "demo-org")));
                    User newUser = userRepository.save(new User("guest@opspilot.io", passwordEncoder.encode("Guest123!"), "Guest", "Operator"));
                    membershipRepository.save(new OrganizationMembership(org.getId(), newUser.getId(), Role.OWNER));
                    return newUser;
                });

        List<OrganizationMembership> memberships = membershipRepository.findByUserId(user.getId());
        OrganizationMembership selectedMembership = memberships.isEmpty()
                ? membershipRepository.findAll().stream().findFirst().orElseThrow(() -> new ApiException("No organization found", HttpStatus.INTERNAL_SERVER_ERROR, "NO_ORGANIZATION"))
                : memberships.get(0);

        Organization org = organizationRepository.findById(selectedMembership.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Organization", selectedMembership.getOrganizationId()));

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        String accessToken = tokenProvider.generateAccessToken(
                user.getId(),
                user.getEmail(),
                org.getId(),
                selectedMembership.getRole().name()
        );
        String refreshTokenRaw = UUID.randomUUID().toString();
        createRefreshTokenSession(user.getId(), refreshTokenRaw);

        AuthResponse authResponse = new AuthResponse(
                accessToken,
                900000L,
                UserDto.from(user, selectedMembership.getRole(), org.getId()),
                OrganizationDto.from(org)
        );

        return new AuthResult(authResponse, refreshTokenRaw);
    }

    @Transactional
    public AuthResult refreshToken(String refreshTokenRaw) {
        if (refreshTokenRaw == null || refreshTokenRaw.isBlank()) {
            throw new UnauthorizedException("Refresh token is required", "MISSING_REFRESH_TOKEN");
        }

        String hash = hashToken(refreshTokenRaw);
        RefreshTokenSession session = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired refresh token", "INVALID_REFRESH_TOKEN"));

        if (!session.isActive()) {
            throw new UnauthorizedException("Refresh token is expired or revoked", "REFRESH_TOKEN_INACTIVE");
        }

        // Revoke old session (Rotation)
        session.setRevokedAt(Instant.now());
        refreshTokenRepository.save(session);

        User user = userRepository.findById(session.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", session.getUserId()));

        List<OrganizationMembership> memberships = membershipRepository.findByUserId(user.getId());
        if (memberships.isEmpty()) {
            throw new UnauthorizedException("No organization memberships found", "NO_MEMBERSHIP");
        }
        var membership = memberships.get(0);
        Organization org = organizationRepository.findById(membership.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Organization", membership.getOrganizationId()));

        String newAccessToken = tokenProvider.generateAccessToken(
                user.getId(),
                user.getEmail(),
                org.getId(),
                membership.getRole().name()
        );

        String newRefreshTokenRaw = UUID.randomUUID().toString();
        createRefreshTokenSession(user.getId(), newRefreshTokenRaw);

        AuthResponse authResponse = new AuthResponse(
                newAccessToken,
                900000L,
                UserDto.from(user, membership.getRole(), org.getId()),
                OrganizationDto.from(org)
        );

        return new AuthResult(authResponse, newRefreshTokenRaw);
    }

    @Transactional
    public void logout(String refreshTokenRaw) {
        if (refreshTokenRaw != null && !refreshTokenRaw.isBlank()) {
            String hash = hashToken(refreshTokenRaw);
            refreshTokenRepository.findByTokenHash(hash).ifPresent(session -> {
                session.setRevokedAt(Instant.now());
                refreshTokenRepository.save(session);
            });
        }
    }

    @Transactional(readOnly = true)
    public UserDto getCurrentUser(UUID userId, UUID organizationId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        Role role = Role.VIEWER;
        if (organizationId != null) {
            var membership = membershipRepository.findByOrganizationIdAndUserId(organizationId, userId);
            if (membership.isPresent()) {
                role = membership.get().getRole();
            }
        }

        return UserDto.from(user, role, organizationId);
    }

    private void createRefreshTokenSession(UUID userId, String rawToken) {
        String hash = hashToken(rawToken);
        Instant expiresAt = Instant.now().plusMillis(tokenProvider.getRefreshExpirationMs());
        RefreshTokenSession session = new RefreshTokenSession(userId, hash, expiresAt);
        refreshTokenRepository.save(session);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(encodedhash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    public static class AuthResult {
        private final AuthResponse response;
        private final String refreshTokenRaw;

        public AuthResult(AuthResponse response, String refreshTokenRaw) {
            this.response = response;
            this.refreshTokenRaw = refreshTokenRaw;
        }

        public AuthResponse getResponse() {
            return response;
        }

        public String getRefreshTokenRaw() {
            return refreshTokenRaw;
        }
    }
}
