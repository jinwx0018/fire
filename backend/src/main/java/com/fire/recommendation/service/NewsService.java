package com.fire.recommendation.service;

import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;
import java.util.Map;

public interface NewsService {

    /**
     * 用户端公开列表：标题/摘要均为 LIKE 模糊。
     * 地区：{@code regions} 多选精确匹配（OR）；未传时可用单参数 {@code region}。
     *
     * @param categoryId   分类字典 id（优先于 category 文本）
     * @param category     自由分类精确匹配（兼容旧数据；库无 category 列时不生效）
     * @param keyword      标题或摘要模糊
     */
    IPage<Map<String, Object>> list(Integer pageNum, Integer pageSize, List<String> regions, String region, Long categoryId, String category,
                                    String title, String keyword, String orderBy, String order);

    IPage<Map<String, Object>> adminPage(Integer pageNum, Integer pageSize, List<String> regions, String region, Long categoryId, String category,
                                         String title, String keyword, String orderBy, String order, Integer status);

    /**
     * 用户端地区筛选项：Mapper 去重结果经 {@link com.fire.recommendation.constant.NewsRegionFallback#orProvincialFallback}，
     * 无有效地区时返回 34 个省级后备列表。
     */
    List<String> listDistinctRegions();

    /**
     * 管理端地区筛选项：同上，Mapper 为未删除新闻中的地区去重。
     */
    List<String> listDistinctRegionsForAdmin();

    /**
     * 用户端新闻详情。
     *
     * @param recordView  false 时不增加浏览量（如前端刷新元数据）
     * @param clientIp    匿名访客去重用，{@link com.fire.recommendation.util.HttpClientIp#resolve}
     */
    Map<String, Object> getDetail(Long id, Long viewerUserId, boolean recordView, String clientIp);

    Map<String, Object> adminGetDetail(Long id);

    Long adminSave(Long publisherId, Map<String, Object> body);

    void adminUpdate(Long operatorId, Long id, Map<String, Object> body);

    void adminDelete(Long operatorId, Long id);

    /** @return liked、likeCount，供前端就地更新，避免再次 getDetail 重复计浏览 */
    Map<String, Object> toggleLike(Long newsId, Long userId);
}
