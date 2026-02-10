package com.example.demo.DTO;

import java.time.LocalDate;

public class UserUpdateDto {
    private String email;
    private String name;
    private LocalDate birth;

    // Конструкторы
    public UserUpdateDto() {
    }

    public UserUpdateDto(String email, String name, LocalDate birth) {
        this.email = email;
        this.name = name;
        this.birth = birth;
    }

    // Геттеры и сеттеры
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDate getBirth() {
        return birth;
    }

    public void setBirth(LocalDate birth) {
        this.birth = birth;
    }
}