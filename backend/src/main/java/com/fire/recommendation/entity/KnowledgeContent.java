package com.fire.recommendation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("knowledge_content")
public class KnowledgeContent {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private Long categoryId;
    private String content;
    private String cover;
    private String summary;
    private Long authorId;
    private Integer viewCount;
    private Integer likeCount;
    private Integer status;
    /** 审核驳回原因（仅作者可见），对应表字段 reject_reason */
    @TableField("reject_reason")
    private String rejectReason;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
