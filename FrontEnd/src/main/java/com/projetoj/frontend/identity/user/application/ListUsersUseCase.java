package com.projetoj.frontend.identity.user.application;

import com.projetoj.frontend.identity.user.adapter.rest.UserResponseDto;
import com.projetoj.frontend.identity.user.application.port.UserApiPort;

import java.util.List;

public class ListUsersUseCase {

    private final UserApiPort userApiPort;

    public ListUsersUseCase(UserApiPort userApiPort) {
        this.userApiPort = userApiPort;
    }

    public List<UserResponseDto> execute() {
        return userApiPort.list();
    }
}
