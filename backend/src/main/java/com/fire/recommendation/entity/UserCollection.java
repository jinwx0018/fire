package com.fire.recommendation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_collection")
public class UserCollection {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    /** {@link com.fire.recommendation.constant.CollectionTargetType} */
    private Integer targetType;
    private Long contentId;
    private LocalDateTime createTime;
}
