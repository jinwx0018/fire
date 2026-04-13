package com.fire.recommendation.controller.admin;

import com.fire.recommendation.common.Result;
import com.fire.recommendation.component.PublicEndpointRateLimiter;
import com.fire.recommendation.dto.LoginReq;
import com.fire.recommendation.dto.LoginVO;
import com.fire.recommendation.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理端-登录", description = "仅管理员账号可登录管理端")
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminLoginController {

    private final UserService userService;
    private final PublicEndpointRateLimiter publicEndpointRateLimiter;

    @Operation(summary = "管理端登录", description = "用户名+密码校验，仅角色为 ADMIN 时返回 JWT；普通用户、作者不可登录管理端")
    @PostMapping("/login")
    public Result<LoginVO> login(HttpServletRequest request, @Valid @RequestBody LoginReq req) {
        publicEndpointRateLimiter.beforeAdminLogin(request, req.getUsername());
        return Result.ok(userService.loginForAdmin(req));
    }
}
