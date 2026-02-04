package com.elecbrandy.board.repository;

import com.elecbrandy.board.domain.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

}
