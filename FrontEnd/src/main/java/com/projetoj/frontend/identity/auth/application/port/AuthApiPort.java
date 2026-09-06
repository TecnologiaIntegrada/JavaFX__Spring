package com.projetoj.frontend.identity.auth.application.port;

import com.projetoj.frontend.identity.auth.adapter.rest.LoginResponseDto;

public interface AuthApiPort {
    LoginResponseDto login(String username, String password);
}
