package com.MarketDM.controller;

import com.MarketDM.DTO.UserCreateDto;
import com.MarketDM.DTO.UserResponseDto;
import com.MarketDM.service.UserService;
import com.MarketDM.DTO.LoginRequestDto;
import com.MarketDM.DTO.JwtResponseDto;
import com.MarketDM.config.JwtTokenProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    @PostMapping("/register")
    public ResponseEntity<UserResponseDto> register(@Valid @RequestBody UserCreateDto createDto) {
        UserResponseDto created = userService.create(createDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // Эндпоинт для логина (получения JWT):

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDto loginRequest) {
        // используем AuthenticationManager для проверки
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);
        return ResponseEntity.ok(new JwtResponseDto(jwt));
    }

}
