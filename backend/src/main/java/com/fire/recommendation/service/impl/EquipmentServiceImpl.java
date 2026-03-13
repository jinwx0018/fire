package com.fire.recommendation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fire.recommendation.entity.Equipment;
import com.fire.recommendation.entity.EquipmentType;
import com.fire.recommendation.mapper.EquipmentMapper;
import com.fire.recommendation.mapper.EquipmentTypeMapper;
import com.fire.recommendation.service.EquipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EquipmentServiceImpl implements EquipmentService {

    private final EquipmentMapper equipmentMapper;
    private final EquipmentTypeMapper typeMapper;

    @Override
    public IPage<Map<String, Object>> list(Integer pageNum, Integer pageSize, String keyword, Long typeId) {
        Page<Equipment> page = new Page<>(pageNum != null ? pageNum : 1, pageSize != null ? pageSize : 10);
        LambdaQueryWrapper<Equipment> q = new LambdaQueryWrapper<>();
        q.eq(Equipment::getDeleted, 0).eq(Equipment::getStatus, 1);
        if (StringUtils.hasText(keyword)) q.like(Equipment::getName, keyword);
        if (typeId != null) q.eq(Equipment::getTypeId, typeId);
        q.orderByAsc(Equipment::getSort);
        IPage<Equipment> result = equipmentMapper.selectPage(page, q);
        return result.convert(e -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", e.getId());
            m.put("name", e.getName());
            m.put("typeId", e.getTypeId());
            EquipmentType t = typeMapper.selectById(e.getTypeId());
            m.put("typeName", t != null ? t.getName() : null);
            m.put("cover", e.getCover());
            m.put("summary", e.getSummary());
            return m;
        });
    }

    @Override
    public Map<String, Object> getDetail(Long id) {
        Equipment e = equipmentMapper.selectById(id);
        if (e == null) return null;
        Map<String, Object> m = new HashMap<>();
        m.put("id", e.getId());
        m.put("name", e.getName());
        m.put("typeId", e.getTypeId());
        EquipmentType t = typeMapper.selectById(e.getTypeId());
        m.put("typeName", t != null ? t.getName() : null);
        m.put("cover", e.getCover());
        m.put("usageSteps", e.getUsageSteps());
        m.put("checkPoints", e.getCheckPoints());
        m.put("faultSolution", e.getFaultSolution());
        m.put("images", e.getImages());
        m.put("summary", e.getSummary());
        m.put("createTime", e.getCreateTime());
        return m;
    }

    @Override
    public List<Map<String, Object>> typeList() {
        List<EquipmentType> list = typeMapper.selectList(new LambdaQueryWrapper<EquipmentType>().orderByAsc(EquipmentType::getSort));
        return list.stream().map(t -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", t.getId());
            m.put("name", t.getName());
            return m;
        }).collect(Collectors.toList());
    }

    @Override
    public Long adminSave(Map<String, Object> body) {
        Equipment e = new Equipment();
        e.setName((String) body.get("name"));
        e.setTypeId(Long.valueOf(body.get("typeId").toString()));
        e.setCover((String) body.get("cover"));
        e.setUsageSteps((String) body.get("usageSteps"));
        e.setCheckPoints((String) body.get("checkPoints"));
        e.setFaultSolution((String) body.get("faultSolution"));
        e.setSummary((String) body.get("summary"));
        e.setStatus(1);
        equipmentMapper.insert(e);
        return e.getId();
    }

    @Override
    public void adminUpdate(Long id, Map<String, Object> body) {
        Equipment e = equipmentMapper.selectById(id);
        if (e == null) return;
        if (body.containsKey("name")) e.setName((String) body.get("name"));
        if (body.containsKey("typeId")) e.setTypeId(Long.valueOf(body.get("typeId").toString()));
        if (body.containsKey("cover")) e.setCover((String) body.get("cover"));
        if (body.containsKey("usageSteps")) e.setUsageSteps((String) body.get("usageSteps"));
        if (body.containsKey("checkPoints")) e.setCheckPoints((String) body.get("checkPoints"));
        if (body.containsKey("faultSolution")) e.setFaultSolution((String) body.get("faultSolution"));
        if (body.containsKey("summary")) e.setSummary((String) body.get("summary"));
        equipmentMapper.updateById(e);
    }

    @Override
    public void adminDelete(Long id) {
        equipmentMapper.deleteById(id);
    }

    @Override
    public Map<String, Object> importExcel(MultipartFile file) {
        // TODO: EasyExcel 解析并批量插入
        Map<String, Object> m = new HashMap<>();
        m.put("successCount", 0);
        m.put("failCount", 0);
        return m;
    }
}
