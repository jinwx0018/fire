package com.fire.recommendation.controller;

import com.fire.recommendation.common.Result;
import com.fire.recommendation.dto.LoginReq;
import com.fire.recommendation.dto.LoginVO;
import com.fire.recommendation.dto.RegisterReq;
import com.fire.recommendation.dto.UserInfoVO;
import com.fire.recommendation.component.PublicEndpointRateLimiter;
import com.fire.recommendation.service.AuthorApplicationService;
import com.fire.recommendation.service.UserKnowledgeCategoryBlockService;
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
import java.util.ArrayList;
import java.util.List;
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
    private final UserKnowledgeCategoryBlockService knowledgeCategoryBlockService;
    private final AuthorApplicationService authorApplicationService;
    private final PublicEndpointRateLimiter publicEndpointRateLimiter;

    @Operation(summary = "用户注册", description = "校验用户名、手机号格式及唯一性，密码加密入库，无需登录")
    @PostMapping("/register")
    public Result<Map<String, Object>> register(HttpServletRequest request, @Valid @RequestBody RegisterReq req) {
        publicEndpointRateLimiter.beforeRegister(request);
        Long userId = userService.register(req);
        return Result.ok(Map.of("userId", userId, "username", req.getUsername()));
    }

    @Operation(summary = "用户登录", description = "用户名+密码校验，返回 JWT Token，无需登录")
    @PostMapping("/login")
    public Result<LoginVO> login(HttpServletRequest request, @Valid @RequestBody LoginReq req) {
        publicEndpointRateLimiter.beforeUserLogin(request, req.getUsername());
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

    @Operation(summary = "智能推荐·屏蔽的知识分类", description = "需登录。返回 categoryIds；智能推荐列表会完全排除这些分类下的内容（筛选阶段）")
    @GetMapping("/knowledge/blocked-categories")
    public Result<Map<String, Object>> listBlockedKnowledgeCategories(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<Long> ids = knowledgeCategoryBlockService.listBlockedCategoryIds(userId);
        return Result.ok(Map.of("categoryIds", ids));
    }

    @Operation(summary = "智能推荐·设置屏蔽分类", description = "需登录。请求体 categoryIds 数组，全量替换；最多 30 个，须为有效分类 ID")
    @PutMapping("/knowledge/blocked-categories")
    public Result<Void> replaceBlockedKnowledgeCategories(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Long userId = (Long) request.getAttribute("userId");
        List<Long> ids = parseIdList(body != null ? body.get("categoryIds") : null);
        knowledgeCategoryBlockService.replaceBlockedCategories(userId, ids);
        return Result.ok();
    }

    private static List<Long> parseIdList(Object raw) {
        if (raw == null) {
            return List.of();
        }
        if (raw instanceof List<?> list) {
            List<Long> out = new ArrayList<>();
            for (Object o : list) {
                if (o == null) {
                    continue;
                }
                if (o instanceof Number n) {
                    out.add(n.longValue());
                } else {
                    try {
                        out.add(Long.parseLong(o.toString().trim()));
                    } catch (NumberFormatException ignored) {
                        /* skip */
                    }
                }
            }
            return out;
        }
        return List.of();
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
    public Result<Void> sendEmail(HttpServletRequest request, @RequestBody Map<String, String> body) {
        publicEndpointRateLimiter.beforePasswordResetEmail(request);
        userService.sendResetEmail(body.get("email"));
        return Result.ok();
    }

    @Operation(summary = "通过 Token 重置密码", description = "邮箱链接中的 Token + 新密码，无需登录")
    @PostMapping("/password/resetByToken")
    public Result<Void> resetByToken(@RequestBody Map<String, String> body) {
        userService.resetPasswordByToken(body.get("token"), body.get("newPassword"));
        return Result.ok();
    }

    @Operation(summary = "申请成为作者", description = "需登录。提交后由管理员审核，通过后获得作者权限")
    @PostMapping("/author/apply")
    public Result<Void> applyAuthor(HttpServletRequest request, @RequestBody(required = false) Map<String, String> body) {
        Long userId = (Long) request.getAttribute("userId");
        String applyReason = body != null ? body.get("applyReason") : null;
        String attachments = body != null ? body.get("attachments") : null;
        authorApplicationService.apply(userId, applyReason, attachments);
        return Result.ok();
    }

    @Operation(summary = "刷新访问令牌", description = "通过 refreshToken 换取新的 accessToken 与 refreshToken")
    @PostMapping("/refresh-token")
    public Result<LoginVO> refreshToken(@RequestBody Map<String, String> body) {
        return Result.ok(userService.refreshToken(body != null ? body.get("refreshToken") : null));
    }

    @Operation(summary = "退出所有设备", description = "需登录。执行后旧 access/refresh token 全部失效")
    @PostMapping("/logoutAll")
    public Result<Void> logoutAll(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        userService.forceLogout(userId);
        return Result.ok();
    }

    @Operation(summary = "自助注销账号", description = "需登录。校验密码后软删除账号，管理员不可在用户端注销")
    @PostMapping("/account/delete")
    public Result<Void> selfDeleteAccount(HttpServletRequest request, @RequestBody Map<String, String> body) {
        Long userId = (Long) request.getAttribute("userId");
        userService.selfDeleteAccount(userId, body != null ? body.get("password") : null);
        return Result.ok();
    }

    @Operation(summary = "上传作者申请附件", description = "需登录。上传文件后返回 URL，可填入申请时的 attachments 字段")
    @PostMapping(value = "/author/attachment/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Map<String, String>> uploadAuthorAttachment(HttpServletRequest request, @RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Result.fail(400, "请选择文件");
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            return Result.fail(400, "附件大小不能超过 5MB");
        }
        String originalFilename = file.getOriginalFilename();
        String ext = "";
        if (StringUtils.hasText(originalFilename)) {
            int i = originalFilename.lastIndexOf('.');
            if (i >= 0) ext = originalFilename.substring(i).toLowerCase();
        }
        String[] allowed = { ".jpg", ".jpeg", ".png", ".gif", ".webp", ".pdf" };
        boolean ok = false;
        for (String e : allowed) {
            if (e.equals(ext)) {
                ok = true;
                break;
            }
        }
        if (!ok) {
            return Result.fail(400, "仅支持图片或 pdf");
        }
        String subdir = "author_attachments";
        try {
            Path dir = Paths.get(uploadDir, subdir).toAbsolutePath();
            Files.createDirectories(dir);
            String filename = UUID.randomUUID().toString().replace("-", "") + ext;
            Path target = dir.resolve(filename);
            file.transferTo(target.toFile());
            String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath();
            String url = baseUrl + "/uploads/" + subdir + "/" + filename;
            log.info("作者申请附件已保存: {}", target);
            return Result.ok(Map.of("url", url));
        } catch (IOException e) {
            log.warn("作者申请附件上传失败", e);
            return Result.fail(500, "上传失败");
        }
    }

    @Operation(summary = "我的作者申请状态", description = "需登录。同一用户仅返回按创建时间最新的一条申请（含审核中/已通过/已驳回的全字段）；authorRoleActive 表示当前账号是否仍为作者或管理员（管理端降级后可能为 false，而记录 status 仍为 APPROVED）")
    @GetMapping("/author/application")
    public Result<Map<String, Object>> myAuthorApplication(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.ok(authorApplicationService.getMyApplication(userId));
    }

    @Operation(summary = "我的作者申请历史", description = "需登录。返回当前用户全部申请记录，按提交时间倒序（最新在上）；含 authorRoleActive 与 records 数组")
    @GetMapping("/author/applications")
    public Result<Map<String, Object>> myAuthorApplications(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.ok(authorApplicationService.getMyApplicationsOverview(userId));
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
