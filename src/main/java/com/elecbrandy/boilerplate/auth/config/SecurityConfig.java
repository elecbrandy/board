package com.elecbrandy.boilerplate.auth.config;

import com.elecbrandy.boilerplate.auth.jwt.JwtAccessDeniedHandler;
import com.elecbrandy.boilerplate.auth.jwt.JwtAuthenticationEntryPoint;
import com.elecbrandy.boilerplate.auth.jwt.JwtAuthenticationFilter;
import com.elecbrandy.boilerplate.auth.jwt.JwtTokenProvider;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;


/**
 * Spring Security의 전반적인 인증 및 인가 설정을 담당하는 Configuration 클래스입니다.
 * <p>
 * 1. CSRF 보호 비활성화 (REST API 기반 동작이므로 불필요)<br>
 * 2. CORS(Cross-Origin Resource Sharing) 정책 설정<br>
 * 3. 세션을 사용하지 않는 Stateless(무상태) 세션 정책 적용 (JWT 사용)<br>
 * 4. 인증/인가 예외 처리를 위한 핸들러 등록 (401 Unauthorized, 403 Forbidden)<br>
 * 5. URL 경로별 접근 권한 설정 (회원가입/로그인 및 Swagger UI는 허용, 그 외는 인증 요구)<br>
 * 6. 기본 폼 로그인 인증 필터 전에 커스텀 JWT 인증 필터({@link JwtAuthenticationFilter}) 추가
 * </p>
 * *
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Value("#{'${app.cors.allowed-origins}'.split(',')}")
    private List<String> allowedOrigins;

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    /**
     * 인증(Authentication)을 처리하는 중심 인터페이스인 {@link AuthenticationManager} 빈을 등록합니다.
     * 로그인 시도 시 사용자의 자격 증명을 검증하는 데 사용됩니다.
     *
     * @param authenticationConfiguration Spring Security의 인증 구성 객체
     * @return 초기화된 AuthenticationManager 인스턴스
     * @throws Exception 초기화 중 발생할 수 있는 예외
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * HTTP 요청에 대한 보안 필터 체인을 구성합니다.
     * <p>
     * JWT 토큰 기반 인증을 수행하기 위해 세션 정책을 STATELESS로 설정하며,
     * {@link UsernamePasswordAuthenticationFilter}가 실행되기 전에 {@link JwtAuthenticationFilter}가 먼저 실행되도록 등록합니다.
     * </p>
     *
     * @param http Spring Security의 HttpSecurity 객체 (보안 설정을 위한 빌더)
     * @return 구성이 완료된 SecurityFilterChain 인스턴스
     * @throws Exception 보안 구성 중 발생할 수 있는 예외
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((jwtAuthenticationEntryPoint)) // 401
                        .accessDeniedHandler(jwtAccessDeniedHandler) // 403
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**", "/api/users/register").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * 프론트엔드 애플리케이션과의 원활한 통신을 위해 CORS 정책을 설정하는 빈을 등록합니다.
     * <p>
     * application.yaml에 정의된 {@code app.cors.allowed-origins} 값을 기반으로 허용할 Origin을 설정하며,
     * 모든 헤더 및 주요 HTTP 메서드(GET, POST, PUT, DELETE, OPTIONS)를 허용합니다.
     * 또한, 쿠키나 인증 정보 등을 포함한 요청(Credentials)을 허용하도록 설정합니다.
     * </p>
     *
     * @return CORS 설정이 적용된 CorsConfigurationSource 인스턴스
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins); // 프론트엔드 주소
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
