package com.MarketDM.config;

import com.MarketDM.security.JwtAuthenticationEntryPoint;
import com.MarketDM.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
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
    /*
     Вынесенный бин провайдер чтобы, чтобы не создавать его 3 раза
     */
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
        http
                .securityMatcher("/api/**", "/mobile/**")
                .authenticationProvider(daoAuthenticationProvider())// Используем общий бин
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.disable())// API stateless, CSRF не нужен
                .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthEntryPoint))
                .authorizeHttpRequests(auth -> auth
                        // Публичные эндпоинты (без токена)
                        .requestMatchers(
                                "/api/v1/auth/**",
                                "/api/v1/public/**",
                                "/api/v1/products/**",
                                "/api/v1/categories/**",
                                "/api/v1/search/**"
                        ).permitAll()
                        // Только покупатели
                        .requestMatchers("/api/v1/cart/**").hasRole("CUSTOMER")
                        .requestMatchers("/api/v1/orders/**").hasRole("CUSTOMER")
                        .requestMatchers("/api/v1/reviews/**").hasRole("CUSTOMER")
                        .requestMatchers("/api/v1/profile/**").hasRole("CUSTOMER")
                        // Только продавцы
                        .requestMatchers("/api/v1/seller/**").hasRole("SELLER")
                        .requestMatchers("/api/v1/seller/products/**").hasRole("SELLER")
                        .requestMatchers("/api/v1/seller/analytics/**").hasRole("SELLER")
                        // Только админы
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        // Всё остальное – только аутентифицированные
                        .anyRequest().authenticated()
                )
                .sessionManagement(sess -> sess
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)

                // Заголовки безопасности для API
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'"))
                        .httpStrictTransportSecurity(hsts -> hsts.maxAgeInSeconds(31536000).includeSubDomains(true))
                        .frameOptions(frame -> frame.deny()) // API не должен быть во фреймах
                );
        return http.build();
    }

    @Value("${app.security.remember-me.key}")
    private String rememberMeKey;

    //Административная панель – веб-интерфейс с сессиями
    @Bean
    @Order(2)
    public SecurityFilterChain adminWebSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/admin/**")// Оставляем /admin/**, статика ниже
                .authenticationProvider(daoAuthenticationProvider())//Хоть бин и объявлен Spring Security не использует его автоматически в кастомизированных цепочках HttpSecurity, так что нужно его объявить или создастся дефолтный. И не появятся ошибки UserDetailsService и PasswordEncoder.
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/admin/login", "/admin/error").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        // Разрешаю статику для админки:
                        .requestMatchers("/webjars/**", "/css/**", "/js/**", "/images/**").permitAll()
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
                        .tokenValiditySeconds(86400 * 30) // 30 дней
                )
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())// с осторожностью нужна защита от XSS
                        .ignoringRequestMatchers("/api/**", "/mobile/**") // только API исключения
                )
                //Заголовки для админки
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin()) // Разрешаем фреймы для H2 Console (dev)
                        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline';"))
                        .httpStrictTransportSecurity(hsts -> hsts.maxAgeInSeconds(31536000).includeSubDomains(true))
                );

        return http.build();
    }

    /* Основной веб-интерфейс: главная, каталог, корзина, регистрация и т.д. */
    @Bean
    @Order(3)
    public SecurityFilterChain webSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/**") // Ловит всё, что не подошло под /api и /admin
                .authenticationProvider(daoAuthenticationProvider())
                .authorizeHttpRequests(auth -> auth
                        // Публичные страницы
                        .requestMatchers("/", "/login", "/register", "/products", "/categories", "/search")
                        .permitAll()
                        // Статические ресурсы
                        .requestMatchers("/error","/images/**", "/css/**", "/js/**", "/favicon.*")
                        .permitAll()
                        // Страницы для авторизованных
                        .requestMatchers("/cart", "/profile", "/orders")
                        .authenticated()
                        // Всё остальное
                        .anyRequest().authenticated() // Будет доступно после авторизации
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/login?error=true")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                )
                .sessionManagement(sess -> sess
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )
                .rememberMe(remember -> remember
                        .key(rememberMeKey)
                        .tokenValiditySeconds(86400 * 7) // 7 дней
                )
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())// с осторожностью нужна защита от XSS
                        .ignoringRequestMatchers("/api/**", "/mobile/**") // только API исключения
                );

        return http.build();
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
