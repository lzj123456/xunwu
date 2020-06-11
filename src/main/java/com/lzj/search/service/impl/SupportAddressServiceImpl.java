package com.lzj.search.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Multimap;
import com.lzj.search.base.BaiduMapLocation;
import com.lzj.search.base.ServiceMultiResult;
import com.lzj.search.base.ServiceResult;
import com.lzj.search.dto.SubwayDTO;
import com.lzj.search.dto.SubwayStationDTO;
import com.lzj.search.dto.SupportAddressDTO;
import com.lzj.search.entity.Subway;
import com.lzj.search.entity.SubwayStation;
import com.lzj.search.entity.SupportAddress;
import com.lzj.search.repository.SubwayRepository;
import com.lzj.search.repository.SubwayStationRepository;
import com.lzj.search.repository.SupportAddressRepository;
import com.lzj.search.service.SupportAddressService;
import com.lzj.search.util.CommonUtil;
import org.apache.http.entity.BasicHttpEntity;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author lizijian
 */
@Service
public class SupportAddressServiceImpl implements SupportAddressService {

    @Autowired
    private SupportAddressRepository addressRepository;

    @Autowired
    private SubwayRepository subwayRepository;

    @Autowired
    private SubwayStationRepository subwayStationRepository;

    @Autowired
    private RestTemplateBuilder restTemplateBuilder;

    private final static String BAIDU_KEY = "E7HLIpfwnnmRSNsxyMh4qv3zgWGjO9MB";

    private final static String BAIDU_LOCATION_API = "http://api.map.baidu.com/geocoding/v3/?";

    @Override
    public ServiceMultiResult<SupportAddressDTO> findAllCities() {
        List<SupportAddress> addresses = addressRepository.findAllByLevel(SupportAddress.Level.CITY.getValue());
        List<SupportAddressDTO> addressDTOS = new ArrayList<>();
        for (SupportAddress supportAddress : addresses) {
            SupportAddressDTO target = CommonUtil.map(supportAddress, SupportAddressDTO.class);
            addressDTOS.add(target);
        }

        return new ServiceMultiResult<>(addressDTOS.size(), addressDTOS);
    }

    @Override
    public Map<SupportAddress.Level, SupportAddressDTO> findCityAndRegion(String cityEnName, String regionEnName) {
        Map<SupportAddress.Level, SupportAddressDTO> result = new HashMap<>();

        SupportAddress city = addressRepository.findByEnNameAndLevel(cityEnName, SupportAddress.Level.CITY
                .getValue());
        SupportAddress region = addressRepository.findByEnNameAndBelongTo(regionEnName, city.getEnName());

        result.put(SupportAddress.Level.CITY, CommonUtil.map(city, SupportAddressDTO.class));
        result.put(SupportAddress.Level.REGION, CommonUtil.map(region, SupportAddressDTO.class));
        return result;
    }

    @Override
    public ServiceMultiResult<SupportAddressDTO> findAllRegionsByCityName(String cityName) {
        if (cityName == null) {
            return new ServiceMultiResult<>(0, null);
        }

        List<SupportAddressDTO> result = new ArrayList<>();

        List<SupportAddress> regions = addressRepository.findAllByLevelAndBelongTo(SupportAddress.Level.REGION
                .getValue(), cityName);
        for (SupportAddress region : regions) {
            result.add(CommonUtil.map(region, SupportAddressDTO.class));
        }
        return new ServiceMultiResult<>(regions.size(), result);
    }

    @Override
    public List<SubwayDTO> findAllSubwayByCity(String cityEnName) {
        List<SubwayDTO> result = new ArrayList<>();
        List<Subway> subways = subwayRepository.findAllByCityEnName(cityEnName);
        if (subways.isEmpty()) {
            return result;
        }

        subways.forEach(subway -> result.add(CommonUtil.map(subway, SubwayDTO.class)));
        return result;
    }

    @Override
    public List<SubwayStationDTO> findAllStationBySubway(Long subwayId) {
        List<SubwayStationDTO> result = new ArrayList<>();
        List<SubwayStation> stations = subwayStationRepository.findAllBySubwayId(subwayId);
        if (stations.isEmpty()) {
            return result;
        }

        stations.forEach(station -> result.add(CommonUtil.map(station, SubwayStationDTO.class)));
        return result;
    }

    @Override
    public ServiceResult<SubwayDTO> findSubway(Long subwayId) {
        if (subwayId == null) {
            return ServiceResult.notFound();
        }
        Subway subway = subwayRepository.findById(subwayId).get();
        return ServiceResult.of(CommonUtil.map(subway, SubwayDTO.class));
    }

    @Override
    public ServiceResult<SubwayStationDTO> findSubwayStation(Long stationId) {
        if (stationId == null) {
            return ServiceResult.notFound();
        }
        SubwayStation station = subwayStationRepository.findById(stationId).get();
        return ServiceResult.of(CommonUtil.map(station, SubwayStationDTO.class));
    }

    @Override
    public ServiceResult<SupportAddressDTO> findCity(String cityEnName) {
        if (cityEnName == null) {
            return ServiceResult.notFound();
        }

        SupportAddress supportAddress = addressRepository.findByEnNameAndLevel(cityEnName, SupportAddress.Level.CITY.getValue());
        if (supportAddress == null) {
            return ServiceResult.notFound();
        }

        SupportAddressDTO addressDTO = CommonUtil.map(supportAddress, SupportAddressDTO.class);
        return ServiceResult.of(addressDTO);
    }

    @Override
    public ServiceResult<BaiduMapLocation> findBaiduMapLocation(String city, String address) {
        String encodeAddress, encodeCity;
        try {
            encodeAddress = URLEncoder.encode(address, "UTF-8");
            encodeCity = URLEncoder.encode(city, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }

        StringBuilder url = new StringBuilder(BAIDU_LOCATION_API);
        url.append("address=").append(encodeAddress).append("&")
                .append("city=").append(encodeCity).append("&")
                .append("output=json&")
                .append("ak=").append(BAIDU_KEY);
        URI uri = null;
        try {
            uri = new URI(url.toString());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        RestTemplate restTemplate = restTemplateBuilder.build();
        String responseBody = restTemplate.getForObject(uri, String.class);
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            BaiduMapLocation location = new BaiduMapLocation();
            JsonNode jsonLocation = jsonNode.get("result").get("location");
            location.setLongitude(jsonLocation.get("lng").asDouble());
            location.setLatitude(jsonLocation.get("lat").asDouble());
            return ServiceResult.of(location);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public ServiceResult lbsUpload(BaiduMapLocation location, String title, String address, long houseId, int price, int area) {
        RestTemplate restTemplate = restTemplateBuilder.build();
        MultiValueMap<String, String> params = new LinkedMultiValueMap();
        params.add("latitude", String.valueOf(location.getLatitude()));
        params.add("longitude", String.valueOf(location.getLongitude()));
        params.add("coord_type", String.valueOf(3)); // 百度坐标系
        params.add("geotable_id", "212000");
        params.add("ak", BAIDU_KEY);
        params.add("houseId", String.valueOf(houseId));
        params.add("price", String.valueOf(price));
        params.add("area", String.valueOf(area));
        params.add("title", String.valueOf(title));
        params.add("address", address);
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity(params, httpHeaders);
        URI uri = null;
        try {
            uri = new URI("http://api.map.baidu.com/geodata/v3/poi/create");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return null;
        }
        String responseBody = restTemplate.postForObject(uri, requestEntity, String.class);
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            if (jsonNode.get("status").asInt() == 0) {
                return new ServiceResult(true);
            }
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public ServiceResult removeLbs(Long houseId) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("geotable_id", "212000");
        params.add("ak", BAIDU_KEY);
        params.add("houseId", String.valueOf(houseId));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> httpEntity = new HttpEntity<>(params, headers);
        URI uri = null;
        try {
             uri = new URI("http://api.map.baidu.com/geodata/v3/poi/delete");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return null;
        }

        RestTemplate restTemplate = restTemplateBuilder.build();
        String response = restTemplate.postForObject(uri, httpEntity, String.class);
        System.out.println(response);
        return null;
    }

}
