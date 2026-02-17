package com.elecbrandy.board.service;

import com.elecbrandy.board.domain.dto.LoginRequest;
import com.elecbrandy.board.domain.dto.RegisterRequest;
import com.elecbrandy.board.domain.dto.RegisterResponse;
import com.elecbrandy.board.domain.dto.TokenInfo;
import com.elecbrandy.board.domain.entity.RefreshToken;
import com.elecbrandy.board.domain.entity.User;
import com.elecbrandy.board.global.jwt.JwtTokenProvider;
import com.elecbrandy.board.repository.RefreshTokenRepository;
import com.elecbrandy.board.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional(readOnly = false) // 쓰기 작업이니까 readOnly 해제
    public RegisterResponse register(RegisterRequest request) {
        // request 검증
        validateDuplicateUser(request);

        // request DTO -> Entity 변환
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        User user = request.toEntity(encodedPassword);

        // user 정보 db에 저장 (repository 호출)
        User savedUser = userRepository.save(user);

        // repository 반환값 Entity -> response DTO 반환
        return new RegisterResponse(savedUser);
    }

    @Transactional(readOnly = false) // 쓰기 작업이니까 readOnly 해제
    public TokenInfo login(LoginRequest request) {
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword());
        // 2. 실제 검증 (사용자 비밀번호 체크)이 이루어지는 부분
        Authentication authentication = authenticationManager.authenticate(authenticationToken);

        // 3. token 생성
        TokenInfo tokenInfo = jwtTokenProvider.generateToken(authentication);

        // 4. refresh token db 저장 (기존에 있으면 업데이트, 없으면 생성)
        refreshTokenRepository.findById(authentication.getName()) // authentication.getName()은 email
                .ifPresentOrElse(
                        r -> r.updateValue(tokenInfo.getRefreshToken()), // 이미 있으면 토큰값 업데이트
                        () -> refreshTokenRepository.save(new RefreshToken(authentication.getName(), tokenInfo.getRefreshToken())) // 없으면 새로 저장
                );

        // 5. 인증 정보를 기반으로 JWT 토큰 생성
        return jwtTokenProvider.generateToken(authentication);
    }

    @Transactional
    public TokenInfo reissue(String refreshToken) {
        // 1. 토큰 유효성 검사
        if (refreshToken == null || !jwtTokenProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않은 Refresh Token입니다.");
        }

        // 2. 토큰에서 유저 정보 추출
        Authentication authentication = jwtTokenProvider.getAuthentication(refreshToken);

        // 3. DB에서 해당 유저의 Refresh Token 가져오기
        RefreshToken dbToken = refreshTokenRepository.findByKey(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("로그아웃 된 사용자입니다."));

        // 4. [중요] 토큰 불일치 감지 (RTR 전략: 이미 사용된 토큰으로 요청 시)
        if (!dbToken.getValue().equals(refreshToken)) {
            // 보안 위협: 해당 유저의 Refresh Token 강제 삭제 (로그아웃 처리)
            refreshTokenRepository.delete(dbToken);
            // 예외 발생 -> Controller/Handler에서 처리
            throw new IllegalStateException("보안 위협이 감지되어 로그아웃 처리되었습니다.");
        }

        // 5. 새 토큰 생성 및 DB 업데이트
        TokenInfo newToken = jwtTokenProvider.generateToken(authentication);
        dbToken.updateValue(newToken.getRefreshToken()); // Dirty Checking으로 DB 업데이트

        return newToken;
    }

    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken != null && jwtTokenProvider.validateToken(refreshToken)) {
            Authentication authentication = jwtTokenProvider.getAuthentication(refreshToken);
            // DB에서 삭제 (확실한 로그아웃)
            refreshTokenRepository.deleteById(authentication.getName());
        }
    }

    // Validate
    private void validateDuplicateUser(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalStateException("이미 존재하는 이메일입니다.");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalStateException("이미 존재하는 이름입니다.");
        }
    }
}

