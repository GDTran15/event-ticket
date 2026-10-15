package com.duong.eventhub.auth;

import com.duong.eventhub.user.AppUser;
import com.duong.eventhub.user.AppUserRepository;
import com.duong.eventhub.user.Role;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.net.URI;
import java.util.Locale;
import java.util.UUID;

import org.apache.coyote.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final AuthService authService;

    public ResponseEntity<Void> createdAccount(@RequestBody @Valid CreateAccountRequest request){
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand().toUri();
        return ResponseEntity.created(location).build();
    }

}
