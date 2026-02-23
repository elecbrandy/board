package com.elecbrandy.boilerplate.repository;

import com.elecbrandy.boilerplate.domain.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByKeyAndValue(String key, String value);
    Optional<RefreshToken> findByValue(String value);
    boolean existsByKey(String key);
    void deleteAllByKey(String key);
    List<RefreshToken> findAllByKeyOrderByIdAsc(String key);

    @Modifying
    @Query(value = """
    DELETE FROM refresh_token
    WHERE key_email = :email
      AND id NOT IN (
          SELECT id FROM (
              SELECT id FROM refresh_token
              WHERE key_email = :email
              ORDER BY id DESC
              LIMIT :keepCount
          ) AS latest
      )
    """, nativeQuery = true)
    void deleteOldTokensKeepLatest(@Param("email") String email, @Param("keepCount") int keepCount);

    long countByKey(String key);
}