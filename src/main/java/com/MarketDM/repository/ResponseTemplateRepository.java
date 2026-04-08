package com.MarketDM.repository;

import com.MarketDM.entity.support.ResponseTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ResponseTemplateRepository extends JpaRepository<ResponseTemplate, Long> {
    Optional<ResponseTemplate> findByShortcutAndActiveTrue(String shortcut);
    List<ResponseTemplate> findByCategoryAndActiveTrueOrderByUsageCountDesc(String category);
}
