package com.fire.recommendation.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 生成与项目一致的 BCrypt 密码哈希，用于在数据库中直接插入用户（如管理员）。
 * 运行方式：在 IDE 中运行 main，或：mvn -q exec:java -Dexec.mainClass="com.fire.recommendation.util.BcryptHashUtil"
 */
public class BcryptHashUtil {

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String password = args.length > 0 ? args[0] : "123456";
        String hash = encoder.encode(password);
        System.out.println("Password: " + password);
        System.out.println("BCrypt hash (use in SQL): " + hash);
    }
}
