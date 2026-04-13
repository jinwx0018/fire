package com.fire.recommendation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("news_comment")
public class NewsComment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long newsId;
    private Long userId;
    private Long parentId;
    private String content;
    private Integer likeCount;
    /** 1 显示 0 隐藏 */
    private Integer status;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createTime;
}
