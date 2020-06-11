package com.lzj.search.service.impl;

import com.lzj.search.base.*;
import com.lzj.search.document.HouseIndex;
import com.lzj.search.dto.HouseBucketDTO;
import com.lzj.search.entity.House;
import com.lzj.search.entity.HouseDetail;
import com.lzj.search.entity.HouseTag;
import com.lzj.search.entity.SupportAddress;
import com.lzj.search.form.MapSearch;
import com.lzj.search.form.RentSearch;
import com.lzj.search.repository.HouseDetailRepository;
import com.lzj.search.repository.HouseRepository;
import com.lzj.search.repository.HouseTagRepository;
import com.lzj.search.repository.SupportAddressRepository;
import com.lzj.search.repository.search.SearchRepository;
import com.lzj.search.service.SearchService;
import com.lzj.search.service.SupportAddressService;
import com.lzj.search.util.CommonUtil;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.indices.AnalyzeRequest;
import org.elasticsearch.client.indices.AnalyzeResponse;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.suggest.Suggest;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.SuggestBuilders;
import org.elasticsearch.search.suggest.completion.CompletionSuggestion;
import org.elasticsearch.search.suggest.completion.CompletionSuggestionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * @author lizijian
 */
@Service
public class SearchServiceImpl implements SearchService {

    @Autowired
    private SearchRepository searchRepository;

    @Autowired
    private HouseRepository houseRepository;

    @Autowired
    private HouseDetailRepository houseDetailRepository;

    @Autowired
    private HouseTagRepository houseTagRepository;

    @Autowired
    private SupportAddressRepository supportAddressRepository;

    @Autowired
    private SupportAddressService supportAddressService;

    @Autowired
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    @Override
    public void index(Long houseId) {
        House house = houseRepository.findById(houseId).get();
        HouseIndex houseIndex = CommonUtil.map(house, HouseIndex.class);
        List<HouseTag> tags = houseTagRepository.findAllByHouseId(houseId);
        List<String> houseTags = new ArrayList<>();
        tags.forEach(houseTag -> houseTags.add(houseTag.getName()));
        houseIndex.setTags(houseTags);
        houseIndex.setHouseId(houseId);

        HouseDetail detail = houseDetailRepository.findByHouseId(houseId);

        SupportAddress city = supportAddressRepository.findByEnNameAndLevel(house.getCityEnName(), SupportAddress.Level.CITY.getValue());

        SupportAddress region = supportAddressRepository.findByEnNameAndLevel(house.getRegionEnName(), SupportAddress.Level.REGION.getValue());

        String address = city.getCnName() + region.getCnName() + house.getStreet() + house.getDistrict() + detail.getDetailAddress();
        ServiceResult<BaiduMapLocation> baiduMapLocation = supportAddressService.findBaiduMapLocation(city.getCnName(), address);
        houseIndex.setLocation(baiduMapLocation.getResult());
        List<HouseIndex> HouseIndexList = searchRepository.findByHouseId(houseId);
        int size = HouseIndexList.size();
        if (size == 0) {
            create(houseIndex);
        } else if (size == 1) {
            update(houseIndex);
        } else {
            deleteAndCreat(houseIndex);
        }

        ServiceResult serviceResult = supportAddressService.lbsUpload(baiduMapLocation.getResult(), house.getStreet() + house.getDistrict(),
                city.getCnName() + region.getCnName() + house.getStreet() + house.getDistrict(),
                houseIndex.getHouseId(), house.getPrice(), house.getArea());
    }

    @Override
    public void remove(Long houseId) {
        searchRepository.deleteById(houseId);
        supportAddressService.removeLbs(houseId);
    }

    @Override
    public ServiceMultiResult<Long> query(RentSearch rentSearch) {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        if (!StringUtils.isEmpty(rentSearch.getCityEnName())) {
            boolQuery.filter(
                    QueryBuilders.termQuery("cityEnName", rentSearch.getCityEnName())
            );
        }
        if (!StringUtils.isEmpty(rentSearch.getRegionEnName())) {
            boolQuery.filter(
                    QueryBuilders.termQuery("regionEnName", rentSearch.getRegionEnName())
            );
        }

        RentValueBlock area = RentValueBlock.matchArea(rentSearch.getAreaBlock());
        if (!RentValueBlock.ALL.equals(area)) {
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("area");
            rangeBuild(area, rangeQuery);
            boolQuery.filter(rangeQuery);
        }

        RentValueBlock price = RentValueBlock.matchPrice(rentSearch.getPriceBlock());
        if (!RentValueBlock.ALL.equals(price)) {
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("price");
            rangeBuild(price, rangeQuery);
            boolQuery.filter(rangeQuery);
        }

        boolQuery.must(
                QueryBuilders.multiMatchQuery(rentSearch.getKeywords(),
                        "title",
                        "district",
                        "street"
                )
        );

        NativeSearchQueryBuilder nativeSearchQueryBuilder = new NativeSearchQueryBuilder();
        nativeSearchQueryBuilder.withQuery(boolQuery);
        nativeSearchQueryBuilder.withPageable(PageRequest.of(rentSearch.getStart(), rentSearch.getSize(),
                Sort.by(Sort.Direction.DESC, rentSearch.getOrderBy())));

        SearchHits<HouseIndex> searchHits = elasticsearchRestTemplate.search(nativeSearchQueryBuilder.build(), HouseIndex.class);
        List<Long> houseIds = new ArrayList<>();
        searchHits.forEach(houseIndexSearchHit -> houseIds.add(houseIndexSearchHit.getContent().getHouseId()));

        return new ServiceMultiResult<>(searchHits.getTotalHits(), houseIds);
    }

    @Override
    public ServiceMultiResult<String> suggest(String prefix) {
        CompletionSuggestionBuilder suggestion = SuggestBuilders.completionSuggestion("suggest")
                .prefix(prefix).size(5);
        SuggestBuilder suggestBuilder = new SuggestBuilder();
        suggestBuilder.addSuggestion("autocompletion", suggestion);
        SearchResponse response = elasticsearchRestTemplate.suggest(suggestBuilder, IndexCoordinates.of("xunwu"));
        Suggest suggest = response.getSuggest();
        Suggest.Suggestion result = suggest.getSuggestion("autocompletion");
        Set<String> suggestSet = new HashSet<>();
        int maxSuggest = 0;
        for (Object entry : result.getEntries()) {
            if (entry instanceof CompletionSuggestion.Entry) {
                CompletionSuggestion.Entry item = (CompletionSuggestion.Entry) entry;
                if (item.getOptions().isEmpty()) {
                    continue;
                }

                for (CompletionSuggestion.Entry.Option option : item.getOptions()) {
                    String tip = option.getText().string();
                    if (suggestSet.contains(tip)) {
                        continue;
                    }
                    suggestSet.add(tip);
                    maxSuggest++;
                }
            }
            if (maxSuggest > 5) {
                break;
            }
        }
        List<String> list = new ArrayList(Arrays.asList(suggestSet.toArray()));
        ServiceMultiResult<String> stringServiceMultiResult = new ServiceMultiResult<>(suggestSet.size(), list);
        return stringServiceMultiResult;
    }

    @Override
    public ServiceResult<Long> aggregateDistrictHouse(String cityEnName, String regionEnName, String district) {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery()
                .filter(QueryBuilders.termQuery("cityEnName", cityEnName))
                .filter(QueryBuilders.termQuery("regionEnName", regionEnName))
                .filter(QueryBuilders.termQuery("district", district));
        NativeSearchQueryBuilder nativeSearchQueryBuilder = new NativeSearchQueryBuilder();
        nativeSearchQueryBuilder.withQuery(boolQuery);
        nativeSearchQueryBuilder.addAggregation(AggregationBuilders.terms("AGG_DISTRICT").field("district").size(1));

        SearchHits<HouseIndex> search = elasticsearchRestTemplate.search(nativeSearchQueryBuilder.build(), HouseIndex.class);
        Aggregation agg = search.getAggregations().get("AGG_DISTRICT");
        Terms terms = (Terms) agg;
        Long l = terms.getBuckets().get(0).getDocCount();
        return new ServiceResult<>(true, "ok", l);
    }

    @Override
    public ServiceMultiResult<HouseBucketDTO> mapAggregate(String cityEnName) {
        BoolQueryBuilder boolQuery= QueryBuilders.boolQuery().filter(
                QueryBuilders.termQuery("cityEnName", cityEnName)
        );

        NativeSearchQueryBuilder nativeSearchQueryBuilder = new NativeSearchQueryBuilder()
                .withQuery(boolQuery)
                .addAggregation(AggregationBuilders.terms("mapAggregate").field("regionEnName"));
        SearchHits<HouseIndex> search = elasticsearchRestTemplate.search(nativeSearchQueryBuilder.build(), HouseIndex.class);
        Terms terms = search.getAggregations().get("mapAggregate");

        List<HouseBucketDTO> bucketDTOS = new ArrayList<>();
        for (Terms.Bucket bucket : terms.getBuckets()) {
            bucketDTOS.add(new HouseBucketDTO(bucket.getKeyAsString(), bucket.getDocCount()));
        }

        return new ServiceMultiResult<>(search.getTotalHits(), bucketDTOS);
    }

    @Override
    public ServiceMultiResult<Long> mapQuery(String cityEnName, String orderBy, String orderDirection, Integer start, Integer size) {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        boolQuery.filter(QueryBuilders.termQuery("cityEnName", cityEnName));
        Sort.Direction direction = "desc".equals(orderDirection) ? Sort.Direction.DESC : Sort.Direction.ASC;

        NativeSearchQueryBuilder nativeSearchQueryBuilder = new NativeSearchQueryBuilder()
                .withQuery(boolQuery)
                .withPageable(PageRequest.of(start / size, size, Sort.by(direction, orderBy)));

        SearchHits<HouseIndex> search = elasticsearchRestTemplate.search(nativeSearchQueryBuilder.build(), HouseIndex.class);

        List<Long> houseIds = new ArrayList<>();
        for (SearchHit<HouseIndex> searchHit : search.getSearchHits()) {
            houseIds.add(searchHit.getContent().getHouseId());
        }
        return new ServiceMultiResult<>(search.getTotalHits(), houseIds);
    }

    @Override
    public ServiceMultiResult<Long> mapQuery(MapSearch mapSearch) {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        boolQuery.filter(QueryBuilders.termQuery("cityEnName", mapSearch.getCityEnName()));
        boolQuery.filter(
                QueryBuilders.geoBoundingBoxQuery("location")
                .setCorners(
                    new GeoPoint(mapSearch.getLeftLatitude(), mapSearch.getLeftLongitude()),
                    new GeoPoint(mapSearch.getRightLatitude(), mapSearch.getRightLongitude())
                )
        );

        Sort.Direction direction = "desc".equals(mapSearch.getOrderDirection()) ? Sort.Direction.DESC : Sort.Direction.ASC;

        NativeSearchQueryBuilder nativeSearchQueryBuilder = new NativeSearchQueryBuilder()
                .withQuery(boolQuery)
                .withPageable(PageRequest.of(mapSearch.getStart() / mapSearch.getSize(), mapSearch.getSize(),
                        Sort.by(direction, mapSearch.getOrderBy())));

        SearchHits<HouseIndex> search = elasticsearchRestTemplate.search(nativeSearchQueryBuilder.build(), HouseIndex.class);
        List<Long> houseIds = new ArrayList<>();
        for (SearchHit<HouseIndex> hit : search.getSearchHits()) {
            houseIds.add(hit.getContent().getHouseId());
        }
        return new ServiceMultiResult<>(search.getTotalHits(), houseIds);
    }

    private void updateSuggest(HouseIndex houseIndex) {
        AnalyzeRequest request = AnalyzeRequest.withGlobalAnalyzer("ik_smart", houseIndex.getTitle());
        AnalyzeResponse response = elasticsearchRestTemplate.execute(client -> client.indices().analyze(request, RequestOptions.DEFAULT));
        List<AnalyzeResponse.AnalyzeToken> tokens = response.getTokens();
        List<HouseSuggest> houseSuggestList = new ArrayList<>();
        for (AnalyzeResponse.AnalyzeToken token : tokens) {
            if ("<NUM>".equals(token.getType()) || token.getTerm().length() < 2) {
                continue;
            }
            HouseSuggest houseSuggest = new HouseSuggest();
            houseSuggest.setInput(token.getTerm());
            houseSuggestList.add(houseSuggest);
        }

        HouseSuggest houseSuggest = new HouseSuggest();
        houseSuggest.setInput(houseIndex.getDistrict());
        houseSuggestList.add(houseSuggest);

        houseIndex.setSuggest(houseSuggestList);
    }

    private void rangeBuild(RentValueBlock range, RangeQueryBuilder rangeQuery) {
        if (range.getMax() > 0) {
            rangeQuery.lte(range.getMax());
        }
        if (range.getMin() > 0) {
            rangeQuery.gte(range.getMin());
        }
    }

    private void deleteAndCreat(HouseIndex houseIndex) {
        searchRepository.deleteById(houseIndex.getHouseId());
        searchRepository.save(houseIndex);
    }

    private void update(HouseIndex houseIndex) {
        updateSuggest(houseIndex);
        searchRepository.save(houseIndex);
    }

    private void create(HouseIndex houseIndex) {
        updateSuggest(houseIndex);
        searchRepository.save(houseIndex);
    }
}
