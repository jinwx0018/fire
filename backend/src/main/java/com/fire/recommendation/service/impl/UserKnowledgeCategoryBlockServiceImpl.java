package com.fire.recommendation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fire.recommendation.entity.KnowledgeCategory;
import com.fire.recommendation.entity.UserKnowledgeCategoryBlock;
import com.fire.recommendation.exception.BusinessException;
import com.fire.recommendation.mapper.KnowledgeCategoryMapper;
import com.fire.recommendation.mapper.UserKnowledgeCategoryBlockMapper;
import com.fire.recommendation.service.UserKnowledgeCategoryBlockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserKnowledgeCategoryBlockServiceImpl implements UserKnowledgeCategoryBlockService {

    private static final int MAX_BLOCKS = 30;

    private final UserKnowledgeCategoryBlockMapper blockMapper;
    private final KnowledgeCategoryMapper categoryMapper;

    @Override
    public List<Long> listBlockedCategoryIds(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return blockMapper.selectList(new LambdaQueryWrapper<UserKnowledgeCategoryBlock>()
                        .eq(UserKnowledgeCategoryBlock::getUserId, userId))
                .stream()
                .map(UserKnowledgeCategoryBlock::getCategoryId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replaceBlockedCategories(Long userId, List<Long> categoryIds) {
        if (userId == null) {
            throw new BusinessException("未登录");
        }
        Set<Long> unique = new LinkedHashSet<>();
        if (categoryIds != null) {
            for (Long id : categoryIds) {
                if (id == null || id <= 0) {
                    continue;
                }
                unique.add(id);
                if (unique.size() > MAX_BLOCKS) {
                    throw new BusinessException("最多屏蔽 " + MAX_BLOCKS + " 个分类");
                }
            }
        }
        blockMapper.delete(new LambdaQueryWrapper<UserKnowledgeCategoryBlock>()
                .eq(UserKnowledgeCategoryBlock::getUserId, userId));
        for (Long cid : unique) {
            KnowledgeCategory cat = categoryMapper.selectById(cid);
            if (cat == null) {
                throw new BusinessException("分类不存在：id=" + cid);
            }
            UserKnowledgeCategoryBlock row = new UserKnowledgeCategoryBlock();
            row.setUserId(userId);
            row.setCategoryId(cid);
            blockMapper.insert(row);
        }
    }
}
