package com.opspilot.organization.service;

import com.opspilot.common.exception.ApiException;
import com.opspilot.common.exception.ResourceNotFoundException;
import com.opspilot.common.tenant.TenantContext;
import com.opspilot.identity.entity.Role;
import com.opspilot.identity.entity.User;
import com.opspilot.identity.repository.UserRepository;
import com.opspilot.organization.dto.InviteMemberRequest;
import com.opspilot.organization.dto.MembershipDto;
import com.opspilot.organization.dto.OrganizationDto;
import com.opspilot.organization.dto.UpdateRoleRequest;
import com.opspilot.organization.entity.Organization;
import com.opspilot.organization.entity.OrganizationMembership;
import com.opspilot.organization.repository.OrganizationMembershipRepository;
import com.opspilot.organization.repository.OrganizationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public OrganizationService(
            OrganizationRepository organizationRepository,
            OrganizationMembershipRepository membershipRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.organizationRepository = organizationRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public OrganizationDto getCurrentOrganization() {
        UUID orgId = TenantContext.getCurrentTenant();
        if (orgId == null) {
            throw new ApiException("Tenant context not resolved", HttpStatus.FORBIDDEN, "TENANT_NOT_RESOLVED");
        }
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", orgId));
        return OrganizationDto.from(org);
    }

    @Transactional
    public OrganizationDto updateCurrentOrganization(OrganizationDto update) {
        UUID orgId = TenantContext.getCurrentTenant();
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", orgId));

        if (update.getName() != null && !update.getName().isBlank()) {
            org.setName(update.getName().trim());
        }
        if (update.getDefaultCurrency() != null && !update.getDefaultCurrency().isBlank()) {
            org.setDefaultCurrency(update.getDefaultCurrency().toUpperCase().trim());
        }
        if (update.getTimezone() != null && !update.getTimezone().isBlank()) {
            org.setTimezone(update.getTimezone().trim());
        }

        org = organizationRepository.save(org);
        return OrganizationDto.from(org);
    }

    @Transactional(readOnly = true)
    public List<MembershipDto> getMembers() {
        UUID orgId = TenantContext.getCurrentTenant();
        List<OrganizationMembership> memberships = membershipRepository.findByOrganizationId(orgId);
        List<MembershipDto> dtos = new ArrayList<>();

        for (OrganizationMembership m : memberships) {
            User user = userRepository.findById(m.getUserId()).orElse(null);
            String email = user != null ? user.getEmail() : "unknown";
            String name = user != null ? (user.getFirstName() + " " + user.getLastName()) : "unknown";
            dtos.add(new MembershipDto(
                    m.getId(),
                    m.getOrganizationId(),
                    m.getUserId(),
                    email,
                    name,
                    m.getRole(),
                    m.getStatus(),
                    m.getCreatedAt()
            ));
        }
        return dtos;
    }

    @Transactional
    public MembershipDto inviteMember(InviteMemberRequest request) {
        UUID orgId = TenantContext.getCurrentTenant();
        String email = request.getEmail().toLowerCase().trim();

        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user == null) {
            user = new User(
                    email,
                    passwordEncoder.encode(UUID.randomUUID().toString().substring(0, 12)),
                    request.getFirstName().trim(),
                    request.getLastName().trim()
            );
            user = userRepository.save(user);
        }

        if (membershipRepository.existsByOrganizationIdAndUserId(orgId, user.getId())) {
            throw new ApiException("User is already a member of this organization", HttpStatus.CONFLICT, "MEMBER_ALREADY_EXISTS");
        }

        OrganizationMembership membership = new OrganizationMembership(orgId, user.getId(), request.getRole());
        membership = membershipRepository.save(membership);

        return new MembershipDto(
                membership.getId(),
                membership.getOrganizationId(),
                user.getId(),
                user.getEmail(),
                user.getFirstName() + " " + user.getLastName(),
                membership.getRole(),
                membership.getStatus(),
                membership.getCreatedAt()
        );
    }

    @Transactional
    public MembershipDto updateMemberRole(UUID membershipId, UpdateRoleRequest request) {
        UUID orgId = TenantContext.getCurrentTenant();
        OrganizationMembership membership = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new ResourceNotFoundException("Membership", membershipId));

        if (!membership.getOrganizationId().equals(orgId)) {
            throw new ApiException("Membership does not belong to active organization", HttpStatus.FORBIDDEN, "CROSS_TENANT_ACCESS_DENIED");
        }

        membership.setRole(request.getRole());
        membership = membershipRepository.save(membership);

        User user = userRepository.findById(membership.getUserId()).orElse(null);
        String email = user != null ? user.getEmail() : "unknown";
        String name = user != null ? (user.getFirstName() + " " + user.getLastName()) : "unknown";

        return new MembershipDto(
                membership.getId(),
                membership.getOrganizationId(),
                membership.getUserId(),
                email,
                name,
                membership.getRole(),
                membership.getStatus(),
                membership.getCreatedAt()
        );
    }
}
