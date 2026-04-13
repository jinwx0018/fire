package com.fire.recommendation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fire.recommendation.dto.LoginReq;
import com.fire.recommendation.dto.LoginVO;
import com.fire.recommendation.dto.RegisterReq;
import com.fire.recommendation.dto.UserInfoVO;
import com.fire.recommendation.entity.SysPasswordResetToken;
import com.fire.recommendation.entity.SysRole;
import com.fire.recommendation.entity.SysUser;
import com.fire.recommendation.entity.UserSession;
import com.fire.recommendation.exception.BusinessException;
import com.fire.recommendation.common.ResultCode;
import com.fire.recommendation.mapper.SysPasswordResetTokenMapper;
import com.fire.recommendation.mapper.SysRoleMapper;
import com.fire.recommendation.mapper.SysUserMapper;
import com.fire.recommendation.mapper.UserSessionMapper;
import com.fire.recommendation.service.AuditLogService;
import com.fire.recommendation.service.UserService;
import com.fire.recommendation.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.BeanUtils;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final int RESET_TOKEN_EXPIRE_MINUTES = 30;
    private static final int RESET_EMAIL_RETRY_SECONDS = 60;
    private static final int RESET_EMAIL_MAX_PER_HOUR = 5;

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysPasswordResetTokenMapper passwordResetTokenMapper;
    private final UserSessionMapper userSessionMapper;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String mailFrom;

    @Value("${app.client-base-url:http://localhost:5173}")
    private String clientBaseUrl;

    @Value("${jwt.refresh-expiration-ms:2592000000}")
    private long refreshExpirationMs;

    @Override
    public Long register(RegisterReq req) {
        if (userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, req.getUsername())) != null) {
            throw new BusinessException("用户名已存在");
        }
        if (userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getPhone, req.getPhone())) != null) {
            throw new BusinessException("手机号已注册");
        }
        SysUser user = new SysUser();
        user.setUsername(req.getUsername());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setPhone(req.getPhone());
        user.setEmail(req.getEmail());
        SysRole userRole = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, "USER"));
        if (userRole == null) {
            throw new BusinessException("系统未配置普通用户角色(USER)，请执行 DB 脚本初始化 sys_role");
        }
        user.setRoleId(userRole.getId());
        user.setStatus(1);
        user.setTokenVersion(0);
        user.setDeleted(0);
        userMapper.insert(user);
        auditLogService.log(user.getId(), "USER_REGISTER", "SYS_USER", user.getId(), "用户注册成功");
        return user.getId();
    }

    @Override
    public LoginVO login(LoginReq req) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, req.getUsername()));
        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        if (Integer.valueOf(1).equals(user.getDeleted())) {
            throw new BusinessException("账号已注销");
        }
        if (user.getStatus() != 1) {
            throw new BusinessException("账号已被禁用");
        }
        SysRole role = roleMapper.selectById(user.getRoleId());
        String roleCode = role != null ? role.getRoleCode() : "USER";
        Integer tokenVersion = user.getTokenVersion() == null ? 0 : user.getTokenVersion();
        String token = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), roleCode, tokenVersion);
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), tokenVersion, refreshExpirationMs);
        saveRefreshSession(user.getId(), tokenVersion, refreshToken);
        LoginVO vo = LoginVO.builder()
                .token(token)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .username(user.getUsername())
                .role(roleCode)
                .avatar(user.getAvatar())
                .build();
        auditLogService.log(user.getId(), "USER_LOGIN", "SYS_USER", user.getId(), "用户登录");
        log.info("登录成功: username={}, userId={}, role={}", user.getUsername(), user.getId(), roleCode);
        return vo;
    }

    @Override
    public LoginVO loginForAdmin(LoginReq req) {
        LoginVO vo = login(req);
        if (!"ADMIN".equals(vo.getRole())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅管理员可登录管理端");
        }
        return vo;
    }

    @Override
    public LoginVO refreshToken(String refreshToken) {
        if (!StringUtils.hasText(refreshToken) || !jwtUtil.validate(refreshToken)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "refreshToken 无效");
        }
        if (!"refresh".equals(jwtUtil.getTokenType(refreshToken))) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "token 类型错误");
        }
        Long userId = jwtUtil.getUserId(refreshToken);
        Integer tokenVersion = jwtUtil.getTokenVersion(refreshToken);
        UserSession session = userSessionMapper.selectOne(
                new LambdaQueryWrapper<UserSession>()
                        .eq(UserSession::getRefreshToken, refreshToken)
                        .eq(UserSession::getRevoked, 0)
                        .gt(UserSession::getExpireTime, LocalDateTime.now())
                        .last("LIMIT 1"));
        if (session == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "refreshToken 已失效");
        }
        SysUser user = userMapper.selectById(userId);
        if (user == null || Integer.valueOf(1).equals(user.getDeleted()) || user.getStatus() != 1) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户状态异常");
        }
        Integer currentVersion = user.getTokenVersion() == null ? 0 : user.getTokenVersion();
        if (!tokenVersion.equals(currentVersion)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "会话已失效，请重新登录");
        }
        SysRole role = roleMapper.selectById(user.getRoleId());
        String roleCode = role != null ? role.getRoleCode() : "USER";
        String newAccessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), roleCode, tokenVersion);
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getId(), tokenVersion, refreshExpirationMs);
        session.setRevoked(1);
        userSessionMapper.updateById(session);
        saveRefreshSession(user.getId(), tokenVersion, newRefreshToken);
        return LoginVO.builder()
                .token(newAccessToken)
                .refreshToken(newRefreshToken)
                .userId(user.getId())
                .username(user.getUsername())
                .role(roleCode)
                .avatar(user.getAvatar())
                .build();
    }

    @Override
    public void forceLogout(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null || Integer.valueOf(1).equals(user.getDeleted())) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        Integer currentVersion = user.getTokenVersion() == null ? 0 : user.getTokenVersion();
        user.setTokenVersion(currentVersion + 1);
        userMapper.updateById(user);
        userSessionMapper.update(
                null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<UserSession>()
                        .eq(UserSession::getUserId, userId)
                        .eq(UserSession::getRevoked, 0)
                        .set(UserSession::getRevoked, 1));
        auditLogService.log(userId, "FORCE_LOGOUT_SELF", "SYS_USER", userId, "用户主动退出所有设备");
    }

    @Override
    public UserInfoVO getInfo(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null || Integer.valueOf(1).equals(user.getDeleted())) throw new BusinessException(ResultCode.NOT_FOUND);
        UserInfoVO vo = new UserInfoVO();
        BeanUtils.copyProperties(user, vo);
        vo.setUserId(user.getId());
        SysRole role = roleMapper.selectById(user.getRoleId());
        vo.setRole(role != null ? role.getRoleCode() : "USER");
        return vo;
    }

    @Override
    public void updateInfo(Long userId, String avatar, String phone, String email) {
        SysUser user = userMapper.selectById(userId);
        if (user == null || Integer.valueOf(1).equals(user.getDeleted())) throw new BusinessException(ResultCode.NOT_FOUND);
        if (StringUtils.hasText(avatar)) user.setAvatar(avatar);
        if (StringUtils.hasText(phone)) {
            SysUser exist = userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getPhone, phone).ne(SysUser::getId, userId));
            if (exist != null) throw new BusinessException("手机号已被使用");
            user.setPhone(phone);
        }
        if (StringUtils.hasText(email)) {
            SysUser exist = userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getEmail, email).ne(SysUser::getId, userId));
            if (exist != null) throw new BusinessException("邮箱已被使用");
            user.setEmail(email);
        }
        userMapper.updateById(user);
    }

    @Override
    public void updatePassword(Long userId, String oldPassword, String newPassword) {
        SysUser user = userMapper.selectById(userId);
        if (user == null || Integer.valueOf(1).equals(user.getDeleted()) || !passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("原密码错误");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        Integer currentVersion = user.getTokenVersion() == null ? 0 : user.getTokenVersion();
        user.setTokenVersion(currentVersion + 1);
        userMapper.updateById(user);
        userSessionMapper.update(
                null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<UserSession>()
                        .eq(UserSession::getUserId, userId)
                        .eq(UserSession::getRevoked, 0)
                        .set(UserSession::getRevoked, 1));
        auditLogService.log(userId, "CHANGE_PASSWORD", "SYS_USER", userId, "用户修改密码");
    }

    @Override
    public void sendResetEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new BusinessException("邮箱不能为空");
        }
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getEmail, email));
        if (user == null) {
            throw new BusinessException("该邮箱未注册");
        }
        LocalDateTime now = LocalDateTime.now();
        SysPasswordResetToken latest = passwordResetTokenMapper.selectOne(
                new LambdaQueryWrapper<SysPasswordResetToken>()
                        .eq(SysPasswordResetToken::getUserId, user.getId())
                        .orderByDesc(SysPasswordResetToken::getCreateTime)
                        .last("LIMIT 1"));
        if (latest != null && latest.getCreateTime() != null
                && latest.getCreateTime().plusSeconds(RESET_EMAIL_RETRY_SECONDS).isAfter(now)) {
            throw new BusinessException("发送过于频繁，请稍后再试");
        }
        Long countInHour = passwordResetTokenMapper.selectCount(
                new LambdaQueryWrapper<SysPasswordResetToken>()
                        .eq(SysPasswordResetToken::getUserId, user.getId())
                        .ge(SysPasswordResetToken::getCreateTime, now.minusHours(1)));
        if (countInHour != null && countInHour >= RESET_EMAIL_MAX_PER_HOUR) {
            throw new BusinessException("1小时内发送次数已达上限，请稍后再试");
        }
        passwordResetTokenMapper.update(
                null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<SysPasswordResetToken>()
                        .eq(SysPasswordResetToken::getUserId, user.getId())
                        .eq(SysPasswordResetToken::getUsed, 0)
                        .set(SysPasswordResetToken::getUsed, 1));
        String token = UUID.randomUUID().toString().replace("-", "");
        SysPasswordResetToken record = new SysPasswordResetToken();
        record.setUserId(user.getId());
        record.setToken(token);
        record.setExpireTime(now.plusMinutes(RESET_TOKEN_EXPIRE_MINUTES));
        record.setUsed(0);
        passwordResetTokenMapper.insert(record);
        String resetUrl = clientBaseUrl + "/#/reset-password?token=" + token;
        if (mailSender != null) {
            try {
                SimpleMailMessage msg = new SimpleMailMessage();
                if (StringUtils.hasText(mailFrom)) {
                    msg.setFrom(mailFrom);
                }
                msg.setTo(email);
                msg.setSubject("消防科普推荐系统 - 重置密码");
                msg.setText("请点击以下链接重置密码（" + RESET_TOKEN_EXPIRE_MINUTES + "分钟内有效）：\n" + resetUrl);
                mailSender.send(msg);
                log.info("重置密码邮件已发送至: {}", email);
            } catch (Exception e) {
                log.warn("发送重置邮件失败: {}", e.getMessage());
                throw new BusinessException("发送邮件失败，请稍后重试");
            }
        } else {
            log.info("未配置邮件服务，重置链接: {}", resetUrl);
        }
        auditLogService.log(user.getId(), "SEND_RESET_EMAIL", "SYS_USER", user.getId(), "发送找回密码邮件");
    }

    @Override
    public void resetPasswordByToken(String token, String newPassword) {
        if (!StringUtils.hasText(token) || !StringUtils.hasText(newPassword)) {
            throw new BusinessException("Token 和新密码不能为空");
        }
        SysPasswordResetToken record = passwordResetTokenMapper.selectOne(
                new LambdaQueryWrapper<SysPasswordResetToken>()
                        .eq(SysPasswordResetToken::getToken, token)
                        .eq(SysPasswordResetToken::getUsed, 0)
                        .gt(SysPasswordResetToken::getExpireTime, LocalDateTime.now()));
        if (record == null) {
            throw new BusinessException("链接无效或已过期");
        }
        SysUser user = userMapper.selectById(record.getUserId());
        if (user == null || Integer.valueOf(1).equals(user.getDeleted())) {
            throw new BusinessException("用户不存在");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        Integer currentVersion = user.getTokenVersion() == null ? 0 : user.getTokenVersion();
        user.setTokenVersion(currentVersion + 1);
        userMapper.updateById(user);
        record.setUsed(1);
        passwordResetTokenMapper.updateById(record);
        userSessionMapper.update(
                null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<UserSession>()
                        .eq(UserSession::getUserId, user.getId())
                        .eq(UserSession::getRevoked, 0)
                        .set(UserSession::getRevoked, 1));
        auditLogService.log(user.getId(), "RESET_PASSWORD_BY_EMAIL", "SYS_USER", user.getId(), "邮箱找回密码");
    }

    @Override
    public IPage<UserInfoVO> adminUserList(Integer pageNum, Integer pageSize, String username, String role) {
        Page<SysUser> page = new Page<>(pageNum != null ? pageNum : 1, pageSize != null ? pageSize : 10);
        LambdaQueryWrapper<SysUser> q = new LambdaQueryWrapper<>();
        q.eq(SysUser::getDeleted, 0);
        if (StringUtils.hasText(username)) q.like(SysUser::getUsername, username);
        if (StringUtils.hasText(role)) {
            SysRole roleEntity = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, role));
            if (roleEntity != null) q.eq(SysUser::getRoleId, roleEntity.getId());
        }
        IPage<SysUser> result = userMapper.selectPage(page, q);
        return result.convert(u -> {
            UserInfoVO vo = new UserInfoVO();
            BeanUtils.copyProperties(u, vo);
            vo.setUserId(u.getId());
            vo.setStatus(u.getStatus());
            SysRole r = roleMapper.selectById(u.getRoleId());
            vo.setRole(r != null ? r.getRoleCode() : "USER");
            return vo;
        });
    }

    @Override
    public UserInfoVO adminGetUser(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null || Integer.valueOf(1).equals(user.getDeleted())) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        UserInfoVO vo = new UserInfoVO();
        BeanUtils.copyProperties(user, vo);
        vo.setUserId(user.getId());
        vo.setStatus(user.getStatus());
        SysRole r = roleMapper.selectById(user.getRoleId());
        vo.setRole(r != null ? r.getRoleCode() : "USER");
        return vo;
    }

    @Override
    public Long adminCreateUser(java.util.Map<String, Object> body) {
        String username = body != null ? (String) body.get("username") : null;
        String password = body != null ? (String) body.get("password") : null;
        String phone = body != null ? (String) body.get("phone") : null;
        if (!StringUtils.hasText(username)) {
            throw new BusinessException("用户名不能为空");
        }
        if (!StringUtils.hasText(password)) {
            throw new BusinessException("密码不能为空");
        }
        if (!StringUtils.hasText(phone)) {
            throw new BusinessException("手机号不能为空");
        }
        if (userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username)) != null) {
            throw new BusinessException("用户名已存在");
        }
        if (userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getPhone, phone)) != null) {
            throw new BusinessException("手机号已注册");
        }
        SysRole defaultUserRole = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, "USER"));
        if (defaultUserRole == null) {
            throw new BusinessException("系统未配置普通用户角色(USER)，请执行 DB 脚本初始化 sys_role");
        }
        Long roleId = defaultUserRole.getId();
        if (body != null && body.get("role") != null) {
            String roleCode = body.get("role").toString();
            SysRole r = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, roleCode));
            if (r != null) roleId = r.getId();
        } else if (body != null && body.get("roleId") != null) {
            roleId = Long.valueOf(body.get("roleId").toString());
        }
        Integer status = 1;
        if (body != null && body.get("status") != null) {
            status = Integer.valueOf(body.get("status").toString());
        }
        SysUser user = new SysUser();
        user.setUsername(username.trim());
        user.setPassword(passwordEncoder.encode(password));
        user.setPhone(phone.trim());
        user.setEmail(body != null && body.get("email") != null ? body.get("email").toString().trim() : null);
        user.setAvatar(body != null && body.get("avatar") != null ? (String) body.get("avatar") : null);
        user.setRoleId(roleId);
        user.setStatus(status);
        user.setTokenVersion(0);
        user.setDeleted(0);
        userMapper.insert(user);
        auditLogService.log(null, "ADMIN_CREATE_USER", "SYS_USER", user.getId(), "管理端新增用户");
        return user.getId();
    }

    @Override
    public void adminUpdateUser(Long id, java.util.Map<String, Object> body, Long operatorId) {
        SysUser user = userMapper.selectById(id);
        if (user == null || Integer.valueOf(1).equals(user.getDeleted())) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (body == null) {
            return;
        }
        SysUser simulated = simulateUserAfterAdminUpdate(user, body);
        assertNotRemovingLastActiveAdmin(user, simulated);
        if (body.containsKey("username") && StringUtils.hasText((String) body.get("username"))) {
            String username = ((String) body.get("username")).trim();
            SysUser exist = userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username).ne(SysUser::getId, id));
            if (exist != null) throw new BusinessException("用户名已被使用");
            user.setUsername(username);
        }
        if (body.containsKey("phone") && body.get("phone") != null) {
            String phone = body.get("phone").toString().trim();
            if (StringUtils.hasText(phone)) {
                SysUser exist = userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getPhone, phone).ne(SysUser::getId, id));
                if (exist != null) throw new BusinessException("手机号已被使用");
                user.setPhone(phone);
            }
        }
        if (body.containsKey("email")) {
            user.setEmail(body.get("email") != null ? body.get("email").toString().trim() : null);
        }
        if (body.containsKey("avatar")) {
            user.setAvatar((String) body.get("avatar"));
        }
        if (body.containsKey("role")) {
            String roleCode = body.get("role").toString();
            SysRole r = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, roleCode));
            if (r != null) user.setRoleId(r.getId());
        }
        if (body.containsKey("roleId")) {
            user.setRoleId(Long.valueOf(body.get("roleId").toString()));
        }
        if (body.containsKey("status")) {
            user.setStatus(Integer.valueOf(body.get("status").toString()));
        }
        if (body.containsKey("password") && body.get("password") != null && StringUtils.hasText(body.get("password").toString())) {
            user.setPassword(passwordEncoder.encode(body.get("password").toString()));
            Integer currentVersion = user.getTokenVersion() == null ? 0 : user.getTokenVersion();
            user.setTokenVersion(currentVersion + 1);
        }
        if (body.containsKey("forceLogout") && Boolean.parseBoolean(body.get("forceLogout").toString())) {
            Integer currentVersion = user.getTokenVersion() == null ? 0 : user.getTokenVersion();
            user.setTokenVersion(currentVersion + 1);
        }
        userMapper.updateById(user);
        if ((body.containsKey("forceLogout") && Boolean.parseBoolean(body.get("forceLogout").toString()))
                || (body.containsKey("password") && body.get("password") != null && StringUtils.hasText(body.get("password").toString()))
                || (body.containsKey("status") && Integer.valueOf(body.get("status").toString()) == 0)) {
            userSessionMapper.update(
                    null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<UserSession>()
                            .eq(UserSession::getUserId, id)
                            .eq(UserSession::getRevoked, 0)
                            .set(UserSession::getRevoked, 1));
        }
        auditLogService.log(null, "ADMIN_UPDATE_USER", "SYS_USER", id, "管理端修改用户信息");
    }

    @Override
    public void adminDeleteUser(Long id, Long operatorId) {
        if (operatorId != null && operatorId.equals(id)) {
            throw new BusinessException("不能删除当前登录账号");
        }
        SysUser user = userMapper.selectById(id);
        if (user == null || Integer.valueOf(1).equals(user.getDeleted())) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (isActiveAdmin(user) && countActiveAdmins() <= 1) {
            throw new BusinessException("不能删除最后一个可登录的管理员");
        }
        user.setDeleted(1);
        user.setDeletedTime(LocalDateTime.now());
        user.setStatus(0);
        Integer currentVersion = user.getTokenVersion() == null ? 0 : user.getTokenVersion();
        user.setTokenVersion(currentVersion + 1);
        userMapper.updateById(user);
        userSessionMapper.update(
                null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<UserSession>()
                        .eq(UserSession::getUserId, id)
                        .eq(UserSession::getRevoked, 0)
                        .set(UserSession::getRevoked, 1));
        auditLogService.log(null, "ADMIN_SOFT_DELETE_USER", "SYS_USER", id, "管理端注销账号");
    }

    @Override
    public void adminForceLogoutUser(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null || Integer.valueOf(1).equals(user.getDeleted())) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        Integer currentVersion = user.getTokenVersion() == null ? 0 : user.getTokenVersion();
        user.setTokenVersion(currentVersion + 1);
        userMapper.updateById(user);
        userSessionMapper.update(
                null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<UserSession>()
                        .eq(UserSession::getUserId, id)
                        .eq(UserSession::getRevoked, 0)
                        .set(UserSession::getRevoked, 1));
        auditLogService.log(null, "ADMIN_FORCE_LOGOUT", "SYS_USER", id, "管理端强制下线用户");
    }

    private boolean isActiveAdmin(SysUser u) {
        if (u == null || Integer.valueOf(1).equals(u.getDeleted())) {
            return false;
        }
        if (u.getStatus() == null || u.getStatus() != 1) {
            return false;
        }
        SysRole r = roleMapper.selectById(u.getRoleId());
        return r != null && "ADMIN".equals(r.getRoleCode());
    }

    private long countActiveAdmins() {
        SysRole admin = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, "ADMIN"));
        if (admin == null) {
            return 0;
        }
        return userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getRoleId, admin.getId())
                .eq(SysUser::getDeleted, 0)
                .eq(SysUser::getStatus, 1));
    }

    /** 根据本次修改推断更新后的用户，用于校验是否动到「最后一个管理员」 */
    private SysUser simulateUserAfterAdminUpdate(SysUser user, Map<String, Object> body) {
        SysUser sim = new SysUser();
        BeanUtils.copyProperties(user, sim);
        if (body.containsKey("role")) {
            String roleCode = body.get("role").toString();
            SysRole r = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, roleCode));
            if (r != null) {
                sim.setRoleId(r.getId());
            }
        }
        if (body.containsKey("roleId")) {
            sim.setRoleId(Long.valueOf(body.get("roleId").toString()));
        }
        if (body.containsKey("status")) {
            sim.setStatus(Integer.valueOf(body.get("status").toString()));
        }
        return sim;
    }

    private void assertNotRemovingLastActiveAdmin(SysUser before, SysUser simulatedAfter) {
        boolean was = isActiveAdmin(before);
        boolean will = isActiveAdmin(simulatedAfter);
        if (was && !will && countActiveAdmins() <= 1) {
            throw new BusinessException("不可移除或停用最后一个可登录的管理员");
        }
    }

    private void saveRefreshSession(Long userId, Integer tokenVersion, String refreshToken) {
        UserSession session = new UserSession();
        session.setUserId(userId);
        session.setTokenVersion(tokenVersion);
        session.setRefreshToken(refreshToken);
        session.setRevoked(0);
        session.setExpireTime(LocalDateTime.now().plusSeconds(refreshExpirationMs / 1000));
        userSessionMapper.insert(session);
    }

    @Override
    public void selfDeleteAccount(Long userId, String password) {
        if (!StringUtils.hasText(password)) {
            throw new BusinessException("请输入登录密码以确认注销");
        }
        SysUser user = userMapper.selectById(userId);
        if (user == null || Integer.valueOf(1).equals(user.getDeleted())) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        SysRole role = roleMapper.selectById(user.getRoleId());
        if (role != null && "ADMIN".equals(role.getRoleCode())) {
            throw new BusinessException("管理员账号不可在用户端自助注销，请联系超级管理员处理");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException("密码错误");
        }
        user.setDeleted(1);
        user.setDeletedTime(LocalDateTime.now());
        user.setStatus(0);
        Integer currentVersion = user.getTokenVersion() == null ? 0 : user.getTokenVersion();
        user.setTokenVersion(currentVersion + 1);
        userMapper.updateById(user);
        userSessionMapper.update(
                null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<UserSession>()
                        .eq(UserSession::getUserId, userId)
                        .eq(UserSession::getRevoked, 0)
                        .set(UserSession::getRevoked, 1));
        auditLogService.log(userId, "USER_SELF_DELETE", "SYS_USER", userId, "用户自助注销账号");
    }
}
