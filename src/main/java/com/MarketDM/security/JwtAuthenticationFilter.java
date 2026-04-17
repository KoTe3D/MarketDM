package com.MarketDM.security;

import com.MarketDM.config.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // Извлекаем JWT из заголовка Authorization
            String jwt = getJwtFromRequest(request);

            // Если токен есть и он валиден
            if (jwt != null && tokenProvider.validateToken(jwt)) {
                // Извлекаем username (email) из токена
                String username = tokenProvider.extractUsername(jwt);

                // Загружаем данные пользователя из БД
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                // Создаём объект аутентификации
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                // Добавляем детали запроса (IP, сессию и т.п.)
                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                // Устанавливаем аутентификацию в SecurityContext
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("User {} authenticated successfully", username);
            }
        } catch (Exception e) {
            log.error("Could not set user authentication in security context", e);
        }

        // Продолжаем выполнение цепочки фильтров
        filterChain.doFilter(request, response);
    }

    /*
     Достаёт JWT из заголовка Authorization (формат "Bearer <token>")
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /*
     Можно переопределить, чтобы не фильтровать некоторые URL
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Например, пропускаем эндпоинты аутентификации без фильтрации
        return path.startsWith("/api/v1/auth/") ||// проверка может быть избыточной, если пути точно совпадают
                path.startsWith("/api/v1/public/") ||//Если используются шаблоны типа /api/v1/auth/**, лучше использовать AntPathRequestMatcher.
                path.startsWith("/swagger-ui/") ||// Но текущий вариант рабочий.
                path.startsWith("/v3/api-docs/");
    }
}