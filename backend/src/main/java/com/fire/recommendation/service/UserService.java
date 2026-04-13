package com.fire.recommendation.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fire.recommendation.dto.LoginVO;
import com.fire.recommendation.dto.RegisterReq;
import com.fire.recommendation.dto.UserInfoVO;
import com.fire.recommendation.dto.LoginReq;

public interface UserService {

    Long register(RegisterReq req);

    LoginVO login(LoginReq req);

    /** 管理端登录：校验用户名密码后，仅当角色为 ADMIN 时返回 Token，否则抛出无权限 */
    LoginVO loginForAdmin(LoginReq req);
    LoginVO refreshToken(String refreshToken);
    void forceLogout(Long userId);

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

    /** 管理端：更新用户（角色、状态、手机、邮箱等）；operatorId 当前管理员 ID，用于保护最后一个管理员 */
    void adminUpdateUser(Long id, java.util.Map<String, Object> body, Long operatorId);

    /** 管理端：删除用户（软删除）；operatorId 当前管理员 ID */
    void adminDeleteUser(Long id, Long operatorId);

    /** 管理端：强制用户下线 */
    void adminForceLogoutUser(Long id);

    /** 用户自助注销（软删除），需校验密码；管理员账号不允许自助注销 */
    void selfDeleteAccount(Long userId, String password);
}
