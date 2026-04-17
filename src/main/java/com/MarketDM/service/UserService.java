package com.MarketDM.service;

import com.MarketDM.DTO.UserCreateDto;
import com.MarketDM.DTO.UserResponseDto;
import com.MarketDM.DTO.UserUpdateDto;
import com.MarketDM.entity.User;
import com.MarketDM.entity.Role;
import com.MarketDM.exception.EmailAlreadyExistsException;
import com.MarketDM.exception.ResourceNotFoundException;
import com.MarketDM.repository.RoleRepository;
import com.MarketDM.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;

    // Поиск всех пользователей
    public List<UserResponseDto> findAll() {
        return userRepository.findAll()
                .stream()
                .map(UserResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    // Поиск пользователя по ID
    public UserResponseDto findById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return UserResponseDto.fromEntity(user);
    }

    // Поиск пользователя по email
    public UserResponseDto findByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        return UserResponseDto.fromEntity(user);
    }

    // Все пользователи с пагинацией
    public Page<UserResponseDto> findAll(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(UserResponseDto::fromEntity);
    }

    // Создание нового пользователя (обычная регистрация)
    @Transactional
    public UserResponseDto create(UserCreateDto createDto) {
        // Проверяем, не занят ли email
        if (userRepository.existsByEmail(createDto.getEmail())) {
            throw new EmailAlreadyExistsException("Email already in use");
        }

        Role customerRole = roleRepository.findByName("ROLE_CUSTOMER")
                .orElseThrow(() -> new RuntimeException("Default role ROLE_CUSTOMER not found"));

        User user = User.builder()
                .email(createDto.getEmail())
                .password(passwordEncoder.encode(createDto.getPassword())) // кодируем пароль
                .firstName(createDto.getFirstName())
                .lastName(createDto.getLastName())
                .provider("local") // обычная регистрация
                .roles(Set.of(customerRole))
                .enabled(true)
                .build();

        user = userRepository.save(user);
        return UserResponseDto.fromEntity(user);
    }

    // Обновление пользователя (частичное)
    @Transactional
    public UserResponseDto update(Long id, UserUpdateDto dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        // Обновляем только переданные поля (не null)
        if (dto.getEmail() != null) {
            // Проверяем, что email не занят другим пользователем
            if (!dto.getEmail().equals(user.getEmail()) &&
                    userRepository.existsByEmail(dto.getEmail())) {
                throw new EmailAlreadyExistsException("Email already in use");
            }
            user.setEmail(dto.getEmail());
        }

        if (dto.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        if (dto.getFirstName() != null) {
            user.setFirstName(dto.getFirstName());
        }

        if (dto.getLastName() != null) {
            user.setLastName(dto.getLastName());
        }

        if (dto.getAvatarUrl() != null) {
            user.setAvatarUrl(dto.getAvatarUrl());
        }

        if (dto.getEnabled() != null) {
            // Здесь можно добавить проверку прав (только админ)
            user.setEnabled(dto.getEnabled());
        }

        return UserResponseDto.fromEntity(user);
    }

    // Удаление пользователя
    @Transactional
    public void delete(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }
}