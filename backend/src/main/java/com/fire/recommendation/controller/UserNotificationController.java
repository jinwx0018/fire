package com.fire.recommendation.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fire.recommendation.common.Result;
import com.fire.recommendation.service.UserNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "用户通知", description = "站内消息列表与已读")
@RestController
@RequestMapping("/user/notifications")
@RequiredArgsConstructor
public class UserNotificationController {

    private final UserNotificationService userNotificationService;

    @Operation(summary = "通知分页列表", description = "需登录")
    @GetMapping
    public Result<IPage<Map<String, Object>>> page(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer isRead) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.ok(userNotificationService.pageForUser(userId, pageNum, pageSize, isRead));
    }

    @Operation(summary = "未读数量", description = "需登录")
    @GetMapping("/unread-count")
    public Result<Map<String, Long>> unreadCount(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.ok(Map.of("count", userNotificationService.unreadCount(userId)));
    }

    @Operation(summary = "通知详情", description = "需登录，仅可查看本人消息")
    @GetMapping("/detail/{id}")
    public Result<Map<String, Object>> detail(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.ok(userNotificationService.getDetail(userId, id));
    }

    @Operation(summary = "标记单条已读", description = "需登录")
    @PutMapping("/{id}/read")
    public Result<Void> markRead(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        userNotificationService.markRead(userId, id);
        return Result.ok();
    }

    @Operation(summary = "全部标记已读", description = "需登录")
    @PutMapping("/read-all")
    public Result<Void> markAllRead(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        userNotificationService.markAllRead(userId);
        return Result.ok();
    }
}
