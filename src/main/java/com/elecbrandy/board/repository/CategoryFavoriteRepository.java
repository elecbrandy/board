package com.elecbrandy.board.repository;

import com.elecbrandy.board.domain.entity.CategoryFavorite;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryFavoriteRepository extends JpaRepository<CategoryFavorite, Long> {

}
