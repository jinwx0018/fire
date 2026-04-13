package com.fire.recommendation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("news")
public class News {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String content;
    private String region;
    /** 分类字典 id；可与自由文本 category 并存（迁移期） */
    private Long categoryId;
    /**
     * 旧版自由分类文本（可选）。
     * 你的数据库当前可能未包含该列，为避免 MyBatis-Plus 自动 SELECT 不存在列，这里设为 exist=false。
     * 若你已在表中添加 category 列，可将 exist=false 去掉以恢复旧字段兼容。
     */
    @TableField(exist = false)
    private String category;
    private Integer urgencyLevel;
    private Long publisherId;
    private String summary;
    /**
     * 封面图 URL。若库中尚未执行 {@code ALTER TABLE news ADD COLUMN cover_url ...}（见 DB/12a_news_cover_url_only.sql），
     * 为避免自动 SELECT 不存在的列，此处 {@code exist=false}；补列后请去掉 {@code exist=false} 以持久化封面。
     */
    @TableField(exist = false)
    private String coverUrl;
    private Integer viewCount;
    /** 点赞数（冗余字段） */
    private Integer likeCount;
    private Integer status;
    @TableLogic
    private Integer deleted;
    private LocalDateTime publishTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
