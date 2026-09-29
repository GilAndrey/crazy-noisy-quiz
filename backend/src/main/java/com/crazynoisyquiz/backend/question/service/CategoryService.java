package com.crazynoisyquiz.backend.question.service;

import com.crazynoisyquiz.backend.question.model.Category;
import com.crazynoisyquiz.backend.question.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<Category> findActiveCategories() {
        return categoryRepository.findAllByActiveTrueOrderByNameAsc();
    }
}
