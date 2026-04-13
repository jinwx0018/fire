package com.fire.recommendation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_comment_like")
public class UserCommentLike {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** FORUM_COMMENT / NEWS_COMMENT / KNOWLEDGE_COMMENT */
    private String channel;
    private Long commentId;
    private Long userId;
    private LocalDateTime createTime;
}
