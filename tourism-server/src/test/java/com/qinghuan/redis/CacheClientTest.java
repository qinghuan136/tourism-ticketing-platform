package com.qinghuan.redis;

import cn.hutool.json.JSONUtil;
import com.qinghuan.pojo.vo.SessionStaticSnapshotVO;
import com.qinghuan.pojo.vo.SessionTicketTypeStaticVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CacheClientTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock lock;

    @Mock
    private Function<Long, SessionStaticSnapshotVO> dbFallback;

    private CacheClient cacheClient;

    @BeforeEach
    void setUp() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        cacheClient = new CacheClient(stringRedisTemplate, redissonClient);
    }

    @Test
    void shouldCacheSessionStaticSnapshotWithNestedTicketTypes() {
        prepareLock();
        String keyPrefix = "session:ticket-types:static:";
        Long sessionId = 21L;
        when(valueOperations.get(keyPrefix + sessionId)).thenReturn(null);

        SessionTicketTypeStaticVO ticketType = new SessionTicketTypeStaticVO();
        ticketType.setSessionTicketTypeId(301L);
        ticketType.setTicketTypeId(1L);
        ticketType.setTicketTypeName("成人票");
        ticketType.setSalePrice(new BigDecimal("60.00"));

        SessionStaticSnapshotVO snapshot = new SessionStaticSnapshotVO();
        snapshot.setSessionId(sessionId);
        snapshot.setVenueId(10L);
        snapshot.setVisitDate(LocalDate.of(2026, 8, 20));
        snapshot.setStartTime(LocalTime.of(9, 0));
        snapshot.setEndTime(LocalTime.of(11, 0));
        snapshot.setBookingStartAt(LocalDateTime.of(2026, 8, 15, 10, 0));
        snapshot.setBookingEndAt(LocalDateTime.of(2026, 8, 20, 8, 30));
        snapshot.setTicketTypes(List.of(ticketType));

        cacheClient.queryWithPassThrough(
                keyPrefix,
                sessionId,
                SessionStaticSnapshotVO.class,
                ignored -> snapshot,
                86400L,
                TimeUnit.SECONDS
        );

        // 捕获实际写入Redis的JSON，验证嵌套票种能够正常序列化和恢复。
        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(
                eq(keyPrefix + sessionId),
                jsonCaptor.capture(),
                eq(86400L),
                eq(TimeUnit.SECONDS)
        );

        SessionStaticSnapshotVO restored = JSONUtil.toBean(
                jsonCaptor.getValue(), SessionStaticSnapshotVO.class);
        assertEquals(sessionId, restored.getSessionId());
        assertEquals(1, restored.getTicketTypes().size());
        assertEquals("成人票", restored.getTicketTypes().get(0).getTicketTypeName());
        assertEquals(0, new BigDecimal("60.00").compareTo(
                restored.getTicketTypes().get(0).getSalePrice()));
        verify(lock).lock();
        verify(lock).unlock();
    }

    @Test
    void queryWithPassThrough_shouldReturnCacheWithoutLock_whenRedisHit() {
        String keyPrefix = "venue:detail:";
        Long venueId = 1L;
        SessionStaticSnapshotVO cached = new SessionStaticSnapshotVO();
        cached.setSessionId(venueId);
        when(valueOperations.get(keyPrefix + venueId))
                .thenReturn(JSONUtil.toJsonStr(cached));

        SessionStaticSnapshotVO result = cacheClient.queryWithPassThrough(
                keyPrefix,
                venueId,
                SessionStaticSnapshotVO.class,
                dbFallback,
                60L,
                TimeUnit.SECONDS
        );

        assertEquals(venueId, result.getSessionId());
        verifyNoInteractions(redissonClient);
        verifyNoInteractions(dbFallback);
    }

    @Test
    void queryWithPassThrough_shouldUseRebuiltCache_whenLockWaitEnds() {
        prepareLock();
        String keyPrefix = "venue:detail:";
        Long venueId = 2L;
        SessionStaticSnapshotVO rebuilt = new SessionStaticSnapshotVO();
        rebuilt.setSessionId(venueId);
        when(valueOperations.get(keyPrefix + venueId))
                .thenReturn(null, JSONUtil.toJsonStr(rebuilt));
        SessionStaticSnapshotVO result = cacheClient.queryWithPassThrough(
                keyPrefix,
                venueId,
                SessionStaticSnapshotVO.class,
                dbFallback,
                60L,
                TimeUnit.SECONDS
        );

        assertEquals(venueId, result.getSessionId());
        verifyNoInteractions(dbFallback);
        verify(lock).lock();
        verify(lock).unlock();
        verify(valueOperations, never()).set(
                eq(keyPrefix + venueId),
                anyString(),
                eq(60L),
                eq(TimeUnit.SECONDS)
        );
    }

    private void prepareLock() {
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
    }
}
