package com.fire.recommendation.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserInfoVO {
    private Long userId;
    private String username;
    private String phone;
    private String email;
    private String avatar;
    private String role;
    /** 状态：0 禁用 1 正常 */
    private Integer status;
    private LocalDateTime createTime;
}
