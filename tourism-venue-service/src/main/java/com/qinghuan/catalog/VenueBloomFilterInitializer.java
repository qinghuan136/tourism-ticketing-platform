package com.qinghuan.catalog;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/** 应用启动后把数据库中的景点 ID 预热到 Redis 布隆过滤器。 */
@Slf4j
@Component
@ConditionalOnProperty(
        prefix = "app.venue-bloom",
        name = "preload-enabled",
        havingValue = "true",
        matchIfMissing = true)
public class VenueBloomFilterInitializer implements ApplicationRunner {

    private final CatalogMapper catalogMapper;
    private final VenueBloomFilter venueBloomFilter;

    public VenueBloomFilterInitializer(CatalogMapper catalogMapper,
                                       VenueBloomFilter venueBloomFilter) {
        this.catalogMapper = catalogMapper;
        this.venueBloomFilter = venueBloomFilter;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<Long> venueIds = catalogMapper.listAllVenueIds();
        venueBloomFilter.initialize(venueIds);
        log.info("景点布隆过滤器初始化完成，已加载 {} 个景点", venueIds.size());
    }
}
