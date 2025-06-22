package com.spkt.libraSys.service.role;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Entity
@Table(name = "roles")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    private Long id;

    @Column(name = "role_name", nullable = false)
    private String roleName;

//    @ManyToMany(mappedBy = "roles")
//    private Set<User> users;
}
