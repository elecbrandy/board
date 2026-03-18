package com.elecbrandy.boilerplate.domain.dto;


import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@Table(name = "refresh_token")
@Getter
@AllArgsConstructor
public class TokenInfo {
    private String grantType;
    private String accessToken;
    private String refreshToken;
}
