package com.swayam.bugwise.repository.jpa;

import com.swayam.bugwise.dto.BugStatisticsDTO;
import com.swayam.bugwise.entity.Bug;
import com.swayam.bugwise.enums.BugSeverity;
import com.swayam.bugwise.enums.BugStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface BugRepository extends JpaRepository<Bug, String> {

    @Query(value = "SELECT b FROM Bug b WHERE " +
            "b.project.id = :projectId AND " +
            "b.severity = :severity AND " +
            "b.status NOT IN (:closed, :resolved)")
    List<Bug> findActiveByProjectAndSeverity(
            @Param("projectId") String projectId,
            @Param("severity") BugSeverity severity,
            @Param("closed") BugStatus closed,
            @Param("resolved") BugStatus resolved
    );

    @Query(value = "SELECT NEW com.swayam.bugwise.dto.BugStatisticsDTO(b.status, COUNT(b)) " +
            "FROM Bug b WHERE b.project.id = :projectId " +
            "GROUP BY b.status")
    List<BugStatisticsDTO> getBugStatisticsByProject(@Param("projectId") String projectId);

    @Query(value = "SELECT b FROM Bug b WHERE " +
            "b.project.id = :projectId AND " +
            "(LOWER(b.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(b.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')))", nativeQuery = true)
    Page<Bug> searchBugsInProject(
            @Param("projectId") String projectId,
            @Param("searchTerm") String searchTerm,
            Pageable pageable
    );

    @Query(value = "SELECT new com.swayam.bugwise.dto.BugStatisticsDTO(b.status, COUNT(b)) FROM Bug b GROUP BY b.status")
    List<BugStatisticsDTO> findBugStatistics();

    @Query(value = "SELECT new com.swayam.bugwise.dto.BugStatisticsDTO(b.status, COUNT(b)) FROM Bug b WHERE b.project.id IN :projectIds GROUP BY b.status")
    List<BugStatisticsDTO> findBugStatisticsByProjectIdIn(@Param("projectIds") Set<String> projectIds);

    @Query("SELECT b FROM Bug b " +
            "JOIN FETCH b.project p " +
            "JOIN FETCH p.organization o " +
            "LEFT JOIN FETCH p.projectManager pm " +
            "WHERE p.organization.id IN :organizationIds")
    Page<Bug> findByProjectOrganizationIdIn(@Param("organizationIds") Set<String> organizationIds, Pageable pageable);

    @Query("SELECT b FROM Bug b " +
            "JOIN FETCH b.project p " +
            "JOIN FETCH p.organization o " +
            "LEFT JOIN FETCH p.projectManager pm " +
            "WHERE p.id IN :projectIds")
    Page<Bug> findByProjectIdIn(@Param("projectIds") Set<String> projectIds, Pageable pageable);
}