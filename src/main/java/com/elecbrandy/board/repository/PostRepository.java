package com.elecbrandy.board.repository;

import com.elecbrandy.board.domain.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {

}
