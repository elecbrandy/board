package com.elecbrandy.board.service;

import com.elecbrandy.board.domain.dto.LoginRequest;
import com.elecbrandy.board.domain.dto.RegisterRequest;
import com.elecbrandy.board.domain.dto.RegisterResponse;
import com.elecbrandy.board.domain.dto.TokenInfo;
import com.elecbrandy.board.domain.entity.User;
import com.elecbrandy.board.global.jwt.JwtTokenProvider;
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
        // authenticate 메서드가 실행될 때 CustomUserDetailsService에서 만든 loadUserByUsername 메서드가 실행됩니다.
        Authentication authentication = authenticationManager.authenticate(authenticationToken);

        // 3. 인증 정보를 기반으로 JWT 토큰 생성
        return jwtTokenProvider.generateToken(authentication);
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

