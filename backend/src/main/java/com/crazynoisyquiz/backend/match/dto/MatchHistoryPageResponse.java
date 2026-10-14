package com.crazynoisyquiz.backend.match.dto;

import java.util.List;

public record MatchHistoryPageResponse(
        // Inclui os dados necessários para navegar pelas páginas do histórico.
        List<MatchHistoryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
