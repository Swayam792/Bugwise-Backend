package com.swayam.bugwise.repository.jpa;

import com.swayam.bugwise.entity.User;
import com.swayam.bugwise.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    List<User> findByOrganizationIdAndRole(String organizationId, UserRole role);

    @Query(value = "SELECT u FROM User u WHERE u.organization.id = :orgId AND u.role = :role AND " +
            "u.isEnabled = true", nativeQuery = true)
    List<User> findActiveUsersByOrganizationAndRole(
            @Param("orgId") String organizationId,
            @Param("role") UserRole role
    );

    @Query(value = "SELECT COUNT(u) FROM User u WHERE u.organization.id = :orgId AND " +
            "u.role = :role AND u.isEnabled = true", nativeQuery = true)
    long countActiveUsersByOrganizationAndRole(
            @Param("orgId") String organizationId,
            @Param("role") UserRole role
    );
}
