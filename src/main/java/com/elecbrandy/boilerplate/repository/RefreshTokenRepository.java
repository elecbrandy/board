package com.elecbrandy.boilerplate.repository;

import com.elecbrandy.boilerplate.domain.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Refresh Token 엔티티를 데이터베이스에서 관리하는 JPA 리포지토리입니다.
 * <p>
 * 사용자의 이메일(key)과 토큰 값(value)을 기준으로 토큰을 조회하거나 삭제하는 역할을 수행합니다.
 * 보안(RTR 방어) 및 세션 관리(다중 기기 로그인 제한)를 위한 커스텀 메서드를 포함하고 있습니다.
 * </p>
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * 사용자의 이메일과 Refresh Token 값을 동시에 만족하는 토큰을 조회합니다.
     * (주로 토큰 재발급 시 정상적인 접근인지 검증하기 위해 사용됩니다.)
     */
    Optional<RefreshToken> findByKeyAndValue(String key, String value);

    /**
     * Refresh Token 값만으로 토큰을 조회합니다.
     * (주로 로그아웃 요청 시 해당 토큰이 DB에 존재하는지 확인할 때 사용됩니다.)
     */
    Optional<RefreshToken> findByValue(String value);

    /**
     * 특정 사용자의 활성화된 토큰이 DB에 최소 1개 이상 존재하는지 확인합니다.
     */
    boolean existsByKey(String key);

    /**
     * 특정 사용자의 모든 Refresh Token을 일괄 삭제합니다.
     * (주로 RTR 재사용 공격이 의심될 때, 해당 사용자의 모든 세션을 강제 종료하기 위해 호출됩니다.)
     */
    void deleteAllByKey(String key);

    List<RefreshToken> findAllByKeyOrderByIdAsc(String key);


    /**
     * 특정 사용자의 토큰 중 가장 최근에 발급된 N개를 제외한 나머지 오래된 토큰을 일괄 삭제합니다.
     * <p>
     * 하나의 계정으로 여러 기기에서 로그인할 때 DB에 무한정 세션(토큰) 데이터가 쌓이는 것을 방지합니다.
     * <br>
     * 서브쿼리를 사용하여 특정 이메일을 가진 토큰 중 최근에 생성된(ID가 큰)
     * {@code keepCount}개의 ID 목록을 구한 뒤, 이 목록에 포함되지 않는(NOT IN) 오래된 레코드들을 삭제합니다.
     * </p>
     *
     * @param email 토큰을 소유한 사용자의 이메일 식별자
     * @param keepCount 유지할 최신 토큰의 최대 개수 (예: 3대 기기까지만 허용한다면 3)
     */
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