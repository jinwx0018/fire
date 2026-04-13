package com.fire.recommendation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fire.recommendation.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /** 未逻辑删除用户按角色计数：roleId, cnt */
    @Select("SELECT role_id AS roleId, COUNT(*) AS cnt FROM sys_user WHERE (deleted IS NULL OR deleted = 0) GROUP BY role_id")
    List<Map<String, Object>> selectRoleUserCounts();
}
