package com.fire.recommendation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("knowledge_tag")
public class KnowledgeTag {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private LocalDateTime createTime;
}
