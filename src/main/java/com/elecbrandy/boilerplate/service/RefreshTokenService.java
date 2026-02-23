package com.elecbrandy.boilerplate.service;

import com.elecbrandy.boilerplate.domain.entity.RefreshToken;
import com.elecbrandy.boilerplate.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteAllByKey(String key) {
        refreshTokenRepository.deleteAllByKey(key);
    }
}

