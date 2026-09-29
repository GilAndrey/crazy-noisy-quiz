package com.crazynoisyquiz.backend.question.controller;

import com.crazynoisyquiz.backend.question.model.Category;
import com.crazynoisyquiz.backend.question.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<Category>> findCategories() {
        List<Category> categories = categoryService.findActiveCategories();
        return ResponseEntity.ok(categories);
    }
}
