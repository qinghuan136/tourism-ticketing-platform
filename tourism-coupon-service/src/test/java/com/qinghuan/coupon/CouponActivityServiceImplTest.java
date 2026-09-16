package com.qinghuan.coupon;

import com.qinghuan.auth.context.UserContext;
import com.qinghuan.auth.model.LoginUser;
import com.qinghuan.common.exception.BusinessException;
import com.qinghuan.common.exception.ErrorCode;
import com.qinghuan.pojo.dto.CouponActivityWriteDTO;
import com.qinghuan.pojo.entity.CouponActivity;
import com.qinghuan.pojo.enums.AccountRole;
import com.qinghuan.pojo.enums.CouponActivityStatus;
import com.qinghuan.pojo.vo.CouponActivityCreatedVO;
import com.qinghuan.pojo.vo.CouponActivityVO;
import com.qinghuan.pojo.remote.venue.VenueSummaryDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class CouponActivityServiceImplTest {

    @Mock
    private CouponMapper couponMapper;

    @Mock
    private CouponPreheatService couponPreheatService;
    @Mock
    private VenueClient venueClient;

    private CouponActivityServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CouponActivityServiceImpl(couponMapper, couponPreheatService, venueClient);
        UserContext.set(new LoginUser(8L, "operator", AccountRole.OPERATOR, 10L));
        lenient().when(venueClient.getVenueSummary(10L))
                .thenReturn(com.qinghuan.common.response.ApiResponse.success(
                        new VenueSummaryDTO(10L, "景点甲", "测试路 1 号", true)));
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void shouldCreateDraftWithoutPreheatingCache() {
        when(couponMapper.insertActivity(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> {
                    CouponActivity activity = invocation.getArgument(0);
                    activity.setId(1001L);
                    return 1;
                });

        CouponActivityCreatedVO result = service.createDraft(validWriteDTO());

        ArgumentCaptor<CouponActivity> captor = ArgumentCaptor.forClass(CouponActivity.class);
        verify(couponMapper).insertActivity(captor.capture());
        CouponActivity saved = captor.getValue();
        assertEquals(1001L, result.id());
        assertEquals(CouponActivityStatus.DRAFT, saved.getStatus());
        assertEquals(500, saved.getRemainingStock());
        assertEquals(10L, saved.getVenueId());
        assertEquals("景点甲", saved.getVenueNameSnapshot());
        assertFalse(saved.getCacheReady());
    }

    @Test
    void shouldPublishOnlyDatabaseState() {
        CouponActivityVO activity = activity(CouponActivityStatus.DRAFT);
        when(couponMapper.findActivity(1001L, 10L)).thenReturn(activity);
        when(couponMapper.updateActivityStatus(
                1001L, 10L, CouponActivityStatus.DRAFT, CouponActivityStatus.PUBLISHED))
                .thenReturn(1);

        service.publish(1001L);

        verify(couponMapper).updateActivityStatus(
                1001L, 10L, CouponActivityStatus.DRAFT, CouponActivityStatus.PUBLISHED);
    }

    @Test
    void shouldRejectDiscountGreaterThanThreshold() {
        CouponActivityWriteDTO writeDTO = validWriteDTO();
        writeDTO.setThresholdAmount(new BigDecimal("10.00"));
        writeDTO.setDiscountAmount(new BigDecimal("20.00"));

        BusinessException exception = assertThrows(
                BusinessException.class, () -> service.createDraft(writeDTO));

        assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());
    }

    private CouponActivityWriteDTO validWriteDTO() {
        CouponActivityWriteDTO dto = new CouponActivityWriteDTO();
        dto.setName("暑期满100减20券");
        dto.setThresholdAmount(new BigDecimal("100.00"));
        dto.setDiscountAmount(new BigDecimal("20.00"));
        dto.setTotalStock(500);
        dto.setClaimStartAt(LocalDateTime.now().plusDays(1));
        dto.setClaimEndAt(LocalDateTime.now().plusDays(2));
        dto.setValidFrom(LocalDateTime.now().plusDays(1));
        dto.setValidUntil(LocalDateTime.now().plusDays(30));
        return dto;
    }

    private CouponActivityVO activity(CouponActivityStatus status) {
        CouponActivityVO activity = new CouponActivityVO();
        activity.setId(1001L);
        activity.setVenueId(10L);
        activity.setStatus(status);
        activity.setClaimEndAt(LocalDateTime.now().plusDays(2));
        return activity;
    }
}
