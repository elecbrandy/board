package com.elecbrandy.board.global.exception;

import com.elecbrandy.board.global.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 중복 회원 예외 처리 (Service에서 던진 에러)
    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(IllegalStateException.class)
    public ApiResponse<Void> handleIllegalStateException(IllegalStateException e) {
        return ApiResponse.fail(e.getMessage());
    }

    // @Valid 검증 실패 예외 처리
    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleValidationExceptions(MethodArgumentNotValidException e) {
        String errorMessage = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        return ApiResponse.fail(errorMessage);
    }

    // 1. 비밀번호가 틀렸을 때 (BadCredentialsException)
    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(BadCredentialsException.class)
    public ApiResponse<Void> handleBadCredentialsException(BadCredentialsException e) {
        // HTTP Status 200 OK를 리턴하면서, Body에는 실패 메시지 담기
        return ApiResponse.fail("아이디 또는 비밀번호가 일치하지 않습니다.");
    }

    // 2. 계정이 없을 때 (UserDetailsService에서 예외 발생 시)
    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler({UsernameNotFoundException.class, InternalAuthenticationServiceException.class})
    public ApiResponse<Void> handleUserNotFoundException(Exception e) {
        return ApiResponse.fail("계정을 찾을 수 없습니다.");
    }

    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleAllException(Exception e) {
        return ApiResponse.error("알 수 없는 오류가 발생했습니다: " + e.getMessage());
    }
}
