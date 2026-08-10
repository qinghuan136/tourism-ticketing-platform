package com.qinghuan.venue;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** 应用启动后预热景点 GEO 索引。 */
@Component
@ConditionalOnProperty(
        prefix = "app.venue-geo",
        name = "preload-enabled",
        havingValue = "true",
        matchIfMissing = true)
public class VenueGeoInitializer implements ApplicationRunner {

    private final VenueGeoService venueGeoService;

    public VenueGeoInitializer(VenueGeoService venueGeoService) {
        this.venueGeoService = venueGeoService;
    }

    @Override
    public void run(ApplicationArguments args) {
        venueGeoService.rebuild();
    }
}
