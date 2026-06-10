package com.MarketDM.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RateLimitFilter extends OncePerRequestFilter {

    private final BucketCache cache = new BucketCache();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Применяем только к эндпоинтам входа
        if (request.getRequestURI().matches(".*/auth/.*")) {
            String ip = request.getRemoteAddr();
            Bucket bucket = cache.getBucket(ip);

            if (bucket.tryConsume(1)) {
                filterChain.doFilter(request, response);
            } else {
                response.setStatus(429);
                response.setContentType("text/plain;charset=UTF-8");
                response.getWriter().write("Too many requests, please try again later.");
            }
        } else {
            filterChain.doFilter(request, response);
        }
    }

    private static class BucketCache {
        // Кэш автоматически удалит бакет, если к нему не обращались 10 минут
        // Максимум 100_000 бакетов в памяти (защита от DDoS)
        private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
                .maximumSize(100_000)
                .expireAfterAccess(10, TimeUnit.MINUTES)
                .build();

        public Bucket getBucket(String key) {
            return buckets.get(key, k -> createNewBucket());
        }

        private Bucket createNewBucket() {
            Refill refill = Refill.greedy(5, Duration.ofMinutes(1)); // 5 запросов в минуту
            Bandwidth limit = Bandwidth.classic(5, refill);
            return Bucket.builder().addLimit(limit).build();
        }
    }
}