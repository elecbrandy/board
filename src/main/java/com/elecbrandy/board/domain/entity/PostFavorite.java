package com.elecbrandy.board.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "post_favorites",
        // user_id 와 post_id의 조합은 유일해야 한다.
        uniqueConstraints = {
            @UniqueConstraint(columnNames = {"user_id", "post_id"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostFavorite extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 외래키: User와 N:1 관계: User 한 명이 여러 개의 favorite posts를 가진다.
    // 즉, 좋아요를 누른 사람
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 외래키2: 좋아요를 받은 게시글
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Builder
    public PostFavorite(User user, Post post) {
        this.user = user;
        this.post = post;
    }
}
