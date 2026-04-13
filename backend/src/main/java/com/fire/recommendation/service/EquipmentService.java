package com.fire.recommendation.service;

import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;
import java.util.Map;

public interface EquipmentService {

    IPage<Map<String, Object>> list(Integer pageNum, Integer pageSize, String keyword, Long typeId);

    /**
     * 管理端分页：未删除的全部器材（可含未上架），keyword、typeId、status 可选；status 为空表示不限。
     */
    IPage<Map<String, Object>> adminPage(Integer pageNum, Integer pageSize, String keyword, Long typeId, Integer status);

    /** 用户端详情：仅已上架 status=1；viewerUserId 可为 null（未登录），collected 恒为 false */
    Map<String, Object> getDetail(Long id, Long viewerUserId);

    /** 管理端详情：任意上架状态，未删除即可 */
    Map<String, Object> adminGetDetail(Long id);

    List<Map<String, Object>> typeList();

    /** 管理端器材类型分页，按 sort、id 升序 */
    IPage<Map<String, Object>> adminTypePage(Integer pageNum, Integer pageSize);

    Long adminSave(Map<String, Object> body);

    void adminUpdate(Long id, Map<String, Object> body);

    void adminDelete(Long id);

    Map<String, Object> importExcel(org.springframework.web.multipart.MultipartFile file);

    Long adminSaveType(Map<String, Object> body);

    void adminUpdateType(Long id, Map<String, Object> body);

    void adminDeleteType(Long id);
}
