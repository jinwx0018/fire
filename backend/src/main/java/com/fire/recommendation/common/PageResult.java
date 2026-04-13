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
    /** 可选：推荐列表等场景下，本页统一策略说明（便于联调与用户理解） */
    private String pageHint;

    public static <T> PageResult<T> of(List<T> list, Long total, Integer pageNum, Integer pageSize) {
        int pages = (pageSize == null || pageSize <= 0) ? 0 : (int) ((total + pageSize - 1) / pageSize);
        return new PageResult<>(list, total, pageNum, pageSize, pages, null);
    }

    public static <T> PageResult<T> of(List<T> list, Long total, Integer pageNum, Integer pageSize, String pageHint) {
        PageResult<T> p = of(list, total, pageNum, pageSize);
        p.setPageHint(pageHint);
        return p;
    }
}
