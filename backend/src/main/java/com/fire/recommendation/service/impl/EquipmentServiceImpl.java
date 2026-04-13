package com.fire.recommendation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.fire.recommendation.dto.EquipmentImportRow;
import com.fire.recommendation.entity.Equipment;
import com.fire.recommendation.entity.EquipmentType;
import com.fire.recommendation.common.ResultCode;
import com.fire.recommendation.exception.BusinessException;
import com.fire.recommendation.constant.CollectionTargetType;
import com.fire.recommendation.mapper.EquipmentMapper;
import com.fire.recommendation.mapper.EquipmentTypeMapper;
import com.fire.recommendation.service.EquipmentService;
import com.fire.recommendation.service.UserCollectionService;
import com.fire.recommendation.util.EquipmentMediaUrlValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EquipmentServiceImpl implements EquipmentService {

    private final EquipmentMapper equipmentMapper;
    private final EquipmentTypeMapper typeMapper;
    private final UserCollectionService userCollectionService;

    @Override
    public IPage<Map<String, Object>> list(Integer pageNum, Integer pageSize, String keyword, Long typeId) {
        Page<Equipment> page = new Page<>(pageNum != null ? pageNum : 1, pageSize != null ? pageSize : 10);
        LambdaQueryWrapper<Equipment> q = new LambdaQueryWrapper<>();
        q.eq(Equipment::getDeleted, 0).eq(Equipment::getStatus, 1);
        if (StringUtils.hasText(keyword)) q.like(Equipment::getName, keyword);
        if (typeId != null) q.eq(Equipment::getTypeId, typeId);
        q.orderByAsc(Equipment::getSort);
        IPage<Equipment> result = equipmentMapper.selectPage(page, q);
        return result.convert(this::toUserListRow);
    }

    @Override
    public IPage<Map<String, Object>> adminPage(Integer pageNum, Integer pageSize, String keyword, Long typeId, Integer status) {
        Page<Equipment> page = new Page<>(pageNum != null ? pageNum : 1, pageSize != null ? pageSize : 10);
        LambdaQueryWrapper<Equipment> q = new LambdaQueryWrapper<>();
        q.eq(Equipment::getDeleted, 0);
        if (status != null) {
            q.eq(Equipment::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            q.like(Equipment::getName, keyword);
        }
        if (typeId != null) {
            q.eq(Equipment::getTypeId, typeId);
        }
        q.orderByAsc(Equipment::getSort);
        IPage<Equipment> result = equipmentMapper.selectPage(page, q);
        return result.convert(this::toAdminListRow);
    }

    private Map<String, Object> toUserListRow(Equipment e) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", e.getId());
        m.put("name", e.getName());
        m.put("typeId", e.getTypeId());
        EquipmentType t = typeMapper.selectById(e.getTypeId());
        m.put("typeName", t != null ? t.getName() : null);
        m.put("cover", e.getCover());
        m.put("summary", e.getSummary());
        return m;
    }

    private Map<String, Object> toAdminListRow(Equipment e) {
        Map<String, Object> m = toUserListRow(e);
        m.put("status", e.getStatus());
        return m;
    }

    @Override
    public Map<String, Object> getDetail(Long id, Long viewerUserId) {
        Equipment e = equipmentMapper.selectById(id);
        if (e == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (e.getStatus() == null || !Integer.valueOf(1).equals(e.getStatus())) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        Map<String, Object> m = equipmentToMap(e);
        if (viewerUserId != null) {
            m.put("collected", userCollectionService.isCollected(viewerUserId, CollectionTargetType.EQUIPMENT, id));
        } else {
            m.put("collected", false);
        }
        return m;
    }

    @Override
    public Map<String, Object> adminGetDetail(Long id) {
        Equipment e = equipmentMapper.selectById(id);
        if (e == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        Map<String, Object> m = equipmentToMap(e);
        m.put("status", e.getStatus());
        return m;
    }

    private Map<String, Object> equipmentToMap(Equipment e) {
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
            m.put("sort", t.getSort());
            return m;
        }).collect(Collectors.toList());
    }

    @Override
    public IPage<Map<String, Object>> adminTypePage(Integer pageNum, Integer pageSize) {
        int pn = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int ps = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
        Page<EquipmentType> page = new Page<>(pn, ps);
        LambdaQueryWrapper<EquipmentType> q = new LambdaQueryWrapper<>();
        q.orderByAsc(EquipmentType::getSort).orderByAsc(EquipmentType::getId);
        return typeMapper.selectPage(page, q).convert(t -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", t.getId());
            m.put("name", t.getName());
            m.put("sort", t.getSort());
            return m;
        });
    }

    @Override
    public Long adminSave(Map<String, Object> body) {
        if (body == null) {
            throw new BusinessException("请求体不能为空");
        }
        String name = body.get("name") != null ? body.get("name").toString().trim() : null;
        if (!StringUtils.hasText(name)) {
            throw new BusinessException("名称不能为空");
        }
        Object tid = body.get("typeId");
        if (tid == null) {
            throw new BusinessException("类型 typeId 不能为空");
        }
        Long typeId;
        try {
            typeId = Long.valueOf(tid.toString().trim());
        } catch (NumberFormatException ex) {
            throw new BusinessException("typeId 无效");
        }
        Equipment e = new Equipment();
        e.setName(name);
        e.setTypeId(typeId);
        e.setCover(strOrNull(body.get("cover")));
        e.setUsageSteps(strOrNull(body.get("usageSteps")));
        e.setCheckPoints(strOrNull(body.get("checkPoints")));
        e.setFaultSolution(strOrNull(body.get("faultSolution")));
        e.setSummary(strOrNull(body.get("summary")));
        e.setImages(strOrNull(body.get("images")));
        e.setStatus(parseEquipmentStatus(body.get("status"), 1));
        EquipmentMediaUrlValidator.validateCoverAndImages(e.getCover(), e.getImages());
        equipmentMapper.insert(e);
        return e.getId();
    }

    @Override
    public void adminUpdate(Long id, Map<String, Object> body) {
        Equipment e = equipmentMapper.selectById(id);
        if (e == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (body == null) {
            return;
        }
        if (body.containsKey("name")) {
            String name = body.get("name") != null ? body.get("name").toString().trim() : null;
            if (!StringUtils.hasText(name)) {
                throw new BusinessException("名称不能为空");
            }
            e.setName(name);
        }
        if (body.containsKey("typeId")) {
            Object tid = body.get("typeId");
            if (tid == null) {
                throw new BusinessException("typeId 不能为空");
            }
            try {
                e.setTypeId(Long.valueOf(tid.toString().trim()));
            } catch (NumberFormatException ex) {
                throw new BusinessException("typeId 无效");
            }
        }
        if (body.containsKey("cover")) e.setCover(strOrNull(body.get("cover")));
        if (body.containsKey("usageSteps")) e.setUsageSteps(strOrNull(body.get("usageSteps")));
        if (body.containsKey("checkPoints")) e.setCheckPoints(strOrNull(body.get("checkPoints")));
        if (body.containsKey("faultSolution")) e.setFaultSolution(strOrNull(body.get("faultSolution")));
        if (body.containsKey("summary")) e.setSummary(strOrNull(body.get("summary")));
        if (body.containsKey("images")) e.setImages(strOrNull(body.get("images")));
        EquipmentMediaUrlValidator.validateCoverAndImages(e.getCover(), e.getImages());
        equipmentMapper.updateById(e);
    }

    @Override
    public void adminDelete(Long id) {
        equipmentMapper.deleteById(id);
    }

    @Override
    public Map<String, Object> importExcel(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请上传 Excel 文件");
        }
        List<EquipmentImportRow> rows = new ArrayList<>();
        try {
            EasyExcel.read(file.getInputStream(), EquipmentImportRow.class, new ReadListener<EquipmentImportRow>() {
                @Override
                public void invoke(EquipmentImportRow data, AnalysisContext context) {
                    if (data != null && StringUtils.hasText(data.getName())) {
                        rows.add(data);
                    }
                }

                @Override
                public void doAfterAllAnalysed(AnalysisContext context) {
                }
            }).sheet().doRead();
        } catch (IOException e) {
            throw new BusinessException("读取 Excel 失败：" + e.getMessage());
        }

        if (rows.size() > 2000) {
            throw new BusinessException("单次导入行数不能超过 2000，请拆分文件");
        }

        List<EquipmentType> typeEntities = typeMapper.selectList(null);
        Map<String, Long> typeByName = new HashMap<>();
        for (EquipmentType t : typeEntities) {
            if (t.getName() != null) {
                typeByName.putIfAbsent(t.getName().trim(), t.getId());
            }
        }

        int success = 0;
        int fail = 0;
        List<String> errors = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            EquipmentImportRow r = rows.get(i);
            int excelRow = i + 2;
            if (!StringUtils.hasText(r.getTypeName())) {
                fail++;
                if (errors.size() < 30) {
                    errors.add("第" + excelRow + "行：类型不能为空");
                }
                continue;
            }
            Long typeId = typeByName.get(r.getTypeName().trim());
            if (typeId == null) {
                fail++;
                if (errors.size() < 30) {
                    errors.add("第" + excelRow + "行：未找到类型「" + r.getTypeName().trim() + "」");
                }
                continue;
            }
            try {
                Equipment e = new Equipment();
                e.setName(r.getName().trim());
                e.setTypeId(typeId);
                String coverVal = StringUtils.hasText(r.getCover()) ? r.getCover().trim() : null;
                EquipmentMediaUrlValidator.validateOptionalUrl("封面URL", coverVal);
                e.setCover(coverVal);
                e.setSummary(StringUtils.hasText(r.getSummary()) ? r.getSummary().trim() : null);
                e.setUsageSteps(StringUtils.hasText(r.getUsageSteps()) ? r.getUsageSteps().trim() : null);
                e.setCheckPoints(StringUtils.hasText(r.getCheckPoints()) ? r.getCheckPoints().trim() : null);
                e.setFaultSolution(StringUtils.hasText(r.getFaultSolution()) ? r.getFaultSolution().trim() : null);
                e.setStatus(1);
                equipmentMapper.insert(e);
                success++;
            } catch (Exception ex) {
                fail++;
                if (errors.size() < 30) {
                    errors.add("第" + excelRow + "行：" + ex.getMessage());
                }
            }
        }

        Map<String, Object> m = new HashMap<>();
        m.put("successCount", success);
        m.put("failCount", fail);
        if (!errors.isEmpty()) {
            m.put("errors", errors);
        }
        return m;
    }

    @Override
    public Long adminSaveType(Map<String, Object> body) {
        if (body == null) {
            throw new BusinessException("请求体不能为空");
        }
        String name = body.get("name") != null ? body.get("name").toString().trim() : null;
        if (!StringUtils.hasText(name)) {
            throw new BusinessException("类型名称不能为空");
        }
        EquipmentType t = new EquipmentType();
        t.setName(name);
        if (body.get("sort") != null) {
            try {
                t.setSort(Integer.valueOf(body.get("sort").toString().trim()));
            } catch (NumberFormatException ex) {
                throw new BusinessException("sort 无效");
            }
        } else {
            t.setSort(0);
        }
        typeMapper.insert(t);
        return t.getId();
    }

    @Override
    public void adminUpdateType(Long id, Map<String, Object> body) {
        EquipmentType t = typeMapper.selectById(id);
        if (t == null) {
            throw new BusinessException("类型不存在");
        }
        if (body.containsKey("name")) {
            String name = body.get("name") != null ? body.get("name").toString().trim() : null;
            if (!StringUtils.hasText(name)) {
                throw new BusinessException("类型名称不能为空");
            }
            t.setName(name);
        }
        if (body.containsKey("sort") && body.get("sort") != null) {
            try {
                t.setSort(Integer.valueOf(body.get("sort").toString().trim()));
            } catch (NumberFormatException ex) {
                throw new BusinessException("sort 无效");
            }
        }
        typeMapper.updateById(t);
    }

    @Override
    public void adminDeleteType(Long id) {
        long cnt = equipmentMapper.selectCount(
                new LambdaQueryWrapper<Equipment>().eq(Equipment::getTypeId, id).eq(Equipment::getDeleted, 0));
        if (cnt > 0) {
            throw new BusinessException("该类型下仍有器材，无法删除");
        }
        typeMapper.deleteById(id);
    }

    private static String strOrNull(Object o) {
        if (o == null) {
            return null;
        }
        String s = o.toString().trim();
        return s.isEmpty() ? null : s;
    }

    /** 用户端仅展示 status=1；0 表示下架仅管理端可见 */
    private static int parseEquipmentStatus(Object raw, int defaultStatus) {
        if (raw == null) {
            return defaultStatus;
        }
        try {
            int v = Integer.parseInt(raw.toString().trim());
            if (v == 0 || v == 1) {
                return v;
            }
        } catch (NumberFormatException ignored) {
            /* fallthrough */
        }
        throw new BusinessException("status 只能为 0（下架）或 1（上架）");
    }
}
