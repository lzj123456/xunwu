package com.lzj.search.base;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.elasticsearch.annotations.Field;

/**
 * 百度位置信息
 * Created by lizijian.
 */
public class BaiduMapLocation {
    // 经度，ES中强制规定geo_point类型字段里的数据结构是这样的，经纬度名字是lon与lat
    @Field(name = "lon")
    private double longitude;

    // 纬度
    @Field(name = "lat")
    private double latitude;

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }
}
