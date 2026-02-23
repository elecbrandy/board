package com.elecbrandy.boilerplate.global.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "공통 응답 포맷")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    @Schema(description = "응답 상태", example = "success", allowableValues = {"success", "fail", "error"})
    private String status;

    @Schema(description = "응답 메시지", example = "로그인 성공")
    private String message;

    @Schema(description = "응답 코드", example = "SUCCESS")
    private String code;

    @Schema(description = "응답 데이터")
    private T data;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(
                ResponseStatus.SUCCESS.getValue(),
                ErrorCode.SUCCESS.getMessage(),
                ErrorCode.SUCCESS.getCode(),
                data
        );
    }

    public static <T> ApiResponse<T> success() {
        return success(null);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(
                ResponseStatus.SUCCESS.getValue(),
                message,
                ErrorCode.SUCCESS.getCode(),
                data
        );
    }

    public static <T> ApiResponse<T> ok(String message) {
        return new ApiResponse<>(
                ResponseStatus.SUCCESS.getValue(),
                message,
                ErrorCode.SUCCESS.getCode(),
                null
        );
    }

    public static <T> ApiResponse<T> fail(ErrorCode errorCode, T data) {
        return new ApiResponse<>(
                ResponseStatus.FAIL.getValue(),
                errorCode.getMessage(),
                errorCode.getCode(),
                data
        );
    }

    public static <T> ApiResponse<T> fail(ErrorCode errorCode) {
        return fail(errorCode, null);
    }

    public static <T> ApiResponse<T> fail(String message, String code) {
        return new ApiResponse<>(
                ResponseStatus.FAIL.getValue(),
                message,
                code,
                null
        );
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode) {
        return new ApiResponse<>(
                ResponseStatus.ERROR.getValue(),
                errorCode.getMessage(),
                errorCode.getCode(),
                null
        );
    }
}
