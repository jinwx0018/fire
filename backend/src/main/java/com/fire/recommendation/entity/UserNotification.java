package com.fire.recommendation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_notification")
public class UserNotification {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String title;
    private String content;
    /** 点击后跳转地址（前端路由），如 /forum/12?commentId=34 */
    private String actionUrl;
    /** 跳转按钮文案，如 去查看 */
    private String actionText;
    /** 0 未读 1 已读 */
    private Integer isRead;
    private LocalDateTime createTime;
}
