package com.fire.recommendation.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fire.recommendation.dto.AiRecommendContext;
import com.fire.recommendation.service.AiRecommendService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 智能推荐 - AI 模型增强实现。
 * AI 参与选品+排序：reorderByAi 对候选打分/重排；enrichRecommendReasons 补充推荐理由。
 * 支持两种模式：
 * 1) 通用模式：POST { userId, candidates, keywords?, context? }，响应 { order } 或 { scores } 用于重排，{ reasons } 用于理由；
 * 2) 大模型模式（provider=doubao）：用 prompt 让模型输出推荐顺序（id 列表）实现重排，或生成推荐理由；Prompt 可含检索词/偏好分类/热点词。
 * 未配置或失败时返回/保持原列表。
 */
@Slf4j
@Service
public class AiRecommendServiceImpl implements AiRecommendService {

    private final RestTemplate restTemplate;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public AiRecommendServiceImpl(@Qualifier("aiRecommendRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Value("${ai.recommend.enabled:false}")
    private boolean enabled;

    @Value("${ai.recommend.api-url:}")
    private String apiUrl;

    @Value("${ai.recommend.api-key:}")
    private String apiKey;

    @Value("${ai.recommend.provider:}")
    private String provider;

    @Value("${ai.recommend.model:}")
    private String model;

    @Override
    public boolean isAiRecommendEnabled() {
        return enabled;
    }

    @Override
    public boolean isAiLlmReady() {
        if (!enabled) {
            return false;
        }
        if (isLlmChatMode()) {
            return StringUtils.hasText(apiUrl) && StringUtils.hasText(apiKey) && StringUtils.hasText(model);
        }
        return StringUtils.hasText(apiUrl);
    }

    @Override
    public List<Map<String, Object>> reorderByAi(Long userId, List<Map<String, Object>> candidates, AiRecommendContext ctx) {
        if (ctx == null) {
            ctx = AiRecommendContext.empty();
        }
        if (!enabled || candidates == null || candidates.isEmpty()) {
            return candidates == null ? new ArrayList<>() : new ArrayList<>(candidates);
        }
        if (isLlmChatMode() && StringUtils.hasText(apiUrl) && StringUtils.hasText(apiKey) && StringUtils.hasText(model)) {
            return reorderByLlmChat(userId, candidates, ctx);
        }
        if (!isLlmChatMode() && StringUtils.hasText(apiUrl)) {
            return reorderByGenericApi(userId, candidates, ctx);
        }
        return new ArrayList<>(candidates);
    }

    private void appendUserContextToPrompt(StringBuilder prompt, AiRecommendContext ctx) {
        if (ctx == null) {
            return;
        }
        List<String> hot = ctx.getHotTopicKeywords();
        boolean any = false;
        if (StringUtils.hasText(ctx.getKeyword())) {
            prompt.append("用户当前检索词：").append(ctx.getKeyword().trim()).append("\n");
            any = true;
        }
        if (StringUtils.hasText(ctx.getTopCategoryName())) {
            prompt.append("用户偏好知识分类（根据历史行为统计）：").append(ctx.getTopCategoryName()).append("\n");
            any = true;
        }
        if (hot != null && !hot.isEmpty()) {
            prompt.append("平台热点消防主题参考：").append(String.join("、", hot)).append("\n");
            any = true;
        }
        if (any) {
            prompt.append("\n");
        }
    }

    private void mergeAiContextIntoRequest(Map<String, Object> requestBody, AiRecommendContext ctx) {
        if (ctx == null) {
            return;
        }
        if (StringUtils.hasText(ctx.getKeyword())) {
            requestBody.put("keywords", ctx.getKeyword().trim());
        }
        Map<String, Object> context = new LinkedHashMap<>();
        if (StringUtils.hasText(ctx.getTopCategoryName())) {
            context.put("topCategoryName", ctx.getTopCategoryName());
        }
        if (StringUtils.hasText(ctx.getKeyword())) {
            context.put("keyword", ctx.getKeyword().trim());
        }
        List<String> hot = ctx.getHotTopicKeywords();
        if (hot != null && !hot.isEmpty()) {
            context.put("hotTopics", new ArrayList<>(hot));
        }
        if (ctx.getExtra() != null && !ctx.getExtra().isEmpty()) {
            for (Map.Entry<String, Object> e : ctx.getExtra().entrySet()) {
                context.putIfAbsent(e.getKey(), e.getValue());
            }
        }
        if (!context.isEmpty()) {
            requestBody.put("context", context);
        }
    }

    /** 大模型模式：用 prompt 让模型输出推荐顺序（id 逗号分隔），解析后重排列表 */
    private List<Map<String, Object>> reorderByLlmChat(Long userId, List<Map<String, Object>> candidates, AiRecommendContext ctx) {
        try {
            StringBuilder prompt = new StringBuilder();
            prompt.append("你是一个消防科普推荐系统的排序模型。请根据“对用户的推荐优先级”对以下候选内容进行排序。\n");
            appendUserContextToPrompt(prompt, ctx);
            prompt.append("仅输出排序后的 id 列表，用英文逗号分隔，不要其他文字。例如：3,1,5,2,4\n\n");
            prompt.append("候选列表：\n");
            for (Map<String, Object> m : candidates) {
                Object id = m.get("id");
                String title = m.get("title") != null ? m.get("title").toString() : "";
                String summary = m.get("summary") != null ? m.get("summary").toString() : "";
                if (summary.length() > 60) {
                    summary = summary.substring(0, 60) + "…";
                }
                String cat = m.get("categoryName") != null ? m.get("categoryName").toString() : "";
                prompt.append("id=").append(id).append(" 分类：").append(cat).append(" 标题：").append(title).append(" 摘要：").append(summary).append("\n");
            }
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", model);
            ArrayNode messages = objectMapper.createArrayNode();
            ObjectNode userMsg = objectMapper.createObjectNode();
            userMsg.put("role", "user");
            userMsg.put("content", prompt.toString());
            messages.add(userMsg);
            body.set("messages", messages);
            body.put("temperature", 0.3);
            body.put("max_tokens", 2048);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            HttpEntity<String> entity = new HttpEntity<>(body.toString(), headers);
            ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.POST, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                logDoubaoErrorIfPresent(root);
                JsonNode choices = root.get("choices");
                if (choices != null && choices.isArray() && choices.size() > 0) {
                    String content = choices.get(0).path("message").path("content").asText("").trim();
                    List<Long> order = parseIdOrderFromLlm(content, candidates);
                    if (!order.isEmpty()) {
                        return reorderListByIds(candidates, order);
                    }
                }
            }
        } catch (HttpStatusCodeException e) {
            log.warn("AI 排序（豆包/方舟）HTTP {} 响应: {}", e.getStatusCode().value(), truncateForLog(e.getResponseBodyAsString()));
        } catch (Exception e) {
            log.warn("AI 排序（大模型）调用失败，使用原顺序: {}", e.getMessage());
        }
        return new ArrayList<>(candidates);
    }

    private List<Long> parseIdOrderFromLlm(String content, List<Map<String, Object>> candidates) {
        List<Long> order = new ArrayList<>();
        Set<Long> candidateIds = candidates.stream()
                .map(m -> toLong(m.get("id")))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        String s = content.replaceAll("[^0-9,]", "").trim();
        if (StringUtils.hasText(s)) {
            for (String part : s.split(",")) {
                part = part.trim();
                if (part.isEmpty()) {
                    continue;
                }
                try {
                    long id = Long.parseLong(part);
                    if (candidateIds.contains(id)) {
                        order.add(id);
                    }
                } catch (NumberFormatException ignored) {
                    // skip
                }
            }
        }
        return order;
    }

    private static Long toLong(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number) {
            return ((Number) o).longValue();
        }
        try {
            return Long.parseLong(o.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private List<Map<String, Object>> reorderListByIds(List<Map<String, Object>> items, List<Long> idOrder) {
        Map<Long, Map<String, Object>> byId = new HashMap<>();
        for (Map<String, Object> m : items) {
            Long id = toLong(m.get("id"));
            if (id != null) {
                byId.put(id, m);
            }
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Long id : idOrder) {
            if (byId.containsKey(id)) {
                result.add(byId.get(id));
            }
        }
        for (Map<String, Object> m : items) {
            if (!result.contains(m)) {
                result.add(m);
            }
        }
        return result;
    }

    /** 通用模式：POST 候选，响应 order 或 scores，返回重排后的列表 */
    private List<Map<String, Object>> reorderByGenericApi(Long userId, List<Map<String, Object>> candidates, AiRecommendContext ctx) {
        try {
            List<Map<String, Object>> payload = candidates.stream().map(m -> {
                Map<String, Object> c = new HashMap<>();
                c.put("id", m.get("id"));
                c.put("title", m.get("title"));
                c.put("summary", m.get("summary"));
                c.put("categoryName", m.get("categoryName"));
                return c;
            }).collect(Collectors.toList());
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("userId", userId);
            requestBody.put("candidates", payload);
            mergeAiContextIntoRequest(requestBody, ctx);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (StringUtils.hasText(apiKey)) {
                headers.set("Authorization", "Bearer " + apiKey);
                headers.set("X-API-Key", apiKey);
            }
            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);
            ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.POST, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode orderNode = root.get("order");
                if (orderNode != null && orderNode.isArray()) {
                    List<Long> order = new ArrayList<>();
                    for (JsonNode n : orderNode) {
                        order.add(n.isNumber() ? n.asLong() : Long.parseLong(n.asText()));
                    }
                    return reorderListByIds(candidates, order);
                }
                JsonNode scoresNode = root.get("scores");
                if (scoresNode != null && scoresNode.isArray()) {
                    List<Double> scores = new ArrayList<>();
                    for (JsonNode n : scoresNode) {
                        scores.add(n.isNumber() ? n.asDouble() : 0);
                    }
                    if (scores.size() >= candidates.size()) {
                        List<Integer> indices = new ArrayList<>();
                        for (int i = 0; i < candidates.size(); i++) {
                            indices.add(i);
                        }
                        indices.sort(Comparator.comparingDouble(i -> -scores.get(i)));
                        List<Map<String, Object>> result = new ArrayList<>();
                        for (int i : indices) {
                            result.add(candidates.get(i));
                        }
                        return result;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("AI 排序（通用 API）调用失败，使用原顺序: {}", e.getMessage());
        }
        return new ArrayList<>(candidates);
    }

    @Override
    public void enrichRecommendReasons(Long userId, List<Map<String, Object>> items, AiRecommendContext ctx) {
        if (ctx == null) {
            ctx = AiRecommendContext.empty();
        }
        if (!enabled || items == null || items.isEmpty()) {
            return;
        }
        if (isLlmChatMode() && StringUtils.hasText(apiUrl) && StringUtils.hasText(apiKey) && StringUtils.hasText(model)) {
            enrichByLlmChat(items, ctx);
            return;
        }
        if (!isLlmChatMode() && StringUtils.hasText(apiUrl)) {
            enrichByGenericApi(userId, items, ctx);
        }
    }

    private boolean isLlmChatMode() {
        if ("doubao".equalsIgnoreCase(provider)) {
            return true;
        }
        return apiUrl != null && (apiUrl.contains("volces.com") || apiUrl.contains("doubao") || apiUrl.contains("chat/completions"));
    }

    /** 调用豆包/火山方舟等 OpenAI 兼容的 chat 接口 */
    private void enrichByLlmChat(List<Map<String, Object>> items, AiRecommendContext ctx) {
        try {
            StringBuilder prompt = new StringBuilder();
            prompt.append("请针对以下消防科普推荐列表，为每条内容生成一句简短的推荐理由（不超过30字）。严格按顺序输出，每行一条理由，不要编号、不要空行。\n");
            appendUserContextToPrompt(prompt, ctx);
            prompt.append("\n");
            for (int i = 0; i < items.size(); i++) {
                Map<String, Object> m = items.get(i);
                String title = m.get("title") != null ? m.get("title").toString() : "";
                String summary = m.get("summary") != null ? m.get("summary").toString() : "";
                prompt.append(i + 1).append(". 标题：").append(title);
                if (StringUtils.hasText(summary)) {
                    prompt.append(" 摘要：").append(summary.length() > 80 ? summary.substring(0, 80) + "…" : summary);
                }
                prompt.append("\n");
            }

            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", model);
            ArrayNode messages = objectMapper.createArrayNode();
            ObjectNode userMsg = objectMapper.createObjectNode();
            userMsg.put("role", "user");
            userMsg.put("content", prompt.toString());
            messages.add(userMsg);
            body.set("messages", messages);
            body.put("temperature", 0.7);
            body.put("max_tokens", 2048);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            HttpEntity<String> entity = new HttpEntity<>(body.toString(), headers);
            ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.POST, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                logDoubaoErrorIfPresent(root);
                JsonNode choices = root.get("choices");
                if (choices != null && choices.isArray() && choices.size() > 0) {
                    String content = choices.get(0).path("message").path("content").asText("");
                    String[] lines = content.split("\n");
                    for (int i = 0; i < items.size() && i < lines.length; i++) {
                        String reason = lines[i].trim();
                        if (reason.matches("^\\d+[.．]\\s*.*")) {
                            reason = reason.replaceFirst("^\\d+[.．]\\s*", "");
                        }
                        if (StringUtils.hasText(reason)) {
                            items.get(i).put("recommendReason", reason);
                        }
                    }
                }
            }
        } catch (HttpStatusCodeException e) {
            log.warn("AI 推荐理由（豆包/方舟）HTTP {} 响应: {}", e.getStatusCode().value(), truncateForLog(e.getResponseBodyAsString()));
        } catch (Exception e) {
            log.warn("AI 推荐增强（大模型）调用失败，使用规则推荐结果: {}", e.getMessage());
        }
    }

    /** 火山方舟错误体通常为 { "error": { "message": "...", "code": "..." } } */
    private void logDoubaoErrorIfPresent(JsonNode root) {
        if (root == null || !root.has("error")) {
            return;
        }
        JsonNode err = root.get("error");
        String msg = err.path("message").asText(err.toString());
        log.warn("豆包/方舟 API 返回 error: {}", msg);
    }

    private static String truncateForLog(String s) {
        if (s == null) {
            return "";
        }
        String t = s.replaceAll("\\s+", " ").trim();
        return t.length() > 800 ? t.substring(0, 800) + "…" : t;
    }

    private void enrichByGenericApi(Long userId, List<Map<String, Object>> items, AiRecommendContext ctx) {
        try {
            List<Map<String, Object>> candidates = items.stream().map(m -> {
                Map<String, Object> c = new HashMap<>();
                c.put("id", m.get("id"));
                c.put("title", m.get("title"));
                c.put("summary", m.get("summary"));
                c.put("categoryName", m.get("categoryName"));
                return c;
            }).collect(Collectors.toList());
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("userId", userId);
            requestBody.put("candidates", candidates);
            mergeAiContextIntoRequest(requestBody, ctx);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (StringUtils.hasText(apiKey)) {
                headers.set("Authorization", "Bearer " + apiKey);
                headers.set("X-API-Key", apiKey);
            }
            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);
            ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.POST, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode reasons = root.has("reasons") ? root.get("reasons") : (root.has("items") ? root.get("items") : null);
                if (reasons != null && reasons.isArray()) {
                    for (int i = 0; i < reasons.size() && i < items.size(); i++) {
                        JsonNode node = reasons.get(i);
                        String reason = node.isTextual() ? node.asText() : (node.has("recommendReason") ? node.get("recommendReason").asText() : null);
                        if (reason != null) {
                            items.get(i).put("recommendReason", reason);
                        }
                    }
                }
                JsonNode order = root.get("order");
                if (order != null && order.isArray()) {
                    List<Object> idOrder = new ArrayList<>();
                    for (JsonNode n : order) {
                        idOrder.add(n.isNumber() ? n.asLong() : n.asText());
                    }
                    List<Map<String, Object>> sorted = new ArrayList<>();
                    for (Object id : idOrder) {
                        for (Map<String, Object> item : items) {
                            if (Objects.equals(item.get("id"), id) || Objects.equals(String.valueOf(item.get("id")), String.valueOf(id))) {
                                sorted.add(item);
                                break;
                            }
                        }
                    }
                    if (sorted.size() == items.size()) {
                        items.clear();
                        items.addAll(sorted);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("AI 推荐增强调用失败，使用规则推荐结果: {}", e.getMessage());
        }
    }
}
