package com.fire.recommendation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fire.recommendation.entity.News;
import com.fire.recommendation.mapper.NewsMapper;
import com.fire.recommendation.service.NewsRssService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NewsRssServiceImpl implements NewsRssService {

    private static final DateTimeFormatter RFC1123 = DateTimeFormatter.RFC_1123_DATE_TIME;

    private final NewsMapper newsMapper;

    @Value("${app.client-base-url:http://localhost:5177}")
    private String clientBaseUrl;

    @Override
    public String buildRssXml(int maxItems) {
        int limit = Math.min(Math.max(maxItems, 1), 50);
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<News> q = new LambdaQueryWrapper<News>()
                .eq(News::getDeleted, 0)
                .eq(News::getStatus, 1)
                .and(w -> w.isNull(News::getPublishTime).or().le(News::getPublishTime, now))
                .orderByDesc(News::getPublishTime)
                .orderByDesc(News::getId)
                .last("LIMIT " + limit);
        List<News> list = newsMapper.selectList(q);
        String base = clientBaseUrl.endsWith("/") ? clientBaseUrl.substring(0, clientBaseUrl.length() - 1) : clientBaseUrl;
        String link = base + "/news";
        StringBuilder sb = new StringBuilder(4096);
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<rss version=\"2.0\"><channel>\n");
        sb.append("<title>").append(escapeXml("消防新闻")).append("</title>\n");
        sb.append("<link>").append(escapeXml(link)).append("</link>\n");
        sb.append("<description>").append(escapeXml("消防科普推荐系统 - 新闻订阅")).append("</description>\n");
        sb.append("<language>zh-cn</language>\n");
        for (News n : list) {
            String itemLink = base + "/news/" + n.getId();
            sb.append("<item>\n");
            sb.append("<title>").append(escapeXml(n.getTitle())).append("</title>\n");
            sb.append("<link>").append(escapeXml(itemLink)).append("</link>\n");
            sb.append("<guid>").append(escapeXml(itemLink)).append("</guid>\n");
            if (n.getPublishTime() != null) {
                sb.append("<pubDate>")
                        .append(escapeXml(n.getPublishTime().atZone(ZoneId.systemDefault()).format(RFC1123)))
                        .append("</pubDate>\n");
            }
            String desc = StringUtils.hasText(n.getSummary()) ? n.getSummary() : "";
            sb.append("<description>").append(escapeXml(desc)).append("</description>\n");
            sb.append("</item>\n");
        }
        sb.append("</channel></rss>");
        return sb.toString();
    }

    private static String escapeXml(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
