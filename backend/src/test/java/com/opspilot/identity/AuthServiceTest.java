package com.opspilot.identity;

import com.opspilot.common.security.JwtTokenProvider;
import com.opspilot.identity.dto.LoginRequest;
import com.opspilot.identity.dto.RegisterRequest;
import com.opspilot.identity.entity.Role;
import com.opspilot.identity.entity.User;
import com.opspilot.identity.repository.RefreshTokenSessionRepository;
import com.opspilot.identity.repository.UserRepository;
import com.opspilot.identity.service.AuthService;
import com.opspilot.organization.entity.Organization;
import com.opspilot.organization.entity.OrganizationMembership;
import com.opspilot.organization.repository.OrganizationMembershipRepository;
import com.opspilot.organization.repository.OrganizationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest {

    private UserRepository userRepository;
    private OrganizationRepository organizationRepository;
    private OrganizationMembershipRepository membershipRepository;
    private RefreshTokenSessionRepository refreshTokenRepository;
    private PasswordEncoder passwordEncoder;
    private JwtTokenProvider tokenProvider;
    private AuthService authService;

    private User foundUser;

    @BeforeEach
    void setUp() {
        foundUser = null;

        userRepository = (UserRepository) Proxy.newProxyInstance(
                UserRepository.class.getClassLoader(),
                new Class<?>[]{UserRepository.class},
                (proxy, method, args) -> {
                    if ("existsByEmailIgnoreCase".equals(method.getName())) return false;
                    if ("save".equals(method.getName())) {
                        User u = (User) args[0];
                        if (u.getId() == null) u.setId(UUID.randomUUID());
                        return u;
                    }
                    if ("findByEmailIgnoreCase".equals(method.getName())) return Optional.ofNullable(foundUser);
                    return null;
                }
        );

        organizationRepository = (OrganizationRepository) Proxy.newProxyInstance(
                OrganizationRepository.class.getClassLoader(),
                new Class<?>[]{OrganizationRepository.class},
                (proxy, method, args) -> {
                    if ("existsBySlug".equals(method.getName())) return false;
                    if ("save".equals(method.getName())) {
                        Organization org = (Organization) args[0];
                        if (org.getId() == null) org.setId(UUID.randomUUID());
                        return org;
                    }
                    if ("findById".equals(method.getName())) {
                        Organization o = new Organization("Enterprise Logistics", "enterprise-logistics");
                        o.setId((UUID) args[0]);
                        return Optional.of(o);
                    }
                    return null;
                }
        );

        membershipRepository = (OrganizationMembershipRepository) Proxy.newProxyInstance(
                OrganizationMembershipRepository.class.getClassLoader(),
                new Class<?>[]{OrganizationMembershipRepository.class},
                (proxy, method, args) -> {
                    if ("save".equals(method.getName())) return args[0];
                    if ("findByUserId".equals(method.getName())) {
                        UUID uId = (UUID) args[0];
                        OrganizationMembership m = new OrganizationMembership(UUID.randomUUID(), uId, Role.OWNER);
                        return List.of(m);
                    }
                    return null;
                }
        );

        refreshTokenRepository = (RefreshTokenSessionRepository) Proxy.newProxyInstance(
                RefreshTokenSessionRepository.class.getClassLoader(),
                new Class<?>[]{RefreshTokenSessionRepository.class},
                (proxy, method, args) -> args != null && args.length > 0 ? args[0] : null
        );

        passwordEncoder = new PasswordEncoder() {
            @Override
            public String encode(CharSequence rawPassword) {
                return "encoded_" + rawPassword;
            }

            @Override
            public boolean matches(CharSequence rawPassword, String encodedPassword) {
                return ("encoded_" + rawPassword).equals(encodedPassword) || "Password123!".equals(rawPassword);
            }
        };

        tokenProvider = new JwtTokenProvider(
                "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970",
                900000L,
                604800000L
        );

        authService = new AuthService(
                userRepository,
                organizationRepository,
                membershipRepository,
                refreshTokenRepository,
                passwordEncoder,
                tokenProvider
        );
    }

    @Test
    @DisplayName("User registration creates tenant organization and assigns OWNER role")
    void testRegistration() {
        RegisterRequest req = new RegisterRequest("jane@enterprise.com", "Password123!", "Jane", "Doe", "Enterprise Logistics");

        AuthService.AuthResult result = authService.register(req);

        assertNotNull(result.getResponse().getAccessToken());
        assertEquals("jane@enterprise.com", result.getResponse().getUser().getEmail());
        assertEquals(Role.OWNER, result.getResponse().getUser().getRole());
        assertEquals("Enterprise Logistics", result.getResponse().getOrganization().getName());
    }

    @Test
    @DisplayName("User login validates credentials and returns authenticated profile")
    void testLogin() {
        LoginRequest req = new LoginRequest("jane@enterprise.com", "Password123!");

        UUID userId = UUID.randomUUID();
        User user = new User("jane@enterprise.com", "encoded_Password123!", "Jane", "Doe");
        user.setId(userId);
        foundUser = user;

        AuthService.AuthResult result = authService.login(req);

        assertNotNull(result.getResponse().getAccessToken());
        assertEquals(Role.OWNER, result.getResponse().getUser().getRole());
    }

    @Test
    @DisplayName("Guest login authenticates default operational user without credentials")
    void testGuestLogin() {
        UUID userId = UUID.randomUUID();
        User adminUser = new User("admin@opspilot.io", "encoded_Password123!", "Eleanor", "Vance");
        adminUser.setId(userId);
        foundUser = adminUser;

        AuthService.AuthResult result = authService.guestLogin();

        assertNotNull(result.getResponse().getAccessToken());
        assertNotNull(result.getResponse().getUser());
        assertEquals("admin@opspilot.io", result.getResponse().getUser().getEmail());
        assertEquals(Role.OWNER, result.getResponse().getUser().getRole());
    }
}
