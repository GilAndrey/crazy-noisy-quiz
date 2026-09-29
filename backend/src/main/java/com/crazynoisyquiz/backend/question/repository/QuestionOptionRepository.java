package com.crazynoisyquiz.backend.question.repository;

import com.crazynoisyquiz.backend.question.model.QuestionOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QuestionOptionRepository extends JpaRepository<QuestionOption, UUID> {

    List<QuestionOption> findAllByQuestionIdOrderByOptionOrderAsc(UUID questionId);
}
