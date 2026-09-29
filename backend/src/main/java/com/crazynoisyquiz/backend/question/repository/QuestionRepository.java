package com.crazynoisyquiz.backend.question.repository;

import com.crazynoisyquiz.backend.question.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QuestionRepository extends JpaRepository<Question, UUID> {

    List<Question> findAllByCategoryIdAndActiveTrue(UUID categoryId);
}
