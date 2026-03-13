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
import com.fire.recommendation.exception.BusinessException;
import com.fire.recommendation.common.ResultCode;
import com.fire.recommendation.mapper.SysPasswordResetTokenMapper;
import com.fire.recommendation.mapper.SysRoleMapper;
import com.fire.recommendation.mapper.SysUserMapper;
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
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final int RESET_TOKEN_EXPIRE_MINUTES = 30;

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysPasswordResetTokenMapper passwordResetTokenMapper;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${app.client-base-url:http://localhost:5173}")
    private String clientBaseUrl;

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
        user.setRoleId(1L);
        user.setStatus(1);
        userMapper.insert(user);
        return user.getId();
    }

    @Override
    public LoginVO login(LoginReq req) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, req.getUsername()));
        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        if (user.getStatus() != 1) {
            throw new BusinessException("账号已被禁用");
        }
        SysRole role = roleMapper.selectById(user.getRoleId());
        String roleCode = role != null ? role.getRoleCode() : "USER";
        String token = jwtUtil.generate(user.getId(), user.getUsername(), roleCode);
        LoginVO vo = LoginVO.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .role(roleCode)
                .avatar(user.getAvatar())
                .build();
        log.info("登录成功: username={}, userId={}, role={}", user.getUsername(), user.getId(), roleCode);
        return vo;
    }

    @Override
    public UserInfoVO getInfo(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(ResultCode.NOT_FOUND);
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
        if (user == null) throw new BusinessException(ResultCode.NOT_FOUND);
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
        if (user == null || !passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("原密码错误");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
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
        String token = UUID.randomUUID().toString().replace("-", "");
        SysPasswordResetToken record = new SysPasswordResetToken();
        record.setUserId(user.getId());
        record.setToken(token);
        record.setExpireTime(LocalDateTime.now().plusMinutes(RESET_TOKEN_EXPIRE_MINUTES));
        record.setUsed(0);
        passwordResetTokenMapper.insert(record);
        String resetUrl = clientBaseUrl + "/#/reset-password?token=" + token;
        if (mailSender != null) {
            try {
                SimpleMailMessage msg = new SimpleMailMessage();
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
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
        record.setUsed(1);
        passwordResetTokenMapper.updateById(record);
    }

    @Override
    public IPage<UserInfoVO> adminUserList(Integer pageNum, Integer pageSize, String username, String role) {
        Page<SysUser> page = new Page<>(pageNum != null ? pageNum : 1, pageSize != null ? pageSize : 10);
        LambdaQueryWrapper<SysUser> q = new LambdaQueryWrapper<>();
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
        if (user == null) {
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
        Long roleId = 1L;
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
        userMapper.insert(user);
        return user.getId();
    }

    @Override
    public void adminUpdateUser(Long id, java.util.Map<String, Object> body) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (body == null) return;
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
        }
        userMapper.updateById(user);
    }

    @Override
    public void adminDeleteUser(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        userMapper.deleteById(id);
    }
}
