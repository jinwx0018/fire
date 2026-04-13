package com.fire.recommendation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("author_application")
public class AuthorApplication {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String applyReason;
    /** 附件 URL 列表，逗号分隔 */
    private String attachments;
    private String status;  // PENDING, APPROVED, REJECTED
    private String reviewRemark;
    private String rejectReason;
    private Long reviewBy;
    private LocalDateTime reviewTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
