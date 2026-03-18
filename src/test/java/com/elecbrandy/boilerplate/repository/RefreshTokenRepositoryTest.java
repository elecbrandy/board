package com.elecbrandy.boilerplate.repository;

import com.elecbrandy.boilerplate.auth.domain.entity.RefreshToken;
import com.elecbrandy.boilerplate.auth.repository.RefreshTokenRepository;
import com.elecbrandy.boilerplate.domain.entity.User;
import com.elecbrandy.boilerplate.domain.enums.Role;
import com.elecbrandy.boilerplate.support.RepositoryTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenRepositoryTest extends RepositoryTestSupport {

    @Autowired
    private UserRepository userRepository;

    private User saveUser(String email) {
        return userRepository.save(User.builder()
                .email(email)
                .password("encodedPassword")
                .username(email.split("@")[0])
                .role(Role.USER)
                .build());
    }

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Test
    @DisplayName("사용자의 최신 N개의 토큰만 남기고 오래된 토큰은 모두 삭제한다")
    void deleteOldTokensKeepLatest_success() {
        // given
        String email = "multi@example.com";
        saveUser(email);

        // 5개의 기기에서 로그인했다고 가정 (ID가 순차적으로 증가하며 저장됨)
        refreshTokenRepository.save(new RefreshToken(email, "token1"));
        refreshTokenRepository.save(new RefreshToken(email, "token2"));
        refreshTokenRepository.save(new RefreshToken(email, "token3"));
        refreshTokenRepository.save(new RefreshToken(email, "token4"));
        refreshTokenRepository.save(new RefreshToken(email, "token5"));

        // when: 최신 3개만 남기고 삭제 실행
        refreshTokenRepository.deleteOldTokensKeepLatest(email, 3);

        // then: 총 개수는 3개여야 하며, 살아남은 토큰은 최신인 token3, token4, token5 여야 한다.
        List<RefreshToken> remainingTokens = refreshTokenRepository.findAllByKeyOrderByIdAsc(email);

        assertThat(remainingTokens).hasSize(3);
        assertThat(remainingTokens).extracting("value")
                .containsExactly("token3", "token4", "token5");
    }

    @Test
    @DisplayName("이메일로 모든 리프레시 토큰을 정상적으로 삭제한다 (RTR 방어 시 사용)")
    void deleteAllByKey_success() {
        // given
        String email = "hacked@example.com";
        saveUser(email);
        refreshTokenRepository.save(new RefreshToken(email, "tokenA"));
        refreshTokenRepository.save(new RefreshToken(email, "tokenB"));

        // when
        refreshTokenRepository.deleteAllByKey(email);

        // then
        long count = refreshTokenRepository.countByKey(email);
        assertThat(count).isEqualTo(0);
    }
}