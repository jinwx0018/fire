package com.fire.recommendation.controller;

import com.fire.recommendation.common.Result;
import com.fire.recommendation.service.NewsCategoryDictService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "新闻分类", description = "用户端分类下拉")
@RestController
@RequestMapping("/news/category")
@RequiredArgsConstructor
public class NewsCategoryController {

    private final NewsCategoryDictService newsCategoryDictService;

    @Operation(summary = "新闻分类列表", description = "未删除，按 sort_order")
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> list() {
        return Result.ok(newsCategoryDictService.listOptions());
    }
}
