package com.MarketDM.security;

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
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
                response.setStatus(429); // Too Many Requests
                response.getWriter().write("Too many requests, please try again later.");
            }
        } else {
            filterChain.doFilter(request, response);
        }
    }

    // Простейший кэш бакетов в памяти (для прода лучше Redis)
    private static class BucketCache {
        private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

        public Bucket getBucket(String key) {
            return buckets.computeIfAbsent(key, k -> createNewBucket());
        }

        private Bucket createNewBucket() {
            Refill refill = Refill.greedy(5, Duration.ofMinutes(1)); // 5 запросов в минуту
            Bandwidth limit = Bandwidth.classic(5, refill);
            return Bucket4j.builder().addLimit(limit).build();
        }
    }
}