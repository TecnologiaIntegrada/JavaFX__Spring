package com.projetoj.frontend.identity.user.application.port;

import com.projetoj.frontend.identity.user.adapter.rest.CreateUserRequestDto;
import com.projetoj.frontend.identity.user.adapter.rest.UpdateUserRequestDto;
import com.projetoj.frontend.identity.user.adapter.rest.UserResponseDto;

import java.util.List;
import java.util.UUID;

public interface UserApiPort {
    List<UserResponseDto> list();

    UserResponseDto create(CreateUserRequestDto request);

    UserResponseDto update(UUID id, UpdateUserRequestDto request);

    void delete(UUID id);
}
