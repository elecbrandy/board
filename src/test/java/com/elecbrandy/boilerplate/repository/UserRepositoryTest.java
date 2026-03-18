package com.elecbrandy.boilerplate.repository;

import com.elecbrandy.boilerplate.domain.entity.User;
import com.elecbrandy.boilerplate.domain.enums.Role;
import com.elecbrandy.boilerplate.support.RepositoryTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class UserRepositoryTest extends RepositoryTestSupport {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("이메일로 사용자를 정상적으로 조회한다")
    void findByEmail_success() {
        // given
        User user = User.builder()
                .email("test@example.com")
                .password("encodedPassword")
                .username("테스터")
                .role(Role.USER)
                .build();
        userRepository.save(user);

        // when
        Optional<User> foundUser = userRepository.findByEmail("test@example.com");

        // then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUsername()).isEqualTo("테스터");
    }

    @Test
    @DisplayName("존재하는 이메일 및 닉네임 여부를 정상적으로 확인한다")
    void existsByEmailAndUsername() {
        // given
        User user = User.builder()
                .email("test@example.com")
                .password("encodedPassword")
                .username("테스터")
                .role(Role.USER)
                .build();
        userRepository.save(user);

        // when
        boolean existsEmail = userRepository.existsByEmail("test@example.com");
        boolean existsUsername = userRepository.existsByUsername("테스터");
        boolean notExistsEmail = userRepository.existsByEmail("unknown@example.com");

        // then
        assertThat(existsEmail).isTrue();
        assertThat(existsUsername).isTrue();
        assertThat(notExistsEmail).isFalse();
    }
}