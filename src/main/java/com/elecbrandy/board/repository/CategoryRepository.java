package com.elecbrandy.board.repository;

import com.elecbrandy.board.domain.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

}
