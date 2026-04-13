package com.fire.recommendation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class SysUser {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String password;
    private String phone;
    private String email;
    private String avatar;
    private Long roleId;
    private Integer status;
    /** JWT 版本号，用于强制下线/改密后令旧 token 失效 */
    private Integer tokenVersion;
    /** 逻辑删除：0 未删除 1 已删除 */
    private Integer deleted;
    private LocalDateTime deletedTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
