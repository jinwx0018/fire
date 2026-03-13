package com.fire.recommendation.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fire.recommendation.common.Result;
import com.fire.recommendation.service.NewsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "消防新闻", description = "新闻列表、详情，支持按地区、标题、时间、紧急等级筛选")
@RestController
@RequestMapping("/news")
@RequiredArgsConstructor
public class NewsController {

    private final NewsService newsService;

    @Operation(summary = "新闻分页列表", description = "支持 region、title、orderBy(publishTime/urgency)、order(asc/desc)")
    @GetMapping("/list")
    public Result<IPage<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String orderBy,
            @RequestParam(required = false) String order) {
        IPage<Map<String, Object>> page = newsService.list(pageNum, pageSize, region, title, orderBy, order);
        return Result.ok(page);
    }

    @Operation(summary = "新闻详情", description = "标题、内容、地区、紧急等级、发布人、发布时间等")
    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@Parameter(description = "新闻ID") @PathVariable Long id) {
        return Result.ok(newsService.getDetail(id));
    }
}
