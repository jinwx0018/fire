package com.fire.recommendation.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fire.recommendation.common.Result;
import com.fire.recommendation.exception.BusinessException;
import com.fire.recommendation.service.ContentService;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Tag(name = "管理端-知识内容", description = "知识分页、分类增删改、内容审核、知识评论审核")
@RestController
@RequestMapping("/admin/content")
@RequiredArgsConstructor
public class AdminContentCategoryController {

    private static final String EDITOR_IMAGE_SUBDIR = "knowledge_editor";
    private static final long MAX_EDITOR_IMAGE_SIZE = 5 * 1024 * 1024;
    private static final String[] EDITOR_IMAGE_EXT = {".jpg", ".jpeg", ".png", ".gif", ".webp"};

    private final ContentService contentService;

    @Value("${app.upload-dir:./uploads}")
    private String uploadDir;

    @Operation(summary = "知识评论审核上下文", description = "完整知识正文、父评论全文、本条评论；GET /admin/content/comment/{id}/audit-detail")
    @GetMapping("/comment/{id}/audit-detail")
    public Result<Map<String, Object>> knowledgeCommentAuditDetail(@Parameter(description = "评论ID") @PathVariable Long id) {
        return Result.ok(contentService.adminKnowledgeCommentAuditDetail(id));
    }

    @Operation(summary = "知识评论分页（管理端）", description = "可选 contentId、keyword、status；GET /admin/content/comment/page")
    @GetMapping("/comment/page")
    public Result<IPage<Map<String, Object>>> knowledgeCommentPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Long contentId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        return Result.ok(contentService.adminKnowledgeCommentPage(pageNum, pageSize, contentId, keyword, status));
    }

    @Operation(summary = "知识评论显示/隐藏", description = "PUT /admin/content/comment/{id}/status，body: { \"status\": 0|1 }")
    @PutMapping("/comment/{id}/status")
    public Result<Void> knowledgeCommentSetStatus(HttpServletRequest request, @PathVariable Long id, @RequestBody Map<String, Object> body) {
        if (body == null || body.get("status") == null) {
            throw new BusinessException("status 不能为空");
        }
        int st;
        try {
            st = Integer.parseInt(Objects.requireNonNull(body.get("status")).toString().trim());
        } catch (Exception e) {
            throw new BusinessException("status 无效");
        }
        Long op = (Long) request.getAttribute("userId");
        contentService.adminKnowledgeCommentSetStatus(id, st, op);
        return Result.ok();
    }

    @Operation(summary = "删除知识评论（逻辑删除）", description = "DELETE /admin/content/comment/{id}")
    @DeleteMapping("/comment/{id}")
    public Result<Void> knowledgeCommentDelete(HttpServletRequest request, @Parameter(description = "评论ID") @PathVariable Long id) {
        Long op = (Long) request.getAttribute("userId");
        contentService.adminKnowledgeCommentDelete(id, op);
        return Result.ok();
    }

    @Operation(summary = "知识内容分页（管理端）", description = "需管理员。status 为空时返回全部状态；可筛选 categoryId、title")
    @GetMapping("/page")
    public Result<IPage<Map<String, Object>>> contentPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Integer status) {
        return Result.ok(contentService.adminContentPage(pageNum, pageSize, categoryId, title, status));
    }

    @Operation(summary = "回收站分页（管理端）", description = "需管理员。返回已删除内容")
    @GetMapping("/recycle/page")
    public Result<com.fire.recommendation.common.PageResult<Map<String, Object>>> recyclePage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.ok(contentService.adminRecyclePage(pageNum, pageSize));
    }

    @Operation(summary = "新增分类", description = "需登录。请求体 name、sort")
    @PostMapping("/category")
    public Result<Map<String, Long>> saveCategory(@RequestBody Map<String, Object> body) {
        if (body == null) {
            return Result.fail(400, "请求体不能为空");
        }
        Object nameObj = body.get("name");
        String name = nameObj != null ? nameObj.toString().trim() : null;
        if (!StringUtils.hasText(name)) {
            return Result.fail(400, "分类名称不能为空");
        }
        Integer sort = body.get("sort") != null ? Integer.valueOf(body.get("sort").toString()) : 0;
        Long id = contentService.adminSaveCategory(name, sort);
        return Result.ok(Map.of("id", id));
    }

    @Operation(summary = "修改分类", description = "需登录。请求体 name、sort 至少传一项；传 name 时不可为空")
    @PutMapping("/category/{id}")
    public Result<Void> updateCategory(@Parameter(description = "分类ID") @PathVariable Long id, @RequestBody Map<String, Object> body) {
        if (body == null) {
            return Result.fail(400, "请求体不能为空");
        }
        String name = null;
        if (body.containsKey("name")) {
            Object raw = body.get("name");
            name = raw != null ? raw.toString().trim() : "";
            if (!StringUtils.hasText(name)) {
                return Result.fail(400, "分类名称不能为空");
            }
        }
        Integer sort = body.get("sort") != null ? Integer.valueOf(body.get("sort").toString()) : null;
        if (name == null && sort == null) {
            return Result.fail(400, "请至少提供名称或排序");
        }
        contentService.adminUpdateCategory(id, name, sort);
        return Result.ok();
    }

    @Operation(summary = "删除分类", description = "需登录")
    @DeleteMapping("/category/{id}")
    public Result<Void> deleteCategory(@Parameter(description = "分类ID") @PathVariable Long id) {
        contentService.adminDeleteCategory(id);
        return Result.ok();
    }

    @Operation(summary = "审核知识内容", description = "仅对待审核(status=3)内容。status：1=通过(已发布)，0=驳回(改为草稿)；驳回时请传 rejectReason")
    @PutMapping("/audit/{id}")
    public Result<Void> auditContent(
            @Parameter(description = "内容ID") @PathVariable Long id,
            HttpServletRequest request,
            @RequestBody Map<String, Object> body) {
        Long operatorId = (Long) request.getAttribute("userId");
        Integer status = body.get("status") != null ? Integer.valueOf(body.get("status").toString()) : null;
        Object reasonObj = body.get("rejectReason") != null ? body.get("rejectReason") : body.get("reject_reason");
        String rejectReason = reasonObj != null ? reasonObj.toString().trim() : null;
        if (rejectReason != null && rejectReason.isEmpty()) rejectReason = null;
        contentService.auditContent(id, status, rejectReason, operatorId);
        return Result.ok();
    }

    @Operation(summary = "管理端切换内容状态", description = "status 1=发布，2=下架")
    @PutMapping("/status/{id}")
    public Result<Void> changeStatus(@PathVariable Long id, HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Long userId = (Long) request.getAttribute("userId");
        Integer status = body.get("status") != null ? Integer.valueOf(body.get("status").toString()) : null;
        contentService.changeStatus(id, userId, status);
        return Result.ok();
    }

    @Operation(summary = "回收站恢复内容", description = "仅管理员。module：KNOWLEDGE（知识，默认）/ NEWS（新闻）/ FORUM（帖子）")
    @PutMapping("/restore/{id}")
    public Result<Void> restore(
            @PathVariable Long id,
            @RequestParam(defaultValue = "KNOWLEDGE") String module,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        contentService.restore(id, userId, module);
        return Result.ok();
    }

    @Operation(summary = "回收站彻底删除", description = "物理删除，仅管理员，且内容须已在回收站。module 同恢复接口")
    @DeleteMapping("/recycle/{id}")
    public Result<Void> permanentDelete(
            @PathVariable Long id,
            @RequestParam(defaultValue = "KNOWLEDGE") String module,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        contentService.permanentDelete(id, userId, module);
        return Result.ok();
    }

    @Operation(summary = "清空回收站", description = "物理删除全部逻辑删除记录，仅管理员")
    @DeleteMapping("/recycle/all")
    public Result<Void> purgeRecycle(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        contentService.purgeRecycle(userId);
        return Result.ok();
    }

    @Operation(summary = "批量逻辑删除", description = "请求体 { ids: [1,2,3] }，仅管理员")
    @PostMapping("/batch/delete")
    public Result<Void> batchDelete(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        contentService.batchDelete(parseIdList(body), userId);
        return Result.ok();
    }

    @Operation(summary = "批量下架", description = "请求体 { ids: [...] }，仅对已发布内容下架，仅管理员")
    @PostMapping("/batch/offline")
    public Result<Void> batchOffline(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        contentService.batchOffline(parseIdList(body), userId);
        return Result.ok();
    }

    @Operation(summary = "批量恢复发布", description = "请求体 { ids: [...] }，仅对已下架内容恢复为已发布，仅管理员")
    @PostMapping("/batch/publish")
    public Result<Void> batchPublish(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        contentService.batchPublish(parseIdList(body), userId);
        return Result.ok();
    }

    private static List<Long> parseIdList(Map<String, Object> body) {
        if (body == null) return Collections.emptyList();
        Object raw = body.get("ids");
        if (!(raw instanceof List)) {
            return Collections.emptyList();
        }
        List<Long> ids = new ArrayList<>();
        for (Object o : (List<?>) raw) {
            Long id = parseLongFlexible(o);
            if (id != null) {
                ids.add(id);
            }
        }
        return ids;
    }

    /** JSON 反序列化后元素可能是 Integer/Long/Double/BigDecimal，避免 toString 为 "1.0" 时 Long.valueOf 失败 */
    private static Long parseLongFlexible(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number) {
            return ((Number) o).longValue();
        }
        String s = o.toString().trim();
        if (s.isEmpty()) {
            return null;
        }
        int dot = s.indexOf('.');
        if (dot > 0) {
            s = s.substring(0, dot);
        }
        return Long.parseLong(s);
    }

    @Operation(summary = "富文本正文图片上传（管理端）", description = "需管理员。上传图片后返回 url")
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
}
