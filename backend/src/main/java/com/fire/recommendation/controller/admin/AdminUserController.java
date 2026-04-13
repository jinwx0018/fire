package com.fire.recommendation.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fire.recommendation.common.Result;
import com.fire.recommendation.dto.UserInfoVO;
import com.fire.recommendation.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@Tag(name = "管理端-用户管理", description = "管理员对用户的增删改查、冻结解冻、强制下线、注销")
@RestController
@RequestMapping("/admin/user")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    @Operation(summary = "用户列表（分页）", description = "需登录。支持 username 模糊、role 筛选")
    @GetMapping("/list")
    public Result<IPage<UserInfoVO>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String role) {
        IPage<UserInfoVO> page = userService.adminUserList(pageNum, pageSize, username, role);
        return Result.ok(page);
    }

    @Operation(summary = "用户详情（按ID）", description = "需登录。用于编辑回显")
    @GetMapping("/{id}")
    public Result<UserInfoVO> getById(@Parameter(description = "用户ID") @PathVariable Long id) {
        return Result.ok(userService.adminGetUser(id));
    }

    @Operation(summary = "新增用户", description = "需登录。请求体：username、password、phone 必填；email、avatar、role(USER/AUTHOR/ADMIN)、status(0禁用 1正常) 可选")
    @PostMapping
    public Result<Map<String, Long>> create(@RequestBody Map<String, Object> body) {
        Long id = userService.adminCreateUser(body);
        return Result.ok(Map.of("id", id));
    }

    @Operation(summary = "修改用户", description = "需登录。可修改 username、phone、email、avatar、role、status、password（传则重置）")
    @PutMapping("/{id}")
    public Result<Void> update(
            @Parameter(description = "用户ID") @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        userService.adminUpdateUser(id, body, (Long) request.getAttribute("userId"));
        return Result.ok();
    }

    @Operation(summary = "删除用户", description = "需登录。账号注销（软删除）")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "用户ID") @PathVariable Long id, HttpServletRequest request) {
        userService.adminDeleteUser(id, (Long) request.getAttribute("userId"));
        return Result.ok();
    }

    @Operation(summary = "冻结用户", description = "需登录。将 status 置为 0，并使旧 token 失效")
    @PutMapping("/{id}/freeze")
    public Result<Void> freeze(@PathVariable Long id, HttpServletRequest request) {
        userService.adminUpdateUser(id, Map.of("status", 0, "forceLogout", true), (Long) request.getAttribute("userId"));
        return Result.ok();
    }

    @Operation(summary = "解冻用户", description = "需登录。将 status 置为 1")
    @PutMapping("/{id}/unfreeze")
    public Result<Void> unfreeze(@PathVariable Long id, HttpServletRequest request) {
        userService.adminUpdateUser(id, Map.of("status", 1), (Long) request.getAttribute("userId"));
        return Result.ok();
    }

    @Operation(summary = "强制下线用户", description = "需登录。使该用户旧 token 全部失效")
    @PutMapping("/{id}/force-logout")
    public Result<Void> forceLogout(@PathVariable Long id) {
        userService.adminForceLogoutUser(id);
        return Result.ok();
    }
}
