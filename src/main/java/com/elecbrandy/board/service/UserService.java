package com.elecbrandy.board.service;

import com.elecbrandy.board.domain.dto.RegisterRequest;
import com.elecbrandy.board.domain.dto.RegisterResponse;
import com.elecbrandy.board.domain.entity.User;
import com.elecbrandy.board.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

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

