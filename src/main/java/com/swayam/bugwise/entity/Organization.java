package com.swayam.bugwise.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "organizations")
@Getter
@Setter
@NoArgsConstructor
public class Organization extends BaseEntity {
    @NotBlank(message = "Organization name cannot be blank")
    @Column(unique = true)
    private String name;

    @NotBlank(message = "Description cannot be blank")
    private String description;

    @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL)
    @JsonManagedReference
    private Set<Project> projects = new HashSet<>();

    @ManyToMany(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @JoinTable(
            name = "organization_user",
            joinColumns = @JoinColumn(name = "organization_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> users = new HashSet<>();

    @OneToOne
    private User admin;

    public boolean isAdmin(User user) {
        return admin.equals(user);
    }
}