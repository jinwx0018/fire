package com.fire.recommendation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("equipment_type")
public class EquipmentType {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Integer sort;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
