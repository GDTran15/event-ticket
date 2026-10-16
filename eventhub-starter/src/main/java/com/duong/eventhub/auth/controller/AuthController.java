package com.duong.eventhub.auth.controller;

import com.duong.eventhub.auth.dto.LoginRequest;
import com.duong.eventhub.auth.dto.LoginResponse;
import com.duong.eventhub.auth.service.AuthService;
import com.duong.eventhub.auth.dto.CreateAccountRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import java.net.URI;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;


    @PostMapping("/api/v1/create")
    public ResponseEntity<Void> createdAccount(@RequestBody @Valid CreateAccountRequest request){
        UUID id = authService.createAccount(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(id).toUri();
        return ResponseEntity.created(location).build();
    }


    @GetMapping("/me")
    public String me(@AuthenticationPrincipal Jwt jwt) {
        return jwt.getSubject();
    }

    @PostMapping("/api/v1/login")
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest request, HttpServletResponse response){
        return ResponseEntity.ok(authService.login(request,response));
    }
}
