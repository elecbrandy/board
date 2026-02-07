package com.elecbrandy.board.global.response;

import com.elecbrandy.board.domain.enums.ResponseStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private String status;
    private String message;
    private T data;

    // 성공 응답 (데이터 포함)
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(
        ResponseStatus.SUCCESS.getValue(),
            message,
            data
        );
    }

    // 성공 응답 (데이터 없음)
    public static <T> ApiResponse<T> success(String message) {
        return success(message, null);
    }

    // 실패 응답
    public static <T> ApiResponse<T> fail(String message, T data) {
        return new ApiResponse<>(
            ResponseStatus.FAIL.getValue(),
            message,
            data
        );
    }

    // 실패 응답 (데이터 없음)
    public static <T> ApiResponse<T> fail(String message) {
        return fail(message, null);
    }

    // 에러 응답
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(
            ResponseStatus.ERROR.getValue(),
            message,
            null
        );
    }
}
