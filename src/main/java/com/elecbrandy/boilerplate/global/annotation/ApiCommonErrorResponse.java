package com.elecbrandy.boilerplate.global.annotation;

import com.elecbrandy.boilerplate.global.response.CommonResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 전역 API 공통 에러 응답을 정의하는 커스텀 어노테이션입니다.
 * 인터페이스의 클래스 레벨이나 메서드 레벨에 부착하여 사용합니다.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "잘못된 요청 (입력값 검증 실패 등)",
                content = @Content(schema = @Schema(implementation = CommonResponse.class))),
        @ApiResponse(responseCode = "401", description = "인증 실패 (토큰 만료, 유효하지 않은 토큰, 로그인 필요 등)",
                content = @Content(schema = @Schema(implementation = CommonResponse.class))),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                content = @Content(schema = @Schema(implementation = CommonResponse.class)))
})
public @interface ApiCommonErrorResponse {
}
