package com.fire.recommendation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_behavior")
public class UserBehavior {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String behaviorType;
    private String targetType;
    private Long targetId;
    private String extra;
    private LocalDateTime createTime;
}
