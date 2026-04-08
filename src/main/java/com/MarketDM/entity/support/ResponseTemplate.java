package com.MarketDM.entity.support;

import lombok.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "response_templates")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResponseTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 5000)
    private String content;

    private String category;

    @Column(unique = true)
    private String shortcut;

    private boolean active = true;

    private int usageCount = 0;
    private LocalDateTime lastUsed;
}
