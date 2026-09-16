package com.qinghuan.catalog;

public interface GeocodingService {

    /** 将地点名称转换为经纬度，city 可为空。 */
    GeoCoordinate geocode(String placeName, String city);
}
