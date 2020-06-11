package com.lzj.search.service;

import com.lzj.search.base.ServiceMultiResult;
import com.lzj.search.base.ServiceResult;
import com.lzj.search.dto.HouseDTO;
import com.lzj.search.form.DatatableSearch;
import com.lzj.search.form.HouseForm;
import com.lzj.search.form.MapSearch;
import com.lzj.search.form.RentSearch;

/**
 * @author lizijian
 */
public interface HouseService {

    /**
     * 新增
     * @param houseForm
     * @return
     */
    ServiceResult<HouseDTO> save(HouseForm houseForm);

    ServiceResult update(HouseForm houseForm);

    ServiceMultiResult<HouseDTO> adminQuery(DatatableSearch searchBody);

    /**
     * 查询完整房源信息
     * @param id
     * @return
     */
    ServiceResult<HouseDTO> findCompleteOne(Long id);

    /**
     * 移除图片
     * @param id
     * @return
     */
    ServiceResult removePhoto(Long id);

    /**
     * 更新封面
     * @param coverId
     * @param targetId
     * @return
     */
    ServiceResult updateCover(Long coverId, Long targetId);

    /**
     * 新增标签
     * @param houseId
     * @param tag
     * @return
     */
    ServiceResult addTag(Long houseId, String tag);

    /**
     * 移除标签
     * @param houseId
     * @param tag
     * @return
     */
    ServiceResult removeTag(Long houseId, String tag);

    /**
     * 更新房源状态
     * @param id
     * @param status
     * @return
     */
    ServiceResult updateStatus(Long id, int status);

    /**
     * 查询房源信息集
     * @param rentSearch
     * @return
     */
    ServiceMultiResult<HouseDTO> query(RentSearch rentSearch);

    ServiceMultiResult<HouseDTO> wholeMapQuery(MapSearch mapSearch);

    ServiceMultiResult<HouseDTO> boundMapQuery(MapSearch mapSearch);
}
