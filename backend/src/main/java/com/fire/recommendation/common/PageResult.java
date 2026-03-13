package com.fire.recommendation.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {
    private List<T> list;
    private Long total;
    private Integer pageNum;
    private Integer pageSize;
    private Integer pages;

    public static <T> PageResult<T> of(List<T> list, Long total, Integer pageNum, Integer pageSize) {
        int pages = (pageSize == null || pageSize <= 0) ? 0 : (int) ((total + pageSize - 1) / pageSize);
        return new PageResult<>(list, total, pageNum, pageSize, pages);
    }
}
