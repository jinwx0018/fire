package com.fire.recommendation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fire.recommendation.entity.ForumComment;
import com.fire.recommendation.entity.ForumPost;
import com.fire.recommendation.entity.SysUser;
import com.fire.recommendation.exception.BusinessException;
import com.fire.recommendation.common.ResultCode;
import com.fire.recommendation.mapper.ForumCommentMapper;
import com.fire.recommendation.mapper.ForumPostMapper;
import com.fire.recommendation.mapper.SysUserMapper;
import com.fire.recommendation.service.ForumService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ForumServiceImpl implements ForumService {

    private static final String LIKE_KEY_PREFIX = "forum:post:like:";

    private final ForumPostMapper postMapper;
    private final ForumCommentMapper commentMapper;
    private final SysUserMapper userMapper;
    private final StringRedisTemplate redisTemplate;

    @Override
    public IPage<Map<String, Object>> postList(Integer pageNum, Integer pageSize, Integer status, String keyword) {
        Page<ForumPost> page = new Page<>(pageNum != null ? pageNum : 1, pageSize != null ? pageSize : 10);
        LambdaQueryWrapper<ForumPost> q = new LambdaQueryWrapper<>();
        q.eq(ForumPost::getDeleted, 0);
        if (status != null) q.eq(ForumPost::getStatus, status);
        if (StringUtils.hasText(keyword)) q.and(w -> w.like(ForumPost::getTitle, keyword).or().like(ForumPost::getContent, keyword));
        q.orderByDesc(ForumPost::getCreateTime);
        IPage<ForumPost> result = postMapper.selectPage(page, q);
        return result.convert(this::postToMap);
    }

    @Override
    public Map<String, Object> postDetail(Long postId, Long userId) {
        ForumPost p = postMapper.selectById(postId);
        if (p == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        Map<String, Object> m = postToMap(p);
        String key = LIKE_KEY_PREFIX + postId;
        Boolean liked = userId != null && Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(key, String.valueOf(userId)));
        m.put("liked", Boolean.TRUE.equals(liked));
        return m;
    }

    @Override
    public Long publishPost(Long userId, String title, String content) {
        if (!StringUtils.hasText(title)) {
            throw new BusinessException("标题不能为空");
        }
        if (content == null) {
            content = "";
        }
        ForumPost p = new ForumPost();
        p.setUserId(userId);
        p.setTitle(title.trim());
        p.setContent(content);
        p.setStatus(0);
        p.setViewCount(0);
        p.setLikeCount(0);
        p.setCommentCount(0);
        postMapper.insert(p);
        return p.getId();
    }

    @Override
    public void updatePost(Long postId, Long userId, String title, String content) {
        ForumPost p = postMapper.selectById(postId);
        if (p == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (!p.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        if (StringUtils.hasText(title)) p.setTitle(title);
        if (content != null) p.setContent(content);
        postMapper.updateById(p);
    }

    @Override
    public void deletePost(Long postId, Long userId) {
        ForumPost p = postMapper.selectById(postId);
        if (p == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (!p.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        postMapper.deleteById(postId);
    }

    @Override
    public Map<String, Object> likePost(Long postId, Long userId) {
        ForumPost p = postMapper.selectById(postId);
        if (p == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        String key = LIKE_KEY_PREFIX + postId;
        Boolean isMember = redisTemplate.opsForSet().isMember(key, String.valueOf(userId));
        boolean liked;
        if (Boolean.TRUE.equals(isMember)) {
            redisTemplate.opsForSet().remove(key, String.valueOf(userId));
            liked = false;
        } else {
            redisTemplate.opsForSet().add(key, String.valueOf(userId));
            liked = true;
        }
        Long count = redisTemplate.opsForSet().size(key);
        if (count != null) {
            postMapper.update(null, new LambdaUpdateWrapper<ForumPost>().eq(ForumPost::getId, postId).set(ForumPost::getLikeCount, count.intValue()));
        }
        Map<String, Object> m = new HashMap<>();
        m.put("liked", liked);
        m.put("likeCount", count != null ? count.intValue() : 0);
        return m;
    }

    @Override
    public IPage<Map<String, Object>> commentList(Long postId, Integer pageNum, Integer pageSize) {
        Page<ForumComment> page = new Page<>(pageNum != null ? pageNum : 1, pageSize != null ? pageSize : 10);
        LambdaQueryWrapper<ForumComment> q = new LambdaQueryWrapper<>();
        q.eq(ForumComment::getPostId, postId).eq(ForumComment::getDeleted, 0).orderByAsc(ForumComment::getCreateTime);
        IPage<ForumComment> result = commentMapper.selectPage(page, q);
        return result.convert(this::commentToMap);
    }

    @Override
    public Long addComment(Long userId, Long postId, String content, Long parentId) {
        ForumPost post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        ForumComment c = new ForumComment();
        c.setUserId(userId);
        c.setPostId(postId);
        c.setContent(content);
        c.setParentId(parentId);
        commentMapper.insert(c);
        postMapper.update(null, new LambdaUpdateWrapper<ForumPost>().eq(ForumPost::getId, postId)
                .setSql("comment_count = comment_count + 1"));
        return c.getId();
    }

    @Override
    public void deleteComment(Long commentId, Long userId) {
        ForumComment c = commentMapper.selectById(commentId);
        if (c == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (!c.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        commentMapper.deleteById(commentId);
    }

    @Override
    public void auditPost(Long postId, Integer status, String rejectReason) {
        ForumPost p = postMapper.selectById(postId);
        if (p == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        p.setStatus(status);
        p.setRejectReason(rejectReason);
        postMapper.updateById(p);
    }

    private Map<String, Object> postToMap(ForumPost p) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", p.getId());
        m.put("userId", p.getUserId());
        SysUser u = userMapper.selectById(p.getUserId());
        m.put("userName", u != null ? u.getUsername() : null);
        m.put("avatar", u != null ? u.getAvatar() : null);
        m.put("title", p.getTitle());
        m.put("content", p.getContent());
        m.put("status", p.getStatus());
        m.put("viewCount", p.getViewCount());
        m.put("likeCount", p.getLikeCount());
        m.put("commentCount", p.getCommentCount());
        m.put("createTime", p.getCreateTime());
        return m;
    }

    private Map<String, Object> commentToMap(ForumComment c) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", c.getId());
        m.put("userId", c.getUserId());
        m.put("postId", c.getPostId());
        m.put("content", c.getContent());
        m.put("parentId", c.getParentId());
        m.put("createTime", c.getCreateTime());
        SysUser u = userMapper.selectById(c.getUserId());
        m.put("userName", u != null ? u.getUsername() : null);
        m.put("avatar", u != null ? u.getAvatar() : null);
        return m;
    }
}
