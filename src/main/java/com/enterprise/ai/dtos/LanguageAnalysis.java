package com.enterprise.ai.dtos;

import java.util.List;

public record LanguageAnalysis(
        String language,
        int popularityScore,       // 1-10
        String primaryUseCase,
        List<String> topFrameworks,
        String learningDifficulty  // "Easy", "Medium", "Hard"
) {}
