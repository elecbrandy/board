package com.elecbrandy.boilerplate.global.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE) // new AppConstants() 방지
public final class AppConstants {

    // ========================================================================
    // 1. JWT & 인증 관련 (Authentication)
    // ========================================================================
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String ACCESS_TOKEN = "accessToken";
    public static final String REFRESH_TOKEN = "refreshToken";
    public static final String AUTHORITIES_KEY = "auth";
    public static final String ANONYMOUS_USER = "anonymousUser";

    // ========================================================================
    // 3. 정규식 (Validation Regex)
    // ========================================================================

    /**
     * 비밀번호 정규식: 8~20자, 영문 + 숫자 + 특수문자(@$!%*#?&) 각각 최소 1개 이상 포함
     */
    public static final String PASSWORD_REGEX = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,20}$";

    /**
     * (선택) 이메일 정규식 - @Email 어노테이션으로 충분하지만, 더 강력한 검증이 필요할 때 사용
     */
    public static final String EMAIL_REGEX = "^[a-zA-Z0-9+-\\_.]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+$";

    // ========================================================================
    // 4. 시스템 설정 (System)
    // ========================================================================
    public static final String ZONE_ID_SEOUL = "Asia/Seoul";

    // 필요하다면 날짜 포맷도 여기서 관리
    public static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";


    public static final String COOKIE_PATH_ROOT = "/";
    public static final String COOKIE_SAME_SITE_LAX = "Lax";

    public static final String TOKEN_TYPE_KEY = "type";
    public static final String ACCESS_TOKEN_TYPE = "ACCESS";
    public static final String REFRESH_TOKEN_TYPE = "REFRESH";
}