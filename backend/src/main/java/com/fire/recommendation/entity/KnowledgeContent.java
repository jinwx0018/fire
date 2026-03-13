package com.fire.recommendation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
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
    @TableLogic
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
