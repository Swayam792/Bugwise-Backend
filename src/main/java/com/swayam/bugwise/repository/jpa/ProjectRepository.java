package com.swayam.bugwise.repository.jpa;

import com.swayam.bugwise.entity.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface ProjectRepository extends JpaRepository<Project, String> {
    List<Project> findByOrganizationId(String organizationId);
    List<Project> findByProjectManagerId(String projectManagerId);

    @Query(value = "SELECT p FROM Project p WHERE p.organization.id = :orgId AND " +
            "(LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(p.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')))", nativeQuery = true)
    Page<Project> searchProjectsInOrganization(
            @Param("orgId") String organizationId,
            @Param("searchTerm") String searchTerm,
            Pageable pageable
    );

    @Query("SELECT COUNT(p) FROM Project p WHERE p.organization.id = :orgId")
    long countProjectsByOrganization(@Param("orgId") String organizationId);

    List<Project> findByOrganizationIdIn(Set<String> organizationIds);

    @Query("SELECT DISTINCT p FROM Project p JOIN p.bugs b WHERE b.assignedDeveloper.id = :developerId")
    List<Project> findByAssignedBugsDeveloperId(@Param("developerId") String developerId);
}