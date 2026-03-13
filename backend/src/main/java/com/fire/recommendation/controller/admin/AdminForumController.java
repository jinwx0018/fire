package com.fire.recommendation.controller.admin;

import com.fire.recommendation.common.Result;
import com.fire.recommendation.service.ForumService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "管理端-论坛审核", description = "帖子审核通过/驳回")
@RestController
@RequestMapping("/admin/forum")
@RequiredArgsConstructor
public class AdminForumController {

    private final ForumService forumService;

    @Operation(summary = "帖子审核", description = "需登录/管理员。status：1 通过，-1 驳回；驳回时可填 rejectReason")
    @PutMapping("/post/audit/{id}")
    public Result<Void> auditPost(@Parameter(description = "帖子ID") @PathVariable Long id, @RequestBody Map<String, Object> body) {
        Integer status = Integer.valueOf(body.get("status").toString());
        String rejectReason = (String) body.get("rejectReason");
        forumService.auditPost(id, status, rejectReason);
        return Result.ok();
    }
}
