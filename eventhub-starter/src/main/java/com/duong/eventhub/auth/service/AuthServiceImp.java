package com.duong.eventhub.auth.service;

import com.duong.eventhub.auth.dto.CreateAccountRequest;
import com.duong.eventhub.auth.dto.LoginRequest;
import com.duong.eventhub.auth.dto.LoginResponse;
import com.duong.eventhub.exception.ResourceExistedException;
import com.duong.eventhub.security.JwtService;
import com.duong.eventhub.security.MyUserDetails;
import com.duong.eventhub.user.AppUser;
import com.duong.eventhub.user.AppUserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImp implements AuthService{

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;


    @Override
    public UUID createAccount(CreateAccountRequest request) {
        checkIfEmailExist(request.email());

        AppUser user = AppUser.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .displayName(request.displayName())
                .enabled(request.enabled())
                .createdAt(Instant.now())
                .tokenVersion(0)
                .roles(Set.of(request.role()))
                              .build();

        appUserRepository.save(user);
        return user.getId();
    }

    private void checkIfEmailExist(String email){;
        if  (appUserRepository.existsByEmail(email)){
            throw new ResourceExistedException(email + " already existed");
        }
    }

    @Override
    public LoginResponse login(LoginRequest request, HttpServletResponse response){
        Authentication authentication = UsernamePasswordAuthenticationToken.unauthenticated(request.email(),request.password());
        authentication = authenticationManager.authenticate(authentication);

        MyUserDetails myUserDetails = (MyUserDetails) authentication.getPrincipal();


        String accessToken = jwtService.generateAccessToken(myUserDetails);
        String refreshToken = jwtService.generateRefreshToken(myUserDetails);

        Cookie cookie = new Cookie("refresh-token",refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/auth");
        cookie.setMaxAge(7 * 24 * 60 * 60);
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);

        return new LoginResponse(accessToken);
    }
}
