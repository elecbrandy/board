package com.elecbrandy.boilerplate.auth.oauth2.domain;

/**
 * 지원하는 소셜 로그인 제공자(Provider) 목록입니다.
 * <p>
 * User 엔티티에 저장하여 해당 계정이 어떤 방식으로 가입되었는지를 식별합니다.
 * LOCAL = 일반 이메일/비밀번호 가입, GOOGLE = Google OAuth2 가입
 * </p>
 */
public enum OAuthProvider {
    LOCAL,
    GOOGLE
}
