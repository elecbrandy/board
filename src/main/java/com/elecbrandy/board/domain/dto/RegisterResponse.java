package com.elecbrandy.board.domain.dto;

import com.elecbrandy.board.domain.entity.User;
import lombok.Getter;

@Getter
public class RegisterResponse {
    private final Long id;
    private final String email;
    private final String username;

    public RegisterResponse(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.username = user.getUsername();
    }
}
