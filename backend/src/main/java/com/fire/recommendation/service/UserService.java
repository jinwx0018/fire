package com.fire.recommendation.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fire.recommendation.dto.LoginVO;
import com.fire.recommendation.dto.RegisterReq;
import com.fire.recommendation.dto.UserInfoVO;
import com.fire.recommendation.dto.LoginReq;

public interface UserService {

    Long register(RegisterReq req);

    LoginVO login(LoginReq req);

    UserInfoVO getInfo(Long userId);

    void updateInfo(Long userId, String avatar, String phone, String email);

    void updatePassword(Long userId, String oldPassword, String newPassword);

    void sendResetEmail(String email);

    void resetPasswordByToken(String token, String newPassword);

    IPage<UserInfoVO> adminUserList(Integer pageNum, Integer pageSize, String username, String role);

    /** 管理端：根据 ID 查询用户详情 */
    UserInfoVO adminGetUser(Long id);

    /** 管理端：创建用户（可指定角色、状态） */
    Long adminCreateUser(java.util.Map<String, Object> body);

    /** 管理端：更新用户（角色、状态、手机、邮箱等） */
    void adminUpdateUser(Long id, java.util.Map<String, Object> body);

    /** 管理端：删除用户（物理删除） */
    void adminDeleteUser(Long id);
}
