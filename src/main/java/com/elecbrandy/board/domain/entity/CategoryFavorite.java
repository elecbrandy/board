package com.elecbrandy.board.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "category_favorite",
        // user_id 와 post_id의 조합은 유일해야 한다.
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "category_id"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CategoryFavorite extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Builder
    public CategoryFavorite(User user, Category category) {
        this.user = user;
        this.category = category;
    }
}
