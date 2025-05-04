package com.swayam.bugwise.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.swayam.bugwise.enums.BugSeverity;
import com.swayam.bugwise.enums.BugStatus;
import com.swayam.bugwise.enums.BugType;
import com.swayam.bugwise.enums.DeveloperType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "bugs")
@Getter
@Setter
@NoArgsConstructor
public class Bug extends BaseEntity {
    @Field(type = FieldType.Text, analyzer = "english")
    @NotBlank(message = "Title cannot be blank")
    private String title;

    @Field(type = FieldType.Text, analyzer = "english")
    @NotBlank(message = "Description cannot be blank")
    @Column(columnDefinition = "TEXT")
    private String description;

    @Field(type = FieldType.Keyword)
    private BugStatus status = BugStatus.NEW;

    @Field(type = FieldType.Keyword)
    private BugSeverity severity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    @JsonBackReference("project-bugs")
    private Project project;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "bug_developers",
            joinColumns = @JoinColumn(name = "bug_id"),
            inverseJoinColumns = @JoinColumn(name = "developer_id")
    )
    @JsonManagedReference("bug-assigned-developers")
    private Set<User> assignedDeveloper = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_by_id", nullable = false)
    @JsonBackReference("user-reported-bugs")
    private User reportedBy;

    @OneToMany(mappedBy = "bug", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("bug-comments")
    private Set<Comment> comments = new HashSet<>();

    @Enumerated(EnumType.STRING)
    private BugType bugType;

    @Column(name = "expected_time_hours")
    private Integer expectedTimeHours;

    @Column(name = "actual_time_hours")
    private Integer actualTimeHours;

//    @ElementCollection
//    @CollectionTable(name = "bug_required_developer_types", joinColumns = @JoinColumn(name = "bug_id"))
//    @Column(name = "developer_type")
//    @Enumerated(EnumType.STRING)
//    private Set<DeveloperType> requiredDeveloperTypes = new HashSet<>();
}