package com.qinghuan.config.canal;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Canal Client 连接参数。
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.canal")
public class CanalProperties {

    private boolean enabled = false;

    private String host = "127.0.0.1";

    private int port = 11111;

    private String destination = "example";

    private String username = "";

    private String password = "";

    private String database = "tourism_ticketing_platform";

    private String subscribe =
            "tourism_ticketing_platform\\." +
                    "(venue|admission_session|ticket_type|session_ticket_type)";

    private int batchSize = 100;
}
