package com.fire.recommendation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 邮箱找回密码用 Token 表
 */
@Data
@TableName("sys_password_reset_token")
public class SysPasswordResetToken {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String token;
    private LocalDateTime expireTime;
    /** 0 未使用 1 已使用 */
    private Integer used;
    private LocalDateTime createTime;
}
