package com.qinghuan.canal;

import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.alibaba.otter.canal.protocol.Message;
import com.qinghuan.cache.DistributedCacheInvalidator;
import com.qinghuan.config.canal.CanalProperties;
import com.qinghuan.session.SessionService;
import com.qinghuan.venue.VenueGeoService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition
        .ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 消费 Canal 解析出的数据库行变更，并转换成缓存失效操作。
 *
 * 多实例部署时，只允许一个实例启用该组件。
 */
@Component
@ConditionalOnProperty(
        prefix = "app.canal",
        name = "enabled",
        havingValue = "true"
)
public class CanalCacheChangeConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(
                    CanalCacheChangeConsumer.class
            );

    /*
     * 只有这些字段变化时，才需要删除静态缓存。
     *
     * remaining_capacity 和 remaining_quantity 属于动态库存，
     * 订单扣减库存时不能不断清理静态缓存。
     */
    private static final Set<String> SESSION_STATIC_COLUMNS =
            Set.of(
                    "venue_id",
                    "visit_date",
                    "start_time",
                    "end_time",
                    "booking_start_at",
                    "booking_end_at",
                    "status"
            );

    private static final Set<String>
            SESSION_TICKET_TYPE_STATIC_COLUMNS =
            Set.of(
                    "session_id",
                    "ticket_type_id",
                    "sale_price",
                    "status"
            );

    private static final Set<String> TICKET_TYPE_STATIC_COLUMNS =
            Set.of(
                    "name",
                    "description",
                    "audience_rule",
                    "status"
            );

    private final CanalProperties properties;
    private final DistributedCacheInvalidator invalidator;
    private final SessionService sessionService;
    private final VenueGeoService venueGeoService;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor(r -> {
                Thread thread = new Thread(
                        r,
                        "canal-cache-consumer"
                );
                thread.setDaemon(true);
                return thread;
            });

    private volatile boolean running;

    public CanalCacheChangeConsumer(
            CanalProperties properties,
            DistributedCacheInvalidator invalidator,
            SessionService sessionService,
            VenueGeoService venueGeoService) {
        this.properties = properties;
        this.invalidator = invalidator;
        this.sessionService = sessionService;
        this.venueGeoService = venueGeoService;
    }

    /**
     * Spring Boot 启动完成后，使用单独线程消费 Canal。
     */
    @PostConstruct
    public void start() {
        running = true;
        executor.submit(this::consume);
    }

    @PreDestroy
    public void stop() {
        running = false;
        executor.shutdownNow();
    }

    private void consume() {
        while (running) {
            CanalConnector connector = createConnector();

            try {
                connector.connect();
                connector.subscribe(properties.getSubscribe());

                /*
                 * 回滚到 Canal Server 最后一次确认的位置，
                 * 避免从未确认的中间位置继续。
                 */
                connector.rollback();

                log.info(
                        "Canal Client 已连接，destination={}",
                        properties.getDestination()
                );

                consumeMessages(connector);
            } catch (Exception exception) {
                log.error("Canal 消费异常，稍后重新连接", exception);
                sleep(3000);
            } finally {
                try {
                    connector.disconnect();
                } catch (Exception ignored) {
                    // 连接可能已经断开，不需要额外处理。
                }
            }
        }
    }

    private CanalConnector createConnector() {
        return CanalConnectors.newSingleConnector(
                new InetSocketAddress(
                        properties.getHost(),
                        properties.getPort()
                ),
                properties.getDestination(),
                properties.getUsername(),
                properties.getPassword()
        );
    }

    private void consumeMessages(
            CanalConnector connector) {

        while (running) {
            /*
             * 不自动确认消息。
             * 必须等 Redis 删除和 Pub/Sub 发布成功后再 ack。
             */
            Message message = connector.getWithoutAck(
                    properties.getBatchSize()
            );

            long batchId = message.getId();
            List<CanalEntry.Entry> entries =
                    message.getEntries();

            if (batchId == -1 || entries.isEmpty()) {
                sleep(500);
                continue;
            }

            try {
                for (CanalEntry.Entry entry : entries) {
                    handleEntry(entry);
                }

                /*
                 * 所有缓存失效操作都成功后，
                 * 才确认这一批 binlog。
                 */
                connector.ack(batchId);
            } catch (Exception exception) {
                /*
                 * 处理失败时回滚批次。
                 * 下一轮会重新消费，缓存删除本身是幂等的。
                 */
                connector.rollback(batchId);

                log.error(
                        "Canal 批次处理失败，batchId={}",
                        batchId,
                        exception
                );

                sleep(1000);
            }
        }
    }

    private void handleEntry(
            CanalEntry.Entry entry) throws Exception {

        if (entry.getEntryType()
                != CanalEntry.EntryType.ROWDATA) {
            return;
        }

        if (!properties.getDatabase().equals(
                entry.getHeader().getSchemaName())) {
            return;
        }

        CanalEntry.RowChange rowChange =
                CanalEntry.RowChange.parseFrom(
                        entry.getStoreValue()
                );

        if (rowChange.getIsDdl()) {
            return;
        }

        String tableName =
                entry.getHeader().getTableName();

        CanalEntry.EventType eventType =
                rowChange.getEventType();

        for (CanalEntry.RowData rowData
                : rowChange.getRowDatasList()) {

            handleRow(
                    tableName,
                    eventType,
                    rowData
            );
        }
    }

    private void handleRow(
            String tableName,
            CanalEntry.EventType eventType,
            CanalEntry.RowData rowData) {

        switch (tableName) {
            case "venue" ->
                    handleVenue(eventType, rowData);

            case "admission_session" ->
                    handleSession(eventType, rowData);

            case "session_ticket_type" ->
                    handleSessionTicketType(
                            eventType,
                            rowData
                    );

            case "ticket_type" ->
                    handleTicketType(
                            eventType,
                            rowData
                    );

            default -> {
                // subscribe 已经限制了表，通常不会进入这里。
            }
        }
    }

    private void handleVenue(
            CanalEntry.EventType eventType,
            CanalEntry.RowData rowData) {

        Long venueId = getRowId(
                eventType,
                rowData
        );

        invalidator.invalidateVenueDetail(venueId);

        // Redis GEO 为共享索引，Canal 消费成功后同步一次即可供所有实例查询。
        venueGeoService.syncVenue(venueId);
    }

    private void handleSession(
            CanalEntry.EventType eventType,
            CanalEntry.RowData rowData) {

        if (!staticColumnsChanged(
                eventType,
                rowData,
                SESSION_STATIC_COLUMNS)) {
            return;
        }

        Long sessionId = getRowId(
                eventType,
                rowData
        );

        invalidator.invalidateSessionStatic(
                sessionId
        );
    }

    private void handleSessionTicketType(
            CanalEntry.EventType eventType,
            CanalEntry.RowData rowData) {

        if (!staticColumnsChanged(
                eventType,
                rowData,
                SESSION_TICKET_TYPE_STATIC_COLUMNS)) {
            return;
        }

        /*
         * INSERT 使用 after，DELETE 使用 before。
         * UPDATE 时同时获取新旧 session_id，
         * 即便关联关系发生移动，也能删除两边缓存。
         */
        Set<Long> sessionIds = new HashSet<>();

        sessionIds.add(
                getLong(
                        rowData.getBeforeColumnsList(),
                        "session_id"
                )
        );

        sessionIds.add(
                getLong(
                        rowData.getAfterColumnsList(),
                        "session_id"
                )
        );

        sessionIds.remove(null);

        sessionIds.forEach(
                invalidator::invalidateSessionStatic
        );
    }

    private void handleTicketType(
            CanalEntry.EventType eventType,
            CanalEntry.RowData rowData) {

        if (!staticColumnsChanged(
                eventType,
                rowData,
                TICKET_TYPE_STATIC_COLUMNS)) {
            return;
        }

        Long ticketTypeId = getRowId(
                eventType,
                rowData
        );

        /*
         * ticket_type 只包含 ticketTypeId，
         * 需要查询关联表获得受影响的 sessionId。
         */
        sessionService
                .listSessionIdsByTicketTypeId(ticketTypeId)
                .forEach(
                        invalidator::invalidateSessionStatic
                );
    }

    /**
     * UPDATE 时只关注真正修改过的静态字段。
     *
     * INSERT 和 DELETE 一定会影响查询结果，因此直接返回 true。
     */
    private boolean staticColumnsChanged(
            CanalEntry.EventType eventType,
            CanalEntry.RowData rowData,
            Set<String> staticColumns) {

        if (eventType
                != CanalEntry.EventType.UPDATE) {
            return true;
        }

        return rowData.getAfterColumnsList()
                .stream()
                .anyMatch(column ->
                        column.getUpdated()
                                && staticColumns.contains(
                                column.getName()
                        )
                );
    }

    private Long getRowId(
            CanalEntry.EventType eventType,
            CanalEntry.RowData rowData) {

        if (eventType
                == CanalEntry.EventType.DELETE) {
            return getLong(
                    rowData.getBeforeColumnsList(),
                    "id"
            );
        }

        return getLong(
                rowData.getAfterColumnsList(),
                "id"
        );
    }

    private Long getLong(
            List<CanalEntry.Column> columns,
            String columnName) {

        return columns.stream()
                .filter(column ->
                        columnName.equals(column.getName()))
                .map(CanalEntry.Column::getValue)
                .filter(value -> !value.isBlank())
                .map(Long::valueOf)
                .findFirst()
                .orElse(null);
    }

    private void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            running = false;
        }
    }
}
