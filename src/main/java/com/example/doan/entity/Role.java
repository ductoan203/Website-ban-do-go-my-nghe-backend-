package com.example.doan.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import java.util.Set;

@Entity
@Table(name = "role")
@AllArgsConstructor
@Builder
@NoArgsConstructor
@Data
@ToString(exclude = "users")
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long roleId;

    @Column(unique = true)
    private String name;
    private String description;

    @OneToMany(mappedBy = "role", fetch = FetchType.LAZY)
    @JsonManagedReference
    private Set<User> users;

    public Role(String name) {
        this.name = name;
    }
}