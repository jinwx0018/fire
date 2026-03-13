package com.fire.recommendation.controller;

import com.fire.recommendation.common.Result;
import com.fire.recommendation.dto.LoginReq;
import com.fire.recommendation.dto.LoginVO;
import com.fire.recommendation.dto.RegisterReq;
import com.fire.recommendation.dto.UserInfoVO;
import com.fire.recommendation.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

@Tag(name = "用户管理", description = "用户注册、登录、个人信息、密码修改与找回")
@Slf4j
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private static final String AVATAR_SUBDIR = "avatar";
    private static final long MAX_AVATAR_SIZE = 2 * 1024 * 1024; // 2MB
    private static final String[] ALLOWED_EXT = { ".jpg", ".jpeg", ".png", ".gif", ".webp" };

    @Value("${app.upload-dir:./uploads}")
    private String uploadDir;

    private final UserService userService;

    @Operation(summary = "用户注册", description = "校验用户名、手机号格式及唯一性，密码加密入库，无需登录")
    @PostMapping("/register")
    public Result<Map<String, Object>> register(@Valid @RequestBody RegisterReq req) {
        Long userId = userService.register(req);
        return Result.ok(Map.of("userId", userId, "username", req.getUsername()));
    }

    @Operation(summary = "用户登录", description = "用户名+密码校验，返回 JWT Token，无需登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginReq req) {
        LoginVO vo = userService.login(req);
        return Result.ok(vo);
    }

    @Operation(summary = "获取当前用户信息", description = "需登录，返回当前用户的个人信息")
    @GetMapping("/info")
    public Result<UserInfoVO> info(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.ok(userService.getInfo(userId));
    }

    @Operation(summary = "修改用户信息", description = "需登录，可修改头像、手机号、邮箱")
    @PutMapping("/info")
    public Result<Void> updateInfo(HttpServletRequest request, @RequestBody Map<String, String> body) {
        Long userId = (Long) request.getAttribute("userId");
        userService.updateInfo(userId, body.get("avatar"), body.get("phone"), body.get("email"));
        return Result.ok();
    }

    @Operation(summary = "修改密码", description = "需登录，需提供原密码")
    @PutMapping("/password")
    public Result<Void> updatePassword(HttpServletRequest request, @RequestBody Map<String, String> body) {
        Long userId = (Long) request.getAttribute("userId");
        userService.updatePassword(userId, body.get("oldPassword"), body.get("newPassword"));
        return Result.ok();
    }

    @Operation(summary = "发送邮箱重置链接", description = "向邮箱发送重置密码链接，无需登录")
    @PostMapping("/password/sendEmail")
    public Result<Void> sendEmail(@RequestBody Map<String, String> body) {
        userService.sendResetEmail(body.get("email"));
        return Result.ok();
    }

    @Operation(summary = "通过 Token 重置密码", description = "邮箱链接中的 Token + 新密码，无需登录")
    @PostMapping("/password/resetByToken")
    public Result<Void> resetByToken(@RequestBody Map<String, String> body) {
        userService.resetPasswordByToken(body.get("token"), body.get("newPassword"));
        return Result.ok();
    }

    @Operation(summary = "上传头像", description = "需登录。上传图片文件，返回可访问的 URL")
    @PostMapping(value = "/avatar/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Map<String, String>> uploadAvatar(HttpServletRequest request, @RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Result.fail(400, "请选择文件");
        }
        if (file.getSize() > MAX_AVATAR_SIZE) {
            return Result.fail(400, "头像大小不能超过 2MB");
        }
        String originalFilename = file.getOriginalFilename();
        String ext = "";
        if (StringUtils.hasText(originalFilename)) {
            int i = originalFilename.lastIndexOf('.');
            if (i >= 0) ext = originalFilename.substring(i).toLowerCase();
        }
        boolean allowed = false;
        for (String e : ALLOWED_EXT) {
            if (e.equals(ext)) { allowed = true; break; }
        }
        if (!allowed) {
            return Result.fail(400, "仅支持图片：jpg、png、gif、webp");
        }
        try {
            Path dir = Paths.get(uploadDir, AVATAR_SUBDIR).toAbsolutePath();
            Files.createDirectories(dir);
            String filename = UUID.randomUUID().toString().replace("-", "") + ext;
            Path target = dir.resolve(filename);
            file.transferTo(target.toFile());
            String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath();
            String url = baseUrl + "/uploads/" + AVATAR_SUBDIR + "/" + filename;
            log.info("头像已保存: {}", target);
            return Result.ok(Map.of("url", url));
        } catch (IOException e) {
            log.warn("头像上传失败", e);
            return Result.fail(500, "上传失败");
        }
    }
}
