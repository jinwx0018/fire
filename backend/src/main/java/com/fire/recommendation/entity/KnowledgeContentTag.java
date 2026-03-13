package com.fire.recommendation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("knowledge_content_tag")
public class KnowledgeContentTag {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long contentId;
    private Long tagId;
    private LocalDateTime createTime;
}
