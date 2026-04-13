package com.fire.recommendation.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fire.recommendation.common.Result;
import com.fire.recommendation.service.AuthorApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "管理端-作者申请审核", description = "待审核列表、已处理记录分页、通过、驳回")
@RestController
@RequestMapping("/admin/author-applications")
@RequiredArgsConstructor
public class AdminAuthorApplicationController {

    private final AuthorApplicationService authorApplicationService;

    @Operation(summary = "待审核作者申请列表", description = "按提交时间倒序遍历；同一 userId 仅保留最新一条待审核，避免重复展示")
    @GetMapping("/pending")
    public Result<List<Map<String, Object>>> listPending() {
        return Result.ok(authorApplicationService.listPending());
    }

    @Operation(summary = "已处理作者申请分页", description = "状态为已通过、已驳回的记录，按审核时间倒序，便于查看刚处理的结果")
    @GetMapping("/processed/page")
    public Result<IPage<Map<String, Object>>> processedPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.ok(authorApplicationService.pageProcessedApplications(pageNum, pageSize));
    }

    @Operation(summary = "通过作者申请", description = "将用户角色改为 AUTHOR")
    @PutMapping("/{id}/approve")
    public Result<Void> approve(@Parameter(description = "申请ID") @PathVariable Long id, HttpServletRequest request,
                                @RequestBody(required = false) Map<String, String> body) {
        Long adminUserId = (Long) request.getAttribute("userId");
        String reviewRemark = body != null ? body.get("reviewRemark") : null;
        authorApplicationService.approve(id, adminUserId, reviewRemark);
        return Result.ok();
    }

    @Operation(summary = "驳回作者申请")
    @PutMapping("/{id}/reject")
    public Result<Void> reject(@Parameter(description = "申请ID") @PathVariable Long id, HttpServletRequest request, @RequestBody(required = false) Map<String, String> body) {
        Long adminUserId = (Long) request.getAttribute("userId");
        String reason = body != null ? body.get("rejectReason") : null;
        authorApplicationService.reject(id, adminUserId, reason);
        return Result.ok();
    }
}
