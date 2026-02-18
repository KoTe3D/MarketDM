package com.MarketDM.controller;

import com.MarketDM.DTO.UserResponseDto;
import com.MarketDM.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserService userService;

    @GetMapping("/profile")
    public ResponseEntity<UserResponseDto> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        UserResponseDto user = userService.findByEmail(userDetails.getUsername());
        return ResponseEntity.ok(user);
    }
}

