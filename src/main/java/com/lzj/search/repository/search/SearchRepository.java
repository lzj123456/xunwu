package com.lzj.search.repository.search;

import com.lzj.search.document.HouseIndex;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

/**
 * @author lizijian
 */
public interface SearchRepository extends ElasticsearchRepository<HouseIndex, Long> {

    List<HouseIndex> findByHouseId(Long houseId);
}
