package com.elecbrandy.boilerplate.global.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // 200 OK
    SUCCESS(HttpStatus.OK, "SUCCESS", "요청이 성공했습니다."),

    // 400 Bad Request
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "CMN_001", "잘못된 입력값입니다."),
    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "AUTH_004", "비밀번호가 일치하지 않습니다."),

    // 401 Unauthorized
    AUTH_INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_001", "유효하지 않은 토큰입니다."),
    AUTH_EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_002", "만료된 토큰입니다."),
    LOGIN_REQUIRED(HttpStatus.UNAUTHORIZED, "AUTH_005", "로그인이 필요합니다."),

    // 403 Forbidden
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "AUTH_006", "접근 권한이 없습니다."),

    // 404 Not Found
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "AUTH_003", "계정을 찾을 수 없습니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "CMN_003", "리소스를 찾을 수 없습니다."),

    // 409 Conflict
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "AUTH_007", "이미 존재하는 이메일입니다."),
    DUPLICATE_USERNAME(HttpStatus.CONFLICT, "AUTH_008", "이미 존재하는 닉네임입니다."),

    // 500 Internal Server Error
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "CMN_002", "서버 내부 오류가 발생했습니다.");

    private final HttpStatus status; // HTTP 상태 코드 추가
    private final String code;
    private final String message;
}
