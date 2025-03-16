package com.swayam.bugwise.repository.jpa;

import com.swayam.bugwise.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationRepository extends JpaRepository<Organization, String> {
    boolean existsByName(String name);
}