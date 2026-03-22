package com.rolling.api.global.page;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Set;

public final class PageableUtils {

    private PageableUtils() {
    }

    public static Pageable normalize(
            Pageable pageable,
            Sort defaultSort,
            Set<String> allowedSortProperties,
            int defaultSize,
            int maxSize
    ) {
        int pageNumber = pageable == null || pageable.getPageNumber() < 0 ? 0 : pageable.getPageNumber();
        int requestedSize = pageable == null ? defaultSize : pageable.getPageSize();
        int pageSize = requestedSize <= 0 ? defaultSize : Math.min(requestedSize, maxSize);

        Sort requestedSort = pageable == null ? Sort.unsorted() : pageable.getSort();
        Sort sanitizedSort = sanitizeSort(requestedSort, allowedSortProperties);
        if (sanitizedSort.isUnsorted()) {
            sanitizedSort = defaultSort;
        }

        return PageRequest.of(pageNumber, pageSize, sanitizedSort);
    }

    private static Sort sanitizeSort(Sort sort, Set<String> allowedSortProperties) {
        if (sort == null || sort.isUnsorted()) {
            return Sort.unsorted();
        }

        List<Sort.Order> allowedOrders = sort.stream()
                .filter(order -> allowedSortProperties.contains(order.getProperty()))
                .map(order -> new Sort.Order(order.getDirection(), order.getProperty()))
                .toList();

        return allowedOrders.isEmpty() ? Sort.unsorted() : Sort.by(allowedOrders);
    }
}
