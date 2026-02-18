package com.MarketDM.service;

import com.MarketDM.DTO.CategoryDto;
import com.MarketDM.entity.Category;
import com.MarketDM.exception.ResourceNotFoundException;
import com.MarketDM.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<CategoryDto> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public CategoryDto getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
        return mapToDto(category);
    }

    @Transactional
    public CategoryDto createCategory(CategoryDto categoryDto) {
        Category category = Category.builder()
                .name(categoryDto.getName())
                .description(categoryDto.getDescription())
                .build();
        Category saved = categoryRepository.save(category);
        return mapToDto(saved);
    }

    @Transactional
    public CategoryDto updateCategory(Long id, CategoryDto categoryDto) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
        if (categoryDto.getName() != null) category.setName(categoryDto.getName());
        if (categoryDto.getDescription() != null) category.setDescription(categoryDto.getDescription());
        Category updated = categoryRepository.save(category);
        return mapToDto(updated);
    }

    @Transactional
    public void deleteCategory(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Category not found with id: " + id);
        }
        categoryRepository.deleteById(id);
    }

    private CategoryDto mapToDto(Category category) {
        return CategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .build();
    }
}