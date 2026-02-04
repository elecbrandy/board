package com.elecbrandy.board.repository;

import com.elecbrandy.board.domain.entity.PostFavorite;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostFavoriteRepository extends JpaRepository<PostFavorite, Long> {

}
