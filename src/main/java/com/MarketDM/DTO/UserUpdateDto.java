package com.MarketDM.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserUpdateDto {
    @NotBlank
    @Email
    private String email;
    private String password; // может быть null, если не меняется
    @NotBlank
    private String firstName;
    @NotBlank
    private String lastName;
    private String avatarUrl;
    private Boolean enabled; // примитив – всегда передаётся
}