package com.swayam.bugwise.repository.jpa;

import com.swayam.bugwise.entity.User;
import com.swayam.bugwise.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    @Query(value = "SELECT u FROM users u WHERE u.organization.id = :orgId AND u.role = :role AND " +
            "u.isEnabled = true", nativeQuery = true)
    List<User> findActiveUsersByOrganizationAndRole(
            @Param("orgId") String organizationId,
            @Param("role") UserRole role
    );

    @Query(value = "SELECT id FROM users WHERE username = :username", nativeQuery = true)
    String getIdByUserName(@Param("username") String username);

    @Query(value = "SELECT COUNT(u.id) FROM users u " +
            "JOIN organization_user ou ON u.id = ou.user_id " +
            "JOIN organizations o ON ou.organization_id = o.id " +
            "WHERE o.id = :organizationId AND u.role = :role AND u.is_active = true",
            nativeQuery = true)
    long countActiveUsersByOrganizationsAndRole(@Param("organizationId") String organizationId, @Param("role") String role);

    @Query(value = "SELECT COUNT(u) FROM User u JOIN u.assignedBugs b WHERE b.project.id IN :projectIds AND u.role = :role AND u.isActive = true",nativeQuery = true)
    long countActiveUsersByProjectsAndRole(@Param("projectIds") Set<String> projectIds, @Param("role") UserRole role);
}
