package com.MarketDM.repository;

import com.MarketDM.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Поиск пользователя по email (используется при обычной регистрации и логине)
    Optional<User> findByEmail(String email);

    // Поиск пользователя по provider_id (для OAuth2 – быстрый вход)
    Optional<User> findByProviderId(String providerId);

    // Проверка, существует ли пользователь с таким email (для регистрации и обновления)
    boolean existsByEmail(String email);

    // Проверка, существует ли пользователь с таким provider_id (для OAuth2)
    boolean existsByProviderId(String providerId);

    // При использовании составного уникального ключа (provider, provider_id),
    // добавляем метод для поиска по паре:
    Optional<User> findByProviderAndProviderId(String provider, String providerId);
}