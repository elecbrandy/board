package com.elecbrandy.boilerplate.service;

import com.elecbrandy.boilerplate.domain.entity.User;
import com.elecbrandy.boilerplate.global.constants.AppConstants;
import com.elecbrandy.boilerplate.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * Spring Security의 {@link UserDetailsService}를 구현한 커스텀 서비스 클래스입니다.
 * <p>
 * 사용자가 로그인을 시도할 때, Spring Security의 AuthenticationManager는 이 클래스의
 * {@link #loadUserByUsername(String)} 메서드를 호출하여 데이터베이스에서 사용자 정보를 조회합니다.
 * 조회된 커스텀 User 엔티티는 Spring Security가 내부적으로 사용하는 {@link UserDetails} 객체로 변환됩니다.
 * </p>
 * *
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * 이메일(식별자)을 기반으로 데이터베이스에서 사용자 정보를 조회합니다.
     * <p>
     * Spring Security는 기본적으로 'username'이라는 용어를 사용하지만,
     * 본 시스템에서는 이메일(Email)을 로그인 아이디로 사용하므로 파라미터로 이메일을 받습니다.
     * </p>
     *
     * @param email 로그인을 시도하는 사용자의 이메일 (username 역할)
     * @return Spring Security가 인증을 처리하기 위해 필요한 {@link UserDetails} 객체
     * @throws UsernameNotFoundException 해당 이메일을 가진 사용자가 DB에 존재하지 않을 경우 발생
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .map(this::createUserDetails)
                .orElseThrow(() -> new UsernameNotFoundException("해당하는 유저를 찾을 수 없습니다."));
    }

    /**
     * 데이터베이스에서 조회한 커스텀 {@link User} 엔티티를
     * Spring Security의 {@link org.springframework.security.core.userdetails.User} 객체로 변환합니다.
     * <p>
     * 이 과정에서 사용자의 권한(Role) 정보를 추출하여 {@link SimpleGrantedAuthority}로 맵핑하고,
     * 이를 Spring Security 인증 객체에 부여합니다.
     * </p>
     *
     * @param user 데이터베이스에서 조회된 영속성 계층의 커스텀 User 엔티티
     * @return 이메일, 암호화된 비밀번호, 권한 목록을 포함하는 Spring Security의 UserDetails 구현체
     */
    private UserDetails createUserDetails(User user) {
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                Collections.singleton(new SimpleGrantedAuthority(user.getRole().getKey()))
        );
    }
}
