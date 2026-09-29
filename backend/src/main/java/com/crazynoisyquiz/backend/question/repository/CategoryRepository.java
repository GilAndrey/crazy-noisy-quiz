package com.crazynoisyquiz.backend.question.repository;

import com.crazynoisyquiz.backend.question.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    List<Category> findAllByActiveTrueOrderByNameAsc();
}
