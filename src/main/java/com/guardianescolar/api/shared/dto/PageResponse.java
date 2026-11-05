package com.guardianescolar.api.shared.dto;

import java.util.List;

public record PageResponse<T>(
        List<T> items,
        int page,
        int size,
        long totalItems,
        int totalPages,
        boolean first,
        boolean last) {
}
