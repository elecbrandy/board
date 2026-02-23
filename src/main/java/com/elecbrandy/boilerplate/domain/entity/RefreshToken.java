package com.elecbrandy.boilerplate.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "key_email", nullable = false)
    private String key; // 사용자 Email (식별자)

    @Column(nullable = false) // 인덱스 걸어주는 것이 성능상 좋음
    private String value; // Refresh Token String

    @Builder
    public RefreshToken(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public void updateValue(String token) {
        this.value = token;
    }
}
