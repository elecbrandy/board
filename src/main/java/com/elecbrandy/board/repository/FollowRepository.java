package com.elecbrandy.board.repository;

import com.elecbrandy.board.domain.entity.Follow;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FollowRepository extends JpaRepository<Follow, Long> {

}
