package com.MarketDM.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Чистые пути → перенаправление на .html файлы в static/
        registry.addViewController("/login").setViewName("forward:/login.html");
        registry.addViewController("/register").setViewName("forward:/register.html");
        registry.addViewController("/products").setViewName("forward:/products.html");
        registry.addViewController("/categories").setViewName("forward:/categories.html");
        registry.addViewController("/search").setViewName("forward:/search.html");
        registry.addViewController("/cart").setViewName("forward:/cart.html");
        registry.addViewController("/profile").setViewName("forward:/profile.html");
        registry.addViewController("/orders").setViewName("forward:/orders.html");
        registry.addViewController("/delivery").setViewName("forward:/delivery");
        registry.addViewController("/return").setViewName("forward:/return.html");
        registry.addViewController("/faq").setViewName("forward:/faq.html");
        registry.addViewController("/feedback").setViewName("forward:/feedback.html");
        registry.addViewController("/about").setViewName("forward:/about.html");
        registry.addViewController("/jobs").setViewName("forward:/jobs.html");
        registry.addViewController("/blog").setViewName("forward:/blog.html");
        registry.addViewController("/terms").setViewName("forward:/terms.html");
        registry.addViewController("/privacy").setViewName("forward:/privacy.html");

        // Главная: / → index.html
        registry.addViewController("/").setViewName("forward:/index.html");
    }
}
