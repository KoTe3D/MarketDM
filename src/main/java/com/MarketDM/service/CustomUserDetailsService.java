package com.MarketDM.service;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Здесь должна быть загрузка из БД. Пока вернём тестового пользователя.
        return User.withUsername(username)
                .password("{noop}password") // {noop} — без шифрования, только для теста
                .roles("USER")
                .build();
    }
}
