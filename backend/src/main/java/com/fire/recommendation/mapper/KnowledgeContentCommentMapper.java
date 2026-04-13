package com.fire.recommendation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fire.recommendation.entity.KnowledgeContentComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface KnowledgeContentCommentMapper extends BaseMapper<KnowledgeContentComment> {

    @Select("SELECT COUNT(*) FROM knowledge_content_comment WHERE content_id = #{cid} AND deleted = 0 AND status = 1")
    Long countVisibleByContentId(@Param("cid") Long contentId);
}
