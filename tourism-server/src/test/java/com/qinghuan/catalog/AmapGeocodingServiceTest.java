package com.qinghuan.catalog;

import com.qinghuan.config.map.AmapProperties;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.client.RestClient;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AmapGeocodingServiceTest {

    private HttpServer server;
    private StringRedisTemplate stringRedisTemplate;
    private ValueOperations<String, String> valueOperations;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        stringRedisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void geocode_shouldReadCoordinateFromAmapResponse() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v3/geocode/geo", exchange -> {
            byte[] body = """
                    {
                      "status":"1",
                      "info":"OK",
                      "count":"1",
                      "geocodes":[{
                        "formatted_address":"广东省广州市广州塔",
                        "location":"113.324500,23.106700"
                      }]
                    }
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add(
                    "Content-Type", "application/json;charset=UTF-8");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();

        AmapProperties properties = new AmapProperties();
        properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        properties.setKey("test-key");
        AmapGeocodingService service = new AmapGeocodingService(
                RestClient.builder(), properties, stringRedisTemplate);

        GeoCoordinate coordinate = service.geocode("广州塔", "广州");

        assertEquals(113.3245, coordinate.longitude());
        assertEquals(23.1067, coordinate.latitude());
        verify(valueOperations).set(
                "geo:geocode:广州:广州塔",
                "113.3245,23.1067",
                7L,
                TimeUnit.DAYS);
    }

    @Test
    void geocode_shouldReturnCachedCoordinateWithoutCallingAmap() {
        AmapProperties properties = new AmapProperties();
        // 不启动 HTTP Server；若缓存没有生效，本测试会因为连接失败而报错。
        properties.setBaseUrl("http://127.0.0.1:1");
        properties.setKey("test-key");
        when(valueOperations.get("geo:geocode:广州:广州塔"))
                .thenReturn("113.3245,23.1067");
        AmapGeocodingService service = new AmapGeocodingService(
                RestClient.builder(), properties, stringRedisTemplate);

        GeoCoordinate coordinate = service.geocode(" 广州塔 ", " 广州 ");

        assertEquals(113.3245, coordinate.longitude());
        assertEquals(23.1067, coordinate.latitude());
    }
}
