package com.lzj.search.service;

import com.lzj.search.base.ServiceMultiResult;
import com.lzj.search.base.ServiceResult;
import com.lzj.search.dto.HouseBucketDTO;
import com.lzj.search.form.MapSearch;
import com.lzj.search.form.RentSearch;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;

import java.util.List;

/**
 * @author lizijian
 */
public interface SearchService {

    void index(Long houseId);

    void remove(Long houseId);

    ServiceMultiResult<Long> query(RentSearch rentSearch);

    ServiceMultiResult<String> suggest(String prefix);

    /**
     * 聚合特定小区的房间数
     */
    ServiceResult<Long> aggregateDistrictHouse(String cityEnName, String regionEnName, String district);

    ServiceMultiResult<HouseBucketDTO> mapAggregate(String cityEnName);

    ServiceMultiResult<Long> mapQuery(String cityEnName, String orderBy, String orderDirection, Integer start, Integer size);

    /**
     * 精确范围搜索
     * @param mapSearch
     * @return
     */
    ServiceMultiResult<Long> mapQuery(MapSearch mapSearch);
}
