package com.fire.recommendation.service;

import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;
import java.util.Map;

public interface EquipmentService {

    IPage<Map<String, Object>> list(Integer pageNum, Integer pageSize, String keyword, Long typeId);

    Map<String, Object> getDetail(Long id);

    List<Map<String, Object>> typeList();

    Long adminSave(Map<String, Object> body);

    void adminUpdate(Long id, Map<String, Object> body);

    void adminDelete(Long id);

    Map<String, Object> importExcel(org.springframework.web.multipart.MultipartFile file);
}
