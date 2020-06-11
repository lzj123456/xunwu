package com.lzj.search.service.impl;

import com.google.common.collect.Maps;
import com.lzj.search.base.HouseStatus;
import com.lzj.search.base.ServiceMultiResult;
import com.lzj.search.base.ServiceResult;
import com.lzj.search.dto.HouseDTO;
import com.lzj.search.dto.HouseDetailDTO;
import com.lzj.search.dto.HousePictureDTO;
import com.lzj.search.entity.*;
import com.lzj.search.form.*;
import com.lzj.search.repository.*;
import com.lzj.search.service.HouseService;
import com.lzj.search.service.QiNiuService;
import com.lzj.search.service.SearchService;
import com.lzj.search.util.CommonUtil;
import com.lzj.search.util.LoginUserUtil;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author lizijian
 */
@Service
public class HouseServiceImpl implements HouseService {

    @Autowired
    private HouseRepository houseRepository;

    @Autowired
    private SubwayStationRepository subwayStationRepository;

    @Autowired
    private SubwayRepository subwayRepository;

    @Autowired
    private HouseDetailRepository houseDetailRepository;

    @Autowired
    private HousePictureRepository housePictureRepository;

    @Autowired
    private HouseTagRepository houseTagRepository;

    @Autowired
    private QiNiuService qiNiuService;

    @Autowired
    private SearchService searchService;

    @Value("${qiniu.cdn.prefix}")
    private String qiniuCdnPrefix;

    @Override
    public ServiceResult<HouseDTO> save(HouseForm houseForm) {
        House house = CommonUtil.map(houseForm, House.class);
        Date now = new Date();
        house.setCreateTime(now);
        house.setLastUpdateTime(now);
        house.setAdminId(LoginUserUtil.getLoginUserId());
        house = houseRepository.save(house);

        HouseDetail houseDetail = new HouseDetail();
        houseDetail.setHouseId(house.getId());
        HouseDetailDTO houseDetailDTO = warpperSubwayInfo(houseDetail, houseForm);

        List<HousePicture> housePictureList = generatePicture(house.getId(), houseForm);
        housePictureRepository.saveAll(housePictureList);

        HouseDTO houseDTO = CommonUtil.map(house, HouseDTO.class);
        houseDTO.setHouseDetail(houseDetailDTO);
        List<HousePictureDTO> pictureDtoList = new ArrayList<>();
        housePictureList.forEach(housePicture -> pictureDtoList.add(CommonUtil.map(housePicture, HousePictureDTO.class)));
        houseDTO.setPictures(pictureDtoList);
        houseDTO.setCover(qiniuCdnPrefix + houseDTO.getCover());

        List<String> tags = houseForm.getTags();
        if (tags != null || !tags.isEmpty()) {
            List<HouseTag> houseTags = new ArrayList<>();
            for (String tag : tags) {
                houseTags.add(new HouseTag(house.getId(), tag));
            }
            houseTagRepository.saveAll(houseTags);
            houseDTO.setTags(tags);
        }

        return new ServiceResult<>(true, "ok", houseDTO);
    }

    @Override
    @Transactional
    public ServiceResult update(HouseForm houseForm) {
        House house = this.houseRepository.findById(houseForm.getId()).get();
        HouseDetail detail = this.houseDetailRepository.findById(house.getId()).get();

        ServiceResult wrapperResult = wrapperDetailInfo(detail, houseForm);

        houseDetailRepository.save(detail);

        List<HousePicture> pictures = generatePicture(houseForm.getId(), houseForm);
        if (pictures != null && pictures.size() > 0) {
            housePictureRepository.saveAll(pictures);
        }

        if (houseForm.getCover() == null) {
            houseForm.setCover(house.getCover());
        }

        House newHouse = CommonUtil.map(houseForm, House.class);
        newHouse.setAdminId(house.getAdminId());
        newHouse.setLastUpdateTime(new Date());
        newHouse.setCreateTime(house.getCreateTime());
        houseRepository.save(newHouse);

       if (house.getStatus() == HouseStatus.PASSES.getValue()) {
            searchService.index(house.getId());
        }

        return ServiceResult.success();
    }

    @Override
    public ServiceMultiResult<HouseDTO> adminQuery(DatatableSearch searchBody) {
        List<HouseDTO> houseDTOS = new ArrayList<>();

        Sort sort = Sort.by(Sort.Direction.fromString(searchBody.getDirection()), searchBody.getOrderBy());
        int page = searchBody.getStart() / searchBody.getLength();
        Pageable pageable = PageRequest.of(page, searchBody.getLength(), sort);

        Specification<House> specification = (root, query, cb) -> {
            Predicate predicate = cb.equal(root.get("adminId"), LoginUserUtil.getLoginUserId());

            predicate = cb.and(predicate, cb.notEqual(root.get("status"), HouseStatus.DELETED.getValue()));

            if (!StringUtils.isEmpty(searchBody.getCity())) {
                predicate = cb.and(predicate, cb.equal(root.get("cityEnName"), searchBody.getCity()));
            }

            if (searchBody.getStatus() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("status"), searchBody.getStatus()));
            }

            if (searchBody.getCreateTimeMin() != null) {
                predicate = cb.and(predicate, cb.greaterThanOrEqualTo(root.get("createTime"), searchBody.getCreateTimeMin()));
            }

            if (searchBody.getCreateTimeMax() != null) {
                predicate = cb.and(predicate, cb.lessThanOrEqualTo(root.get("createTime"), searchBody.getCreateTimeMax()));
            }

            if (searchBody.getTitle() != null) {
                predicate = cb.and(predicate, cb.like(root.get("title"), "%" + searchBody.getTitle() + "%"));
            }

            return predicate;
        };

        Page<House> housePage = houseRepository.findAll(specification, pageable);
        housePage.forEach(house -> {
            HouseDTO houseDTO = CommonUtil.map(house, HouseDTO.class);
            houseDTO.setCover(qiniuCdnPrefix + house.getCover());
            houseDTOS.add(houseDTO);
        });

        return new ServiceMultiResult<>(housePage.getTotalElements(), houseDTOS);
    }

    @Override
    public ServiceResult<HouseDTO> findCompleteOne(Long id) {
        House house = houseRepository.findById(id).get();

        HouseDetail detail = houseDetailRepository.findByHouseId(id);
        List<HousePicture> pictures = housePictureRepository.findAllByHouseId(id);

        HouseDetailDTO detailDTO = CommonUtil.map(detail, HouseDetailDTO.class);
        List<HousePictureDTO> pictureDTOS = new ArrayList<>();
        for (HousePicture picture : pictures) {
            HousePictureDTO pictureDTO = CommonUtil.map(picture, HousePictureDTO.class);
            pictureDTOS.add(pictureDTO);
        }


        List<HouseTag> tags = houseTagRepository.findAllByHouseId(id);
        List<String> tagList = new ArrayList<>();
        for (HouseTag tag : tags) {
            tagList.add(tag.getName());
        }

        HouseDTO result = CommonUtil.map(house, HouseDTO.class);
        result.setHouseDetail(detailDTO);
        result.setPictures(pictureDTOS);
        result.setTags(tagList);

/*        if (LoginUserUtil.getLoginUserId() > 0) { // 已登录用户
            HouseSubscribe subscribe = subscribeRespository.findByHouseIdAndUserId(house.getId(), LoginUserUtil.getLoginUserId());
            if (subscribe != null) {
                result.setSubscribeStatus(subscribe.getStatus());
            }
        }*/

        return ServiceResult.of(result);
    }

    @Override
    public ServiceResult removePhoto(Long id) {
        HousePicture picture = housePictureRepository.findById(id).get();

        try {
            Response response = this.qiNiuService.deleteFile(picture.getPath());
            if (response.isOK()) {
                housePictureRepository.deleteById(id);
                return ServiceResult.success();
            } else {
                return new ServiceResult(false, response.error);
            }
        } catch (QiniuException e) {
            e.printStackTrace();
            return new ServiceResult(false, e.getMessage());
        }
    }

    @Override
    @Transactional
    public ServiceResult updateCover(Long coverId, Long targetId) {
        HousePicture cover = housePictureRepository.findById(coverId).get();
        houseRepository.updateCover(targetId, cover.getPath());
        return ServiceResult.success();
    }

    @Override
    @Transactional
    public ServiceResult addTag(Long houseId, String tag) {
        House house = houseRepository.findById(houseId).get();

        HouseTag houseTag = houseTagRepository.findByNameAndHouseId(tag, houseId);
        if (houseTag != null) {
            return new ServiceResult(false, "标签已存在");
        }

        houseTagRepository.save(new HouseTag(houseId, tag));
        return ServiceResult.success();
    }

    @Override
    @Transactional
    public ServiceResult removeTag(Long houseId, String tag) {
        House house = houseRepository.findById(houseId).get();

        HouseTag houseTag = houseTagRepository.findByNameAndHouseId(tag, houseId);
        if (houseTag == null) {
            return new ServiceResult(false, "标签不存在");
        }

        houseTagRepository.deleteById(houseTag.getId());
        return ServiceResult.success();
    }

    @Override
    @Transactional
    public ServiceResult updateStatus(Long id, int status) {
        House house = houseRepository.findById(id).get();

        if (house.getStatus() == status) {
            return new ServiceResult(false, "状态没有发生变化");
        }

        if (house.getStatus() == HouseStatus.RENTED.getValue()) {
            return new ServiceResult(false, "已出租的房源不允许修改状态");
        }

        if (house.getStatus() == HouseStatus.DELETED.getValue()) {
            return new ServiceResult(false, "已删除的资源不允许操作");
        }

        houseRepository.updateStatus(id, status);

        // 上架更新索引 其他情况都要删除索引
        if (status == HouseStatus.PASSES.getValue()) {
            searchService.index(id);
        } else {
            searchService.remove(id);
        }
        return ServiceResult.success();
    }

    @Override
    public ServiceMultiResult<HouseDTO> query(RentSearch rentSearch) {
        if (!StringUtils.isEmpty(rentSearch.getKeywords())) {
            if ("*".equals(rentSearch.getRegionEnName())) {
                rentSearch.setRegionEnName(null);
            }
            ServiceMultiResult<Long> serviceMultiResult = searchService.query(rentSearch);
            if (serviceMultiResult.getTotal() == 0) {
                return new ServiceMultiResult<>(0, new ArrayList<>());
            }
            return new ServiceMultiResult<>(serviceMultiResult.getTotal(), wrapperHouseResult(serviceMultiResult));
        }

        return simpleQuery(rentSearch);
    }

    @Override
    public ServiceMultiResult<HouseDTO> wholeMapQuery(MapSearch mapSearch) {
        ServiceMultiResult<Long> houseIds = searchService.mapQuery(mapSearch.getCityEnName(), mapSearch.getOrderBy(), mapSearch.getOrderDirection(),
                mapSearch.getStart(), mapSearch.getSize());

        if (houseIds.getTotal() == 0) {
            return new ServiceMultiResult<>(0, null);
        }

        List<HouseDTO> houseDTOS = wrapperHouseResult(houseIds);
        return new ServiceMultiResult<>(houseIds.getTotal(), houseDTOS);
    }

    @Override
    public ServiceMultiResult<HouseDTO> boundMapQuery(MapSearch mapSearch) {
        ServiceMultiResult<Long> houseIds = searchService.mapQuery(mapSearch);
        if (houseIds.getTotal() == 0) {
            return new ServiceMultiResult<>(0, new ArrayList<>());
        }

        List<HouseDTO> houseDTOS = wrapperHouseResult(houseIds);
        return new ServiceMultiResult<>(houseIds.getTotal(), houseDTOS);
    }


    private ServiceMultiResult<HouseDTO> simpleQuery(RentSearch rentSearch) {
        Sort sort = Sort.by(Sort.Direction.DESC, "lastUpdateTime");
        int page = rentSearch.getStart() / rentSearch.getSize();
        Pageable pageable = PageRequest.of(page, rentSearch.getSize(), sort);

        Specification<House> specification = (root, criteriaQuery, criteriaBuilder) -> {
            Predicate predicate = criteriaBuilder.equal(root.get("status"), HouseStatus.PASSES.getValue());
            predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("cityEnName"), rentSearch.getCityEnName()));
            return predicate;
        };
        Page<House> housePage = houseRepository.findAll(specification, pageable);
        List<HouseDTO> houseDTOS = new ArrayList<>();
        housePage.forEach(house -> {
            HouseDTO map = CommonUtil.map(house, HouseDTO.class);
            map.setCover(this.qiniuCdnPrefix + house.getCover());
            houseDTOS.add(map);
        });
        return new ServiceMultiResult<>(houseDTOS.size(), wrapperHouse(houseDTOS));
    }

    private List<HouseDTO> wrapperHouseResult(ServiceMultiResult<Long> serviceMultiResult) {
        List<HouseDTO> res = new ArrayList<>();
        houseRepository.findAllById(serviceMultiResult.getResult())
                .forEach(house -> {
                    HouseDTO houseDTO = CommonUtil.map(house, HouseDTO.class);
                    houseDTO.setCover(qiniuCdnPrefix + house.getCover());
                    res.add(houseDTO);
                });
        wrapperHouse(res);
        return res;
    }

    private List<HouseDTO> wrapperHouse(List<HouseDTO> houseDTOS) {
        for (HouseDTO houseDTO : houseDTOS) {
            HouseDetail houseDetail = houseDetailRepository.findByHouseId(houseDTO.getId());
            houseDTO.setHouseDetail(CommonUtil.map(houseDetail, HouseDetailDTO.class));
        }
        return houseDTOS;
    }


    private HouseDetailDTO warpperSubwayInfo(HouseDetail houseDetail, HouseForm houseForm) {
        Subway subway = subwayRepository.findById(houseForm.getSubwayLineId()).get();
        SubwayStation subwayStation = subwayStationRepository.findById(houseForm.getSubwayStationId()).get();

        houseDetail.setSubwayLineId(subway.getId());
        houseDetail.setSubwayLineName(subway.getName());

        houseDetail.setSubwayStationId(subwayStation.getId());
        houseDetail.setSubwayStationName(subwayStation.getName());

        houseDetail.setDescription(houseForm.getDescription());
        houseDetail.setDetailAddress(houseForm.getDetailAddress());
        houseDetail.setLayoutDesc(houseForm.getLayoutDesc());
        houseDetail.setRentWay(houseForm.getRentWay());
        houseDetail.setRoundService(houseForm.getRoundService());
        houseDetail.setTraffic(houseForm.getTraffic());

        houseDetail = houseDetailRepository.save(houseDetail);
        return CommonUtil.map(houseDetail, HouseDetailDTO.class);
    }

    private List<HousePicture> generatePicture(Long houseId, HouseForm houseForm) {
        List<HousePicture> housePictureList = new ArrayList<>();
        if (houseForm.getPhotos() == null) return null;
        for (PhotoForm photo : houseForm.getPhotos()) {
            HousePicture housePicture = new HousePicture();
            housePicture.setHouseId(houseId);
            housePicture.setHeight(photo.getHeight());
            housePicture.setWidth(photo.getWidth());
            housePicture.setPath(photo.getPath());
            housePicture.setCdnPrefix(qiniuCdnPrefix);
            housePictureList.add(housePicture);
        }

        return housePictureList;
    }

    /**
     * 房源详细信息对象填充
     * @param houseDetail
     * @param houseForm
     * @return
     */
    private ServiceResult<HouseDTO> wrapperDetailInfo(HouseDetail houseDetail, HouseForm houseForm) {
        Subway subway = subwayRepository.findById(houseForm.getSubwayLineId()).get();
        SubwayStation subwayStation = subwayStationRepository.findById(houseForm.getSubwayStationId()).get();

        houseDetail.setSubwayLineId(subway.getId());
        houseDetail.setSubwayLineName(subway.getName());

        houseDetail.setSubwayStationId(subwayStation.getId());
        houseDetail.setSubwayStationName(subwayStation.getName());

        houseDetail.setDescription(houseForm.getDescription());
        houseDetail.setDetailAddress(houseForm.getDetailAddress());
        houseDetail.setLayoutDesc(houseForm.getLayoutDesc());
        houseDetail.setRentWay(houseForm.getRentWay());
        houseDetail.setRoundService(houseForm.getRoundService());
        houseDetail.setTraffic(houseForm.getTraffic());
        return null;

    }
}
