package com.elecbrandy.boilerplate.global.exception;

import com.elecbrandy.boilerplate.global.response.CommonResponse;
import com.elecbrandy.boilerplate.global.response.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. 비즈니스 로직 예외 (BusinessException)
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<CommonResponse<Void>> handleBusinessException(BusinessException e) {
        log.warn("BusinessException: {}", e.getMessage());
        return ResponseEntity
                .status(e.getErrorCode().getStatus())
                .body(CommonResponse.fail(e.getErrorCode()));
    }

    // 2. @Valid 검증 실패 (400)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CommonResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        BindingResult bindingResult = e.getBindingResult();
        String firstErrorMessage = bindingResult.getAllErrors().get(0).getDefaultMessage();

        log.warn("Validation Error: {}", firstErrorMessage);
        return ResponseEntity
                .status(ErrorCode.INVALID_INPUT.getStatus())
                .body(CommonResponse.fail(firstErrorMessage, ErrorCode.INVALID_INPUT.getCode()));
    }

    // 3. 로그인 실패 (비밀번호 불일치 등) (400 or 401)
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<CommonResponse<Void>> handleBadCredentialsException(BadCredentialsException e) {
        return ResponseEntity
                .status(ErrorCode.PASSWORD_MISMATCH.getStatus())
                .body(CommonResponse.fail(ErrorCode.PASSWORD_MISMATCH));
    }

    // 4. 잘못된 URL 요청 (404) - application.yaml 설정 필요
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<CommonResponse<Void>> handleNoHandlerFoundException(NoHandlerFoundException e) {
        return ResponseEntity
                .status(ErrorCode.RESOURCE_NOT_FOUND.getStatus())
                .body(CommonResponse.fail(ErrorCode.RESOURCE_NOT_FOUND));
    }

    // 5. 그 외 모든 예외 (500)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<CommonResponse<Void>> handleException(Exception e) {
        log.error("Unhandled Exception: ", e);
        return ResponseEntity
                .status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus())
                .body(CommonResponse.error(ErrorCode.INTERNAL_SERVER_ERROR));
    }
}
