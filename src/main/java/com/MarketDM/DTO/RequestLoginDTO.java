package com.MarketDM.DTO;

import lombok.Data;

@Data
public class RequestLoginDTO {
    private String email;
    private String password;
}