package com.swayam.bugwise.service;

import com.swayam.bugwise.dto.OrganizationDTO;
import com.swayam.bugwise.dto.OrganizationRequestDTO;
import com.swayam.bugwise.entity.Organization;
import com.swayam.bugwise.entity.User;
import com.swayam.bugwise.exception.UnauthorizedAccessException;
import com.swayam.bugwise.exception.ValidationException;
import com.swayam.bugwise.repository.jpa.OrganizationRepository;
import com.swayam.bugwise.repository.jpa.UserRepository;
import com.swayam.bugwise.utils.DTOConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OrganizationService {
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;

    public Organization createOrganization(OrganizationRequestDTO request) {
        if (organizationRepository.existsByName(request.getName())) {
            throw new ValidationException(Map.of("name", "Organization name already exists"));
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        log.info("username: {}", username);

        Organization organization = new Organization();
        organization.setName(request.getName());
        organization.setDescription(request.getDescription());

        return organizationRepository.save(organization);
    }

    @Cacheable(value = "organizations", key = "#organizationId")
    public OrganizationDTO getOrganization(String organizationId, User user) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NoSuchElementException("Organization not found"));

        if (!organization.isAdmin(user)) {
            throw new UnauthorizedAccessException("You are not authorized to access this organization");
        }

        return DTOConverter.convertToDTO(organization, OrganizationDTO.class);
    }

    public List<OrganizationDTO> getOrganizationsCreatedByAdmin(String adminUserName) {
        String adminId = userRepository.getIdByUserName(adminUserName);
        List<Organization> organizations = organizationRepository.findByAdminId(adminId);
        return organizations.stream()
                .map(org -> DTOConverter.convertToDTO(org, OrganizationDTO.class))
                .collect(Collectors.toList());
    }
}