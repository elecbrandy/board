package com.elecbrandy.boilerplate.auth.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AuthConstants {

    // 1. 비밀번호 규칙
    public static final int PASSWORD_MIN_LENGTH = 8;
    public static final int PASSWORD_MAX_LENGTH = 20;
    // 영문, 숫자, 특수문자 포함 정규식
    public static final String PASSWORD_REGEX = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,20}$";
    public static final String PASSWORD_MESSAGE = "비밀번호는 8~20자이며 영문, 숫자, 특수문자를 포함해야 합니다.";

    // 이메일 규칙
    public static final String EMAIL_REGEX = "^[a-zA-Z0-9+-\\_.]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+$";
    public static final String EMAIL_MESSAGE = "유효한 이메일 형식이 아닙니다.";

    // 닉네임(Username) 규칙
    public static final int USERNAME_MIN = 2;
    public static final int USERNAME_MAX = 20;
    public static final String USERNAME_REGEX = "^[a-zA-Z0-9가-힣]*$"; // 특수문자 제외, 한글/영문/숫자 허용
    public static final String USERNAME_MESSAGE = "닉네임은 특수문자를 제외한 2~20자여야 합니다.";
}