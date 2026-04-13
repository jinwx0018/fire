package com.fire.recommendation.controller.admin;

import com.fire.recommendation.common.Result;
import com.fire.recommendation.common.ResultCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 管理端系统监控与日志查看（需管理员 JWT）。
 */
@Tag(name = "管理端-系统监控", description = "运行状态、JVM 摘要、日志文件尾部")
@RestController
@RequestMapping("/admin/monitor")
@RequiredArgsConstructor
public class AdminMonitorController {

    private final DataSource dataSource;

    @Value("${logging.file.name:}")
    private String loggingFileName;

    @Operation(summary = "运行概览", description = "数据库连通、JVM 堆内存、线程数、运行时长等")
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("database", checkDatabase());
        m.put("uptimeMs", ManagementFactory.getRuntimeMXBean().getUptime());
        MemoryMXBean mem = ManagementFactory.getMemoryMXBean();
        m.put("heapUsedBytes", mem.getHeapMemoryUsage().getUsed());
        m.put("heapMaxBytes", mem.getHeapMemoryUsage().getMax());
        m.put("threadCount", ManagementFactory.getThreadMXBean().getThreadCount());
        m.put("processors", Runtime.getRuntime().availableProcessors());
        m.put("logFileConfigured", StringUtils.hasText(loggingFileName));
        m.put("logFilePath", StringUtils.hasText(loggingFileName) ? loggingFileName : null);
        return Result.ok(m);
    }

    @Operation(summary = "日志尾部", description = "需配置 logging.file.name；最多 500 行")
    @GetMapping("/logs/tail")
    public Result<String> logTail(@RequestParam(defaultValue = "200") int lines) {
        if (!StringUtils.hasText(loggingFileName)) {
            return Result.fail(ResultCode.BAD_REQUEST, "未配置 logging.file.name，请在 application.yml 中设置日志文件路径");
        }
        int n = Math.max(1, Math.min(lines, 500));
        Path path = Path.of(loggingFileName.trim()).toAbsolutePath().normalize();
        if (!Files.isRegularFile(path)) {
            return Result.fail(ResultCode.BAD_REQUEST, "日志文件不存在或尚未生成: " + path);
        }
        try {
            String text = readLastLines(path, n);
            return Result.ok(text);
        } catch (IOException e) {
            return Result.fail(ResultCode.SERVER_ERROR, "读取日志失败: " + e.getMessage());
        }
    }

    private String checkDatabase() {
        try (Connection c = dataSource.getConnection()) {
            return c.isValid(3) ? "UP" : "UNKNOWN";
        } catch (Exception e) {
            return "DOWN: " + e.getMessage();
        }
    }

    /** 逐行读取文件尾部，控制内存占用 */
    private static String readLastLines(Path path, int maxLines) throws IOException {
        Deque<String> buf = new ArrayDeque<>(maxLines + 1);
        try (var br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = br.readLine()) != null) {
                if (buf.size() >= maxLines) {
                    buf.pollFirst();
                }
                buf.addLast(line);
            }
        }
        return String.join("\n", buf);
    }
}
