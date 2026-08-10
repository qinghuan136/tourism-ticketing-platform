package com.qinghuan.catalog;

import com.qinghuan.common.exception.BusinessException;
import com.qinghuan.common.exception.ErrorCode;
import com.qinghuan.config.map.AmapProperties;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** 调用高德地理编码 Web 服务，将地点名称转换为坐标。 */
@Service
public class AmapGeocodingService implements GeocodingService {

    private static final String CACHE_KEY_PREFIX = "geo:geocode:";
    private static final long CACHE_TTL_DAYS = 7L;

    private final RestClient restClient;
    private final AmapProperties properties;
    private final StringRedisTemplate stringRedisTemplate;

    public AmapGeocodingService(
            RestClient.Builder builder,
            AmapProperties properties,
            StringRedisTemplate stringRedisTemplate) {
        this.properties = properties;
        this.stringRedisTemplate = stringRedisTemplate;

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(3));

        this.restClient = builder.clone()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public GeoCoordinate geocode(String placeName, String city) {
        String normalizedPlaceName = placeName.trim();
        String normalizedCity = StringUtils.hasText(city) ? city.trim() : "";
        String cacheKey = CACHE_KEY_PREFIX
                + normalizedCity + ":" + normalizedPlaceName;

        // 地名解析结果变化很少，命中 Redis 时不再消耗高德接口调用额度。
        String cachedCoordinate = stringRedisTemplate.opsForValue().get(cacheKey);
        if (StringUtils.hasText(cachedCoordinate)) {
            String[] values = cachedCoordinate.split(",", 2);
            return new GeoCoordinate(
                    Double.parseDouble(values[0]),
                    Double.parseDouble(values[1]));
        }

        AmapGeocodeResponse response = restClient.get()
                .uri(uriBuilder -> buildUri(
                        uriBuilder,
                        normalizedPlaceName,
                        normalizedCity))
                .retrieve()
                .requiredBody(AmapGeocodeResponse.class);

        /*
         * 高德的业务失败通常也会返回 HTTP 200，
         * 因此还需要检查响应中的 status 和 geocodes。
         */
        if (!"1".equals(response.status())
                || response.geocodes() == null
                || response.geocodes().isEmpty()) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "地点解析失败：" + response.info());
        }

        // 高德 location 格式固定为“经度,纬度”。
        String[] location = response.geocodes()
                .get(0)
                .location()
                .split(",", 2);

        GeoCoordinate coordinate = new GeoCoordinate(
                Double.parseDouble(location[0]),
                Double.parseDouble(location[1]));

        // 只缓存成功结果，解析失败仍允许后续请求重新调用地图服务。
        stringRedisTemplate.opsForValue().set(
                cacheKey,
                coordinate.longitude() + "," + coordinate.latitude(),
                CACHE_TTL_DAYS,
                TimeUnit.DAYS);
        return coordinate;
    }

    private java.net.URI buildUri(
            UriBuilder uriBuilder,
            String placeName,
            String city) {
        uriBuilder.path("/v3/geocode/geo")
                .queryParam("key", properties.getKey())
                .queryParam("address", placeName);

        if (StringUtils.hasText(city)) {
            uriBuilder.queryParam("city", city);
        }
        return uriBuilder.build();
    }

    /** 只声明当前功能需要的高德响应字段，其余字段由 Jackson 忽略。 */
    private record AmapGeocodeResponse(
            String status,
            String info,
            List<AmapGeocode> geocodes) {
    }

    private record AmapGeocode(String location) {
    }
}
