package com.MarketDM.service;

import com.MarketDM.entity.support.ResponseTemplate;
import com.MarketDM.repository.ResponseTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TemplateService {

    private final ResponseTemplateRepository templateRepository;

    public String processMessage(String message) {
        if (message == null || !message.startsWith("!")) return message;
        String shortcut = message.split(" ", 2)[0];
        return templateRepository.findByShortcutAndActiveTrue(shortcut)
                .map(template -> {
                    template.setUsageCount(template.getUsageCount() + 1);
                    template.setLastUsed(LocalDateTime.now());
                    templateRepository.save(template);
                    return template.getContent();
                })
                .orElse(message);
    }

    public List<ResponseTemplate> getSuggestions(String category) {
        return templateRepository.findByCategoryAndActiveTrueOrderByUsageCountDesc(category);
    }
}