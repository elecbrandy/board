package com.elecbrandy.boilerplate.domain.entity;

import com.elecbrandy.boilerplate.domain.enums.Role;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Builder
    public User(String email, String password, String username, Role role) {
        this.email = email;
        this.password = password;
        this.username = username;
        this.role = (role != null) ? role : Role.USER;
    }

    public void updateUsername(String newUsername) {
        this.username = newUsername;
    }

    public void changePassword(String newPassword) {
        this.password = newPassword;
    }
}
