package com.elecbrandy.board.repository;

import com.elecbrandy.board.domain.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {

}
