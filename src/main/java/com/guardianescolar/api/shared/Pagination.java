package com.guardianescolar.api.shared;

import com.guardianescolar.api.shared.dto.PageResponse;
import java.util.List;

public final class Pagination {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    private Pagination() {
    }

    public static int size(int value) {
        return Math.min(MAX_SIZE, Math.max(1, value));
    }

    public static <T> PageResponse<T> slice(List<T> source, int requestedPage, int requestedSize) {
        int page = Math.max(0, requestedPage);
        int size = size(requestedSize);
        long totalItems = source.size();
        int totalPages = totalItems == 0 ? 0 : (int) ((totalItems + size - 1) / size);
        long requestedFrom = (long) page * size;
        int from = requestedFrom >= source.size() ? source.size() : (int) requestedFrom;
        int to = Math.min(from + size, source.size());
        return new PageResponse<>(source.subList(from, to), page, size, totalItems, totalPages,
                page == 0, totalPages == 0 || page >= totalPages - 1);
    }
}
