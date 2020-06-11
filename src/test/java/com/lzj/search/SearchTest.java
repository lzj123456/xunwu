package com.lzj.search;

import com.lzj.search.base.BaiduMapLocation;
import com.lzj.search.base.ServiceMultiResult;
import com.lzj.search.base.ServiceResult;
import com.lzj.search.document.HouseIndex;
import com.lzj.search.form.RentSearch;
import com.lzj.search.service.SearchService;
import com.lzj.search.service.SupportAddressService;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.indices.AnalyzeRequest;
import org.elasticsearch.client.indices.AnalyzeResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.util.Assert;

/**
 * @author lizijian
 */
public class SearchTest extends SearchApplicationTest {

    @Autowired
    private SearchService searchService;

    @Autowired
    private SupportAddressService supportAddressService;

    @Autowired
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    @Test
    public void test() {
        BaiduMapLocation baiduMapLocation = new BaiduMapLocation();
        baiduMapLocation.setLatitude(40.111);
        baiduMapLocation.setLongitude(116.11);

        ServiceResult serviceResult = supportAddressService.lbsUpload(baiduMapLocation, "住宅区", "西二旗东路", 1L, 10, 20);

    }
}
