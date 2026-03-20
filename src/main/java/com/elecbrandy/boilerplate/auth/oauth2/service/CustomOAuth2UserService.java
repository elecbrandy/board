package com.elecbrandy.boilerplate.auth.oauth2.service;

import com.elecbrandy.boilerplate.auth.oauth2.domain.OAuthProvider;
import com.elecbrandy.boilerplate.domain.entity.User;
import com.elecbrandy.boilerplate.domain.enums.Role;
import com.elecbrandy.boilerplate.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

/**
 * Google OAuth2 인증 성공 후 사용자 정보를 처리하는 서비스입니다.
 * <p>
 * Spring Security가 Google로부터 사용자 프로필을 받아온 뒤 이 클래스를 호출합니다.
 * DB에 해당 이메일이 없으면 자동으로 소셜 계정을 생성(회원가입)하고,
 * 이미 있으면 조회만 합니다 (자동 로그인).
 * <br>
 * 비즈니스 로직(UserService)에 직접 의존하지 않고 UserRepository만 사용하여
 * OAuth2 패키지의 독립성을 유지합니다.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // 1. 부모 클래스로 Google UserInfo 엔드포인트에서 사용자 정보 가져오기
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // 2. 제공자 식별 (google, github 등) - 현재는 google만 지원
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        log.debug("OAuth2 로그인 시도 - provider: {}", registrationId);

        // 3. Google 사용자 속성에서 이메일 추출
        Map<String, Object> attributes = oAuth2User.getAttributes();
        String email = (String) attributes.get("email");
        String name  = (String) attributes.get("name");

        if (email == null) {
            log.error("OAuth2 사용자 정보에서 이메일을 찾을 수 없습니다. attributes: {}", attributes);
            throw new OAuth2AuthenticationException("email_not_found");
        }

        // 4. 이메일로 기존 회원 조회 → 없으면 자동 가입
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> registerOAuthUser(email, name));

        // 5. Spring Security가 사용할 OAuth2User 반환 (이메일을 nameAttributeKey로 사용)
        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority(user.getRole().getKey())),
                attributes,
                "email"  // nameAttributeKey: authentication.getName() 호출 시 반환될 값
        );
    }

    /**
     * 최초 소셜 로그인 시 DB에 사용자를 자동 등록합니다.
     * <p>
     * 비밀번호는 소셜 로그인 계정이므로 의미 없는 UUID 값으로 채웁니다.
     * (이 비밀번호로는 일반 로그인이 불가능하므로 보안 문제 없음)
     * </p>
     */
    private User registerOAuthUser(String email, String name) {
        // 닉네임 중복 방지: name이 이미 사용 중이면 뒤에 랜덤 4자리 추가
        String baseUsername = (name != null && !name.isBlank()) ? name : email.split("@")[0];
        String username = resolveUniqueUsername(baseUsername);

        User newUser = User.builder()
                .email(email)
                .password(UUID.randomUUID().toString()) // 소셜 전용 계정, 실제 사용 안 함
                .username(username)
                .role(Role.USER)
                .provider(OAuthProvider.GOOGLE)
                .build();

        log.info("신규 Google OAuth2 사용자 자동 가입 - email: {}", email);
        return userRepository.save(newUser);
    }

    /**
     * 닉네임 중복 시 숫자를 뒤에 붙여 유일한 닉네임을 반환합니다.
     */
    private String resolveUniqueUsername(String base) {
        // 영문/숫자/한글 외 문자 제거 (UsernameValid 정규식 준수)
        String sanitized = base.replaceAll("[^a-zA-Z0-9가-힣]", "");
        if (sanitized.isBlank()) sanitized = "user";
        // 최대 16자로 자르기 (뒤에 숫자 붙일 여유 확보)
        if (sanitized.length() > 16) sanitized = sanitized.substring(0, 16);

        String candidate = sanitized;
        int suffix = 1;
        while (userRepository.existsByUsername(candidate)) {
            candidate = sanitized + suffix++;
        }
        return candidate;
    }
}
