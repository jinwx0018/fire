package com.fire.recommendation.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fire.recommendation.common.PageResult;
import com.fire.recommendation.common.Result;
import com.fire.recommendation.common.ResultCode;
import com.fire.recommendation.exception.BusinessException;
import com.fire.recommendation.service.ContentService;
import com.fire.recommendation.util.HttpClientIp;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "消防知识内容管理", description = "知识列表、详情、发布、收藏、分享、分类")
@RestController
@RequestMapping("/content")
@RequiredArgsConstructor
public class ContentController {

    private static final String EDITOR_IMAGE_SUBDIR = "knowledge_editor";
    private static final String COVER_IMAGE_SUBDIR = "knowledge_cover";
    private static final long MAX_EDITOR_IMAGE_SIZE = 5 * 1024 * 1024;
    /** 与头像上传一致：封面 2MB，jpg/png/gif/webp */
    private static final long MAX_COVER_IMAGE_SIZE = 2 * 1024 * 1024;
    private static final String[] EDITOR_IMAGE_EXT = {".jpg", ".jpeg", ".png", ".gif", ".webp"};

    private final ContentService contentService;

    @Value("${app.upload-dir:./uploads}")
    private String uploadDir;

    @Operation(summary = "内容分页列表（公开）", description = "仅返回已发布内容；按分类、标题模糊搜索，无需登录。sortBy=hot 按热度；否则按发布时间。timeOrder=asc 从旧到新，desc 从新到旧（仅非 hot 时生效）")
    @GetMapping("/list")
    public Result<IPage<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String title,
            @RequestParam(required = false, defaultValue = "latest") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String timeOrder) {
        IPage<Map<String, Object>> page = contentService.list(pageNum, pageSize, categoryId, title, sortBy, timeOrder);
        return Result.ok(page);
    }

    @Operation(summary = "我的知识列表", description = "需登录，返回当前作者可管理的全部状态内容")
    @GetMapping("/my/drafts")
    public Result<IPage<Map<String, Object>>> myDrafts(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String title) {
        Long userId = (Long) request.getAttribute("userId");
        IPage<Map<String, Object>> page = contentService.myDrafts(userId, pageNum, pageSize, categoryId, title);
        return Result.ok(page);
    }

    @Operation(summary = "知识评论列表", description = "需已发布内容；无需登录；带 Token 时返回每条 liked。sortBy=time 按时间倒序（默认）；sortBy=hot 按点赞倒序")
    @GetMapping("/{id}/comments")
    public Result<IPage<Map<String, Object>>> listContentComments(
            @Parameter(description = "内容ID") @PathVariable Long id,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false, defaultValue = "time") String sortBy,
            HttpServletRequest request) {
        Object attr = request.getAttribute("userId");
        Long viewerId = attr instanceof Long ? (Long) attr : null;
        return Result.ok(contentService.listContentComments(id, pageNum, pageSize, viewerId, sortBy));
    }

    @Operation(summary = "知识评论点赞/取消", description = "需登录；Redis key knowledge:comment:like:{commentId}")
    @PostMapping("/comment/like/{commentId}")
    public Result<Map<String, Object>> likeKnowledgeComment(
            @Parameter(description = "评论ID") @PathVariable Long commentId,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.ok(contentService.toggleKnowledgeCommentLike(userId, commentId));
    }

    @Operation(summary = "删除知识评论", description = "需登录，仅本人可删除自己的评论")
    @DeleteMapping("/comment/{commentId}")
    public Result<Void> deleteKnowledgeComment(
            @Parameter(description = "评论ID") @PathVariable Long commentId,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        contentService.deleteKnowledgeComment(userId, commentId);
        return Result.ok();
    }

    @Operation(summary = "发表评论", description = "需登录；body 含 content、可选 parentId")
    @PostMapping("/{id}/comments")
    public Result<Map<String, Object>> postContentComment(
            @Parameter(description = "内容ID") @PathVariable Long id,
            HttpServletRequest request,
            @RequestBody Map<String, Object> body) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        String text = body.get("content") != null ? body.get("content").toString() : null;
        Long parentId = null;
        Object p = body.get("parentId");
        if (p != null) {
            if (p instanceof Number) {
                parentId = ((Number) p).longValue();
            } else if (StringUtils.hasText(p.toString())) {
                parentId = Long.parseLong(p.toString().trim());
            }
        }
        return Result.ok(contentService.postContentComment(userId, id, text, parentId));
    }

    @Operation(summary = "内容详情", description = "获取单条知识详情；recordView=true 时按规则递增浏览量（作者/管理员/同访客窗口内重复请求不计）")
    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(
            @Parameter(description = "内容ID") @PathVariable Long id,
            @RequestParam(defaultValue = "true") boolean recordView,
            HttpServletRequest request) {
        Object attr = request.getAttribute("userId");
        Long userId = attr != null ? (Long) attr : null;
        Map<String, Object> data = contentService.getDetail(id, userId, recordView, HttpClientIp.resolve(request));
        return Result.ok(data);
    }

    @Operation(summary = "新增内容", description = "需登录，作者/管理员可发布，请求体含 title、categoryId、content、cover、summary、status")
    @PostMapping
    public Result<Map<String, Long>> save(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Long userId = (Long) request.getAttribute("userId");
        Long id = contentService.save(userId, body);
        return Result.ok(Map.of("id", id));
    }

    @Operation(summary = "修改内容", description = "需登录")
    @PutMapping("/{id}")
    public Result<Void> update(@Parameter(description = "内容ID") @PathVariable Long id, HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Long userId = (Long) request.getAttribute("userId");
        contentService.update(id, userId, body);
        return Result.ok();
    }

    @Operation(summary = "删除内容", description = "需登录，逻辑删除")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "内容ID") @PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        contentService.delete(id, userId);
        return Result.ok();
    }

    @Operation(summary = "发布内容", description = "需登录，将草稿发布为已发布状态")
    @PostMapping("/publish/{id}")
    public Result<Void> publish(@Parameter(description = "内容ID") @PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        contentService.publish(id, userId);
        return Result.ok();
    }

    @Operation(summary = "作者下架内容", description = "需登录；作者本人或管理员可将内容下架（status=2）")
    @PostMapping("/offline/{id}")
    public Result<Void> offline(@Parameter(description = "内容ID") @PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        contentService.changeStatus(id, userId, 2);
        return Result.ok();
    }

    @Operation(summary = "收藏内容", description = "需登录，同一用户同一内容仅一条收藏")
    @PostMapping("/collect/{contentId}")
    public Result<Map<String, Boolean>> collect(@Parameter(description = "内容ID") @PathVariable Long contentId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        contentService.collect(userId, contentId);
        return Result.ok(Map.of("collected", true));
    }

    @Operation(summary = "取消收藏", description = "需登录")
    @DeleteMapping("/collect/{contentId}")
    public Result<Void> uncollect(@Parameter(description = "内容ID") @PathVariable Long contentId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        contentService.uncollect(userId, contentId);
        return Result.ok();
    }

    @Operation(summary = "我的收藏列表", description = "需登录；module=knowledge|forum|news|equipment，默认 knowledge")
    @GetMapping("/collect/list")
    public Result<PageResult<Map<String, Object>>> collectList(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false, defaultValue = "knowledge") String module) {
        Long userId = (Long) request.getAttribute("userId");
        PageResult<Map<String, Object>> page = contentService.collectList(userId, pageNum, pageSize, module);
        return Result.ok(page);
    }

    @Operation(summary = "点赞/取消点赞", description = "需登录，第一次点赞、再次点击取消")
    @PostMapping("/like/{contentId}")
    public Result<Map<String, Object>> toggleLike(@Parameter(description = "内容ID") @PathVariable Long contentId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.ok(contentService.toggleLike(userId, contentId));
    }

    @Operation(summary = "生成分享链接", description = "返回前端配置的分享 URL，无需登录")
    @GetMapping("/share/{contentId}")
    public Result<Map<String, Object>> share(@Parameter(description = "内容ID") @PathVariable Long contentId) {
        return Result.ok(contentService.shareUrl(contentId));
    }

    @Operation(summary = "分类列表", description = "知识分类列表（id、name、sort），无需登录")
    @GetMapping("/category/list")
    public Result<List<Map<String, Object>>> categoryList() {
        return Result.ok(contentService.categoryList());
    }

    @Operation(summary = "富文本正文图片上传", description = "需登录。上传图片后返回 url，供编辑器插入")
    @PostMapping(value = "/editor/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Map<String, String>> uploadEditorImage(HttpServletRequest request, @RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Result.fail(400, "请选择图片");
        }
        if (file.getSize() > MAX_EDITOR_IMAGE_SIZE) {
            return Result.fail(400, "图片大小不能超过 5MB");
        }
        String originalFilename = file.getOriginalFilename();
        String ext = "";
        if (StringUtils.hasText(originalFilename)) {
            int i = originalFilename.lastIndexOf('.');
            if (i >= 0) ext = originalFilename.substring(i).toLowerCase();
        }
        boolean allowed = false;
        for (String e : EDITOR_IMAGE_EXT) {
            if (e.equals(ext)) {
                allowed = true;
                break;
            }
        }
        if (!allowed) {
            return Result.fail(400, "仅支持 jpg、png、gif、webp 图片");
        }
        try {
            Path dir = Paths.get(uploadDir, EDITOR_IMAGE_SUBDIR).toAbsolutePath();
            Files.createDirectories(dir);
            String filename = UUID.randomUUID().toString().replace("-", "") + ext;
            Path target = dir.resolve(filename);
            file.transferTo(target.toFile());
            String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath();
            String url = baseUrl + "/uploads/" + EDITOR_IMAGE_SUBDIR + "/" + filename;
            return Result.ok(Map.of("url", url));
        } catch (IOException e) {
            return Result.fail(500, "上传失败");
        }
    }

    @Operation(summary = "知识封面图上传", description = "需登录（作者/管理员）。规则同头像：≤2MB，jpg/png/gif/webp，返回可访问 URL 写入 cover 字段")
    @PostMapping(value = "/cover/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Map<String, String>> uploadCover(HttpServletRequest request, @RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Result.fail(400, "请选择图片");
        }
        if (file.getSize() > MAX_COVER_IMAGE_SIZE) {
            return Result.fail(400, "封面图片大小不能超过 2MB");
        }
        String originalFilename = file.getOriginalFilename();
        String ext = "";
        if (StringUtils.hasText(originalFilename)) {
            int i = originalFilename.lastIndexOf('.');
            if (i >= 0) ext = originalFilename.substring(i).toLowerCase();
        }
        boolean allowed = false;
        for (String e : EDITOR_IMAGE_EXT) {
            if (e.equals(ext)) {
                allowed = true;
                break;
            }
        }
        if (!allowed) {
            return Result.fail(400, "仅支持 jpg、png、gif、webp 图片");
        }
        try {
            Path dir = Paths.get(uploadDir, COVER_IMAGE_SUBDIR).toAbsolutePath();
            Files.createDirectories(dir);
            String filename = UUID.randomUUID().toString().replace("-", "") + ext;
            Path target = dir.resolve(filename);
            file.transferTo(target.toFile());
            String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath();
            String url = baseUrl + "/uploads/" + COVER_IMAGE_SUBDIR + "/" + filename;
            return Result.ok(Map.of("url", url));
        } catch (IOException e) {
            return Result.fail(500, "上传失败");
        }
    }
}
