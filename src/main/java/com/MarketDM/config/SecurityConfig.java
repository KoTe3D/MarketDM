package com.MarketDM.config;

import com.MarketDM.security.JwtAuthenticationEntryPoint;
import com.MarketDM.security.JwtAuthenticationFilter;
import com.MarketDM.security.RateLimitFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.ExceptionTranslationFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.beans.factory.annotation.Value;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthFilter;
    private final JwtAuthenticationEntryPoint jwtAuthEntryPoint;
    private final CorsConfigurationSource corsConfigurationSource;
    private final RateLimitFilter rateLimitFilter;

        /*Вынесенный бин провайдер чтобы, чтобы не создавать его 3 раза*/

        @Value("${app.security.remember-me.key}")
        private String rememberMeKey;

        @Bean
        public DaoAuthenticationProvider daoAuthenticationProvider() {
            DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
            provider.setPasswordEncoder(passwordEncoder());
            return provider;
        }

        /*
         API для мобильных приложений и SPA (React/Vue) – STATELESS, JWT
         */
        @Bean
        @Order(1)
        public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .securityMatcher("/api/**", "/mobile/**")
                    .authenticationProvider(daoAuthenticationProvider())
                    .cors(cors -> cors.configurationSource(corsConfigurationSource))
                    .csrf(csrf -> csrf.disable())

                    // Отключаем стандартное управление контекстом для API
                    .securityContext(ctx -> ctx.disable())

                    .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                    .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                    .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthEntryPoint))
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers(
                                    "/api/v1/auth/**", "/api/v1/public/**",
                                    "/api/v1/products/**", "/api/v1/categories/**", "/api/v1/search/**"
                            ).permitAll()
                            .requestMatchers("/api/v1/cart/**", "/api/v1/orders/**", "/api/v1/reviews/**", "/api/v1/profile/**").hasRole("CUSTOMER")
                            .requestMatchers("/api/v1/seller/**", "/api/v1/seller/products/**", "/api/v1/seller/analytics/**").hasRole("SELLER")
                            .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                            .anyRequest().authenticated()
                    )
                    .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .headers(headers -> headers
                            .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'"))
                            .httpStrictTransportSecurity(hsts -> hsts.maxAgeInSeconds(31536000).includeSubDomains(true))
                            .frameOptions(frame -> frame.deny())
                    )
                    .build();
        }

        //Административная панель – веб-интерфейс с сессиями
        @Bean
        @Order(2)
        public SecurityFilterChain adminWebSecurityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .securityMatcher("/admin/**")
                    .authenticationProvider(daoAuthenticationProvider())
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers("/admin/login", "/admin/error").permitAll()
                            .requestMatchers("/admin/**").hasRole("ADMIN")
                            .anyRequest().authenticated()
                    )
                    .formLogin(form -> form
                            .loginPage("/admin/login")
                            .loginProcessingUrl("/admin/login")
                            .defaultSuccessUrl("/admin/dashboard", true)
                            .failureUrl("/admin/login?error=true")
                            .permitAll()
                    )
                    .logout(logout -> logout
                            .logoutUrl("/admin/logout")
                            .logoutSuccessUrl("/admin/login?logout=true")
                            .invalidateHttpSession(true)
                            .deleteCookies("JSESSIONID")
                    )
                    .sessionManagement(sess -> sess
                            .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                            .maximumSessions(1)
                            .expiredUrl("/admin/login?expired=true")
                    )
                    .rememberMe(remember -> remember
                            .key(rememberMeKey)
                            .tokenValiditySeconds(86400 * 30)
                    )
                    .headers(headers -> headers
                            .frameOptions(frame -> frame.sameOrigin())
                            .httpStrictTransportSecurity(hsts -> hsts.maxAgeInSeconds(31536000).includeSubDomains(true))
                    )
                    .csrf(csrf -> csrf.disable())
                    .build();
        }

        // Основной веб-интерфейс
        @Bean
        @Order(3)
        public SecurityFilterChain webSecurityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .headers(headers -> headers
                            // Запретить загрузку страницы во фрейме (защита от clickjacking)
                            .frameOptions(frame -> frame.deny())
                            // Политика безопасности контента: разрешить загрузку ресурсов только с вашего домена
                            .contentSecurityPolicy(csp -> csp.policyDirectives(
                                    "default-src 'self'; " +
                                            "img-src 'self' data: https:; " +  // Разрешить картинки с вашего домена + data: (для base64)
                                            "style-src 'self' 'unsafe-inline'; " +  // Для инлайн-стилей
                                            "script-src 'self'"  // Запретить внешние скрипты
                            ))
                            // HSTS: принудительный HTTPS (в проде)
                            .httpStrictTransportSecurity(hsts -> hsts
                                    .maxAgeInSeconds(31536000)
                                    .includeSubDomains(true)
                            )
                    )
                    .authorizeHttpRequests(auth -> auth
                            // Публичные страницы
                            .requestMatchers(
                                    "/", "/index.html", "/error",
                                    "/login", "/register", "/products", "/categories", "/search",
                                    "/about", "/delivery", "/return", "/faq", "/feedback",
                                    "/jobs", "/blog", "/terms", "/privacy"
                            ).permitAll()
                            // Статика
                            .requestMatchers(
                                    "/css/**", "/js/**", "/images/**", "/images/*.png", "/images/*.jpg",
                                    "/images/*.jpeg", "/images/*.gif", "/images/*.webp", "/video/**",
                                    "/fonts/**", "/webjars/**", "/favicon.*",
                                    "/*.html", "/**/*.html"
                            ).permitAll()

                            //Личные страницы (требуют авторизации)
                            .requestMatchers("/cart", "/profile", "/orders").authenticated()

                            // Всё остальное
                            .anyRequest().permitAll()
                    )
                    .logout(logout -> logout
                            .logoutUrl("/logout")
                            .logoutSuccessUrl("/")
                            .invalidateHttpSession(true)
                            .deleteCookies("JSESSIONID")
                    )
                    .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                    .rememberMe(remember -> remember
                            .key(rememberMeKey)
                            .tokenValiditySeconds(86400 * 7)
                    )
                    .csrf(csrf -> csrf.disable()) // Для статики/веба без форм Spring-обработки отключаем
                    .build();
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder(12);
        }

        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
            return config.getAuthenticationManager();

        }
    }