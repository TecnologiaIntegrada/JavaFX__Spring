package com.projetoj.frontend.identity.user.application;

import com.projetoj.frontend.identity.user.adapter.rest.CreateUserRequestDto;
import com.projetoj.frontend.identity.user.adapter.rest.UpdateUserRequestDto;
import com.projetoj.frontend.identity.user.adapter.rest.UserResponseDto;
import com.projetoj.frontend.identity.user.application.port.UserApiPort;

import java.util.List;
import java.util.UUID;

public class UserApplicationService {

    private final UserApiPort userApiPort;

    public UserApplicationService(UserApiPort userApiPort) {
        this.userApiPort = userApiPort;
    }

    public List<UserResponseDto> list() {
        return userApiPort.list();
    }

    public UserResponseDto create(CreateUserRequestDto request) {
        return userApiPort.create(request);
    }

    public UserResponseDto update(UUID id, UpdateUserRequestDto request) {
        return userApiPort.update(id, request);
    }

    public void delete(UUID id) {
        userApiPort.delete(id);
    }
}
