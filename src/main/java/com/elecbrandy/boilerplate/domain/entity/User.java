package com.elecbrandy.boilerplate.domain.entity;

import com.elecbrandy.boilerplate.auth.oauth2.domain.OAuthProvider;
import com.elecbrandy.boilerplate.domain.enums.Role;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Builder;
import lombok.NoArgsConstructor;

/**
 * 사용자 엔티티입니다.
 * <p>
 * 일반 가입(LOCAL)과 소셜 가입(GOOGLE)을 구분합니다.
 * DB 마이그레이션: V2__add_oauth_provider.sql 참고
 * </p>
 */
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

    /**
     * 가입 경로 구분자.
     * LOCAL = 이메일/비밀번호 가입, GOOGLE = Google OAuth2 가입
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OAuthProvider provider;

    @Builder
    public User(String email, String password, String username, Role role, OAuthProvider provider) {
        this.email    = email;
        this.password = password;
        this.username = username;
        this.role     = (role != null) ? role : Role.USER;
        this.provider = (provider != null) ? provider : OAuthProvider.LOCAL;
    }

    public void updateUsername(String newUsername) {
        this.username = newUsername;
    }

    public void changePassword(String newPassword) {
        this.password = newPassword;
    }
}