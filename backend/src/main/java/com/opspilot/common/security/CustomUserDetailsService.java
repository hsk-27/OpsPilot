package com.opspilot.common.security;

import com.opspilot.identity.entity.User;
import com.opspilot.identity.repository.UserRepository;
import com.opspilot.organization.entity.OrganizationMembership;
import com.opspilot.organization.repository.OrganizationMembershipRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final OrganizationMembershipRepository membershipRepository;

    public CustomUserDetailsService(UserRepository userRepository, OrganizationMembershipRepository membershipRepository) {
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        List<OrganizationMembership> memberships = membershipRepository.findByUserId(user.getId());
        UUID defaultOrgId = memberships.isEmpty() ? null : memberships.get(0).getOrganizationId();
        var defaultRole = memberships.isEmpty() ? null : memberships.get(0).getRole();

        return UserPrincipal.create(user, defaultOrgId, defaultRole);
    }

    @Transactional(readOnly = true)
    public UserDetails loadUserByIdAndOrg(UUID userId, UUID organizationId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));

        var membership = membershipRepository.findByOrganizationIdAndUserId(organizationId, userId)
                .orElse(null);

        var role = membership != null ? membership.getRole() : null;
        return UserPrincipal.create(user, organizationId, role);
    }
}
