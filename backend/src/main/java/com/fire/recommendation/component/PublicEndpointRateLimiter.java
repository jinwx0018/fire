package com.fire.recommendation.component;

import com.fire.recommendation.util.HttpClientIp;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Locale;

/**
 * 登录、注册、找回密码等匿名接口的限流（按 IP / 用户名维度）。
 */
@Component
public class PublicEndpointRateLimiter {

    private final SlidingWindowCounter perMinute = new SlidingWindowCounter(60_000L);
    private final SlidingWindowCounter perHour = new SlidingWindowCounter(3_600_000L);

    @Value("${app.rate-limit.login-per-ip-per-minute:30}")
    private int loginPerIpPerMinute;

    @Value("${app.rate-limit.login-per-username-per-minute:20}")
    private int loginPerUsernamePerMinute;

    @Value("${app.rate-limit.admin-login-per-ip-per-minute:30}")
    private int adminLoginPerIpPerMinute;

    @Value("${app.rate-limit.register-per-ip-per-hour:30}")
    private int registerPerIpPerHour;

    @Value("${app.rate-limit.reset-email-per-ip-per-hour:20}")
    private int resetEmailPerIpPerHour;

    public void beforeUserLogin(HttpServletRequest request, String username) {
        String ip = HttpClientIp.resolve(request);
        perMinute.incrementAndCheck("login:ip:" + ip, loginPerIpPerMinute);
        if (StringUtils.hasText(username)) {
            perMinute.incrementAndCheck("login:user:" + username.trim().toLowerCase(Locale.ROOT), loginPerUsernamePerMinute);
        }
    }

    public void beforeAdminLogin(HttpServletRequest request, String username) {
        String ip = HttpClientIp.resolve(request);
        perMinute.incrementAndCheck("admin-login:ip:" + ip, adminLoginPerIpPerMinute);
        if (StringUtils.hasText(username)) {
            perMinute.incrementAndCheck("admin-login:user:" + username.trim().toLowerCase(Locale.ROOT), loginPerUsernamePerMinute);
        }
    }

    public void beforeRegister(HttpServletRequest request) {
        perHour.incrementAndCheck("register:ip:" + HttpClientIp.resolve(request), registerPerIpPerHour);
    }

    public void beforePasswordResetEmail(HttpServletRequest request) {
        perHour.incrementAndCheck("reset-email:ip:" + HttpClientIp.resolve(request), resetEmailPerIpPerHour);
    }
}
