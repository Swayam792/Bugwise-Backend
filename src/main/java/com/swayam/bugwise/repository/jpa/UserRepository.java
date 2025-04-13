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
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u JOIN u.organizations o WHERE " +
            "o.id = :orgId AND u.role = :role AND u.isActive = true")
    List<User> findActiveUsersByOrganizationAndRole(
            @Param("orgId") String organizationId,
            @Param("role") UserRole role
    );

    @Query(value = "SELECT id FROM users WHERE email = :email", nativeQuery = true)
    String getIdByEmail(@Param("email") String email);

    @Query("SELECT COUNT(u) FROM User u JOIN u.organizations o WHERE " +
            "o.id = :organizationId AND u.role = :role AND u.isActive = true")
    long countActiveUsersByOrganizationsAndRole(
            @Param("organizationId") String organizationId,
            @Param("role") UserRole role
    );

    @Query(value = "SELECT COUNT(u) FROM User u JOIN u.assignedBugs b " +
            "WHERE b.project.id IN :projectIds AND u.role = :role AND u.isActive = true")
    long countActiveUsersByProjectsAndRole(
            @Param("projectIds") Set<String> projectIds,
            @Param("role") UserRole role
    );

    List<User> findByManagedProjectsIdAndRole(String projectId, UserRole role);
    List<User> findByOrganizationsIdAndRole(String organizationId, UserRole role);
    Set<User> findAllByIdIn(Set<String> ids);
    List<User> findByAssignedProjectsIdAndRole(String projectId, UserRole role);
    Set<User> findAllByEmailIn(Set<String> emails);
}