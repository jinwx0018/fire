package com.fire.recommendation.component;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 在 OWASP 消毒之后，按配置的主机白名单过滤正文中的 img[src]、a[href]。
 * {@code app.news.allowed-resource-hosts} 为空时不做额外过滤（保持原有行为）。
 */
@Slf4j
@Component
public class NewsMediaWhitelistFilter {

    @Value("${app.news.allowed-resource-hosts:}")
    private String allowedHostsRaw;

    private List<String> allowedHosts = List.of();

    @PostConstruct
    void init() {
        if (!StringUtils.hasText(allowedHostsRaw)) {
            allowedHosts = List.of();
            log.info("app.news.allowed-resource-hosts 未配置，新闻正文外链不做主机白名单过滤");
            return;
        }
        List<String> list = new ArrayList<>();
        for (String p : allowedHostsRaw.split(",")) {
            String s = p.trim().toLowerCase(Locale.ROOT);
            if (StringUtils.hasText(s)) {
                list.add(s);
            }
        }
        allowedHosts = List.copyOf(list);
        log.info("新闻正文资源白名单主机: {}", allowedHosts);
    }

    public String filter(String sanitizedHtml) {
        if (allowedHosts.isEmpty() || !StringUtils.hasText(sanitizedHtml)) {
            return sanitizedHtml;
        }
        Document doc = Jsoup.parseBodyFragment(sanitizedHtml);
        doc.outputSettings().prettyPrint(false);
        for (Element img : doc.select("img[src]")) {
            if (!allowedUrl(img.attr("src"), true)) {
                img.remove();
            }
        }
        for (Element a : doc.select("a[href]")) {
            String href = a.attr("href");
            if (!allowedUrl(href, false)) {
                a.removeAttr("href");
                if (!a.hasText() && a.children().isEmpty()) {
                    a.remove();
                }
            }
        }
        Element body = doc.body();
        return body != null ? body.html() : "";
    }

    private boolean allowedUrl(String url, boolean forImage) {
        if (!StringUtils.hasText(url)) {
            return false;
        }
        String u = url.trim();
        String lower = u.toLowerCase(Locale.ROOT);
        if (lower.startsWith("javascript:") || lower.startsWith("data:")) {
            return false;
        }
        if (u.startsWith("/") && !u.startsWith("//")) {
            return !u.contains("..");
        }
        if (!forImage && lower.startsWith("mailto:")) {
            return true;
        }
        try {
            URI uri = URI.create(u);
            String scheme = uri.getScheme();
            if (scheme == null || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
                return false;
            }
            String host = uri.getHost();
            if (!StringUtils.hasText(host)) {
                return false;
            }
            return hostMatchesWhitelist(host.toLowerCase(Locale.ROOT));
        } catch (Exception e) {
            return false;
        }
    }

    private boolean hostMatchesWhitelist(String host) {
        for (String rule : allowedHosts) {
            if (host.equals(rule)) {
                return true;
            }
            if (host.endsWith("." + rule)) {
                return true;
            }
        }
        return false;
    }
}
