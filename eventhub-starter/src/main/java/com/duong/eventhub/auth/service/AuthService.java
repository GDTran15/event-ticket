package com.duong.eventhub.auth.service;


import com.duong.eventhub.auth.dto.CreateAccountRequest;
import com.duong.eventhub.auth.dto.LoginRequest;
import com.duong.eventhub.auth.dto.LoginResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import java.util.UUID;

public interface AuthService {

    UUID createAccount(@Valid CreateAccountRequest request);

    LoginResponse login(LoginRequest request, HttpServletResponse response);
}
