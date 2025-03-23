package com.swayam.bugwise.repository.jpa;

import com.swayam.bugwise.entity.Organization;
import com.swayam.bugwise.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrganizationRepository extends JpaRepository<Organization, String> {
    boolean existsByName(String name);

    @Query(value = "select * from organizations where admin_id = :adminId", nativeQuery = true)
    List<Organization> findByAdminId(@Param("adminId") String adminId);

    List<Organization> findByAdmin(User admin);
}