package com.fire.recommendation.constant;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 新闻「地区」筛选项：库中无任何有效地区时使用省级后备列表（34 个省级行政区）。
 */
public final class NewsRegionFallback {

    /** 34 个省级行政区（直辖市、省、自治区、特别行政区） */
    public static final List<String> PROVINCIAL_LEVEL = List.of(
            "北京市", "天津市", "上海市", "重庆市",
            "河北省", "山西省", "辽宁省", "吉林省", "黑龙江省",
            "江苏省", "浙江省", "安徽省", "福建省", "江西省", "山东省",
            "河南省", "湖北省", "湖南省", "广东省", "海南省",
            "四川省", "贵州省", "云南省", "陕西省", "甘肃省", "青海省",
            "台湾省",
            "内蒙古自治区", "广西壮族自治区", "西藏自治区", "宁夏回族自治区", "新疆维吾尔自治区",
            "香港特别行政区", "澳门特别行政区");

    private NewsRegionFallback() {
    }

    /**
     * 去重排序后的地区列表；若查询为 null、空或去重后为空，则返回 34 个省级后备列表副本。
     * 若库中有至少一条有效地区字符串，则仅返回库中去重结果（不含后备合并）。
     */
    public static List<String> orProvincialFallback(List<String> rawFromDb) {
        if (rawFromDb == null || rawFromDb.isEmpty()) {
            return List.copyOf(PROVINCIAL_LEVEL);
        }
        List<String> fromDb = rawFromDb.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());
        if (fromDb.isEmpty()) {
            return List.copyOf(PROVINCIAL_LEVEL);
        }
        return fromDb;
    }
}
