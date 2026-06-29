package com.MarketDM.controller;

import com.MarketDM.DTO.UserCreateDto;
import com.MarketDM.DTO.UserResponseDto;
import com.MarketDM.DTO.UserUpdateDto;
import com.MarketDM.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // Получить всех пользователей (с пагинацией и сортировкой)
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')") // только админ может видеть всех
    public ResponseEntity<Page<UserResponseDto>> findAll(
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return ResponseEntity.ok(userService.findAll(pageable));
    }

    // Получить одного пользователя по ID
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.isCurrentUser(#id)")
    public ResponseEntity<UserResponseDto> findById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    // Создать нового пользователя (обычная регистрация)
    @PostMapping
    public ResponseEntity<UserResponseDto> create(@Valid @RequestBody UserCreateDto createDto) {
        UserResponseDto created = userService.create(createDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // Обновить пользователя (частичное обновление)
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.isCurrentUser(#id)")
    public ResponseEntity<UserResponseDto> update(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateDto updateDto
    ) {
        UserResponseDto updated = userService.update(id, updateDto);
        return ResponseEntity.ok(updated);
    }

    // Удалить пользователя
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // Получить текущего пользователя (по JWT)
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponseDto> getCurrentUser(
            @org.springframework.security.core.annotation.AuthenticationPrincipal
            org.springframework.security.core.userdetails.UserDetails currentUser
    ) {
        // Предполагаем, что email = username
        return ResponseEntity.ok(userService.findByEmail(currentUser.getUsername()));
    }
}
