package com.qinghuan.venue;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.qinghuan.auth.context.UserContext;
import com.qinghuan.auth.model.LoginUser;
import com.qinghuan.common.exception.BusinessException;
import com.qinghuan.common.exception.ErrorCode;
import com.qinghuan.config.oss.OssUtils;
import com.qinghuan.pojo.dto.VenueUpdateDTO;
import com.qinghuan.pojo.entity.Venue;
import com.qinghuan.pojo.enums.AccountRole;
import com.qinghuan.pojo.enums.VenueStatus;
import com.qinghuan.pojo.vo.CatalogVenueVO;
import com.qinghuan.redis.CacheClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("景点信息修改")
class VenueConstantServiceImplTest {

    @Mock
    private VenueMapper venueMapper;

    @Mock
    private OssUtils ossUtils;

    @Mock
    private CacheClient cacheClient;

    private VenueServiceImpl venueService;

    @BeforeEach
    void setUp() {
        venueService = new VenueServiceImpl(
                venueMapper,
                ossUtils,
                cacheClient,
                Caffeine.<Long, CatalogVenueVO>newBuilder().build());
        UserContext.set(new LoginUser(1L, "operator", AccountRole.OPERATOR, 10L));
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    @DisplayName("未上传新封面时仍可修改景点资料")
    void updateCurrentVenue_shouldKeepOldCover_whenNewCoverIsAbsent() {
        Venue currentVenue = currentVenue();
        VenueUpdateDTO update = updateRequest();
        update.setName("新名称");
        when(venueMapper.getVenueById(10L)).thenReturn(currentVenue);
        when(venueMapper.updateVenue(org.mockito.ArgumentMatchers.any(Venue.class),
                org.mockito.ArgumentMatchers.eq(10L))).thenReturn(1);

        venueService.updateCurrentVenue(update, null);

        ArgumentCaptor<Venue> captor = ArgumentCaptor.forClass(Venue.class);
        verify(venueMapper).updateVenue(captor.capture(), org.mockito.ArgumentMatchers.eq(10L));
        assertEquals("新名称", captor.getValue().getName());
        verify(ossUtils, never()).upload(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any());
        verify(ossUtils, never()).delete(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("非图片文件不能作为景点封面")
    void updateCurrentVenue_shouldRejectNonImageCover() {
        VenueUpdateDTO update = updateRequest();
        MockMultipartFile textFile = new MockMultipartFile(
                "coverImage", "cover.txt", "text/plain", "not-image".getBytes()
        );
        when(venueMapper.getVenueById(10L)).thenReturn(currentVenue());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> venueService.updateCurrentVenue(update, textFile)
        );

        assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());
        verify(venueMapper, never()).updateVenue(
                org.mockito.ArgumentMatchers.any(Venue.class),
                org.mockito.ArgumentMatchers.eq(10L));
    }

    @Test
    @DisplayName("上传新封面后保存 objectKey 并删除旧封面")
    void updateCurrentVenue_shouldReplaceCover_whenImageIsValid() {
        Venue currentVenue = currentVenue();
        VenueUpdateDTO update = updateRequest();
        MockMultipartFile image = new MockMultipartFile(
                "coverImage", "cover.jpg", "image/jpeg", new byte[]{1, 2, 3}
        );
        when(venueMapper.getVenueById(10L)).thenReturn(currentVenue);
        when(ossUtils.upload("venue", image)).thenReturn("venue/2026/07/30/new.jpg");
        when(venueMapper.updateVenue(org.mockito.ArgumentMatchers.any(Venue.class),
                org.mockito.ArgumentMatchers.eq(10L))).thenReturn(1);

        venueService.updateCurrentVenue(update, image);

        ArgumentCaptor<Venue> captor = ArgumentCaptor.forClass(Venue.class);
        verify(venueMapper).updateVenue(captor.capture(), org.mockito.ArgumentMatchers.eq(10L));
        assertEquals("venue/2026/07/30/new.jpg", captor.getValue().getCoverObjectKey());
        verify(ossUtils).delete("venue/old.jpg");
    }

    private VenueUpdateDTO updateRequest() {
        VenueUpdateDTO dto = new VenueUpdateDTO();
        dto.setName("海湾科技馆");
        dto.setAddress("广州市番禺区");
        dto.setStatus(VenueStatus.ENABLED);
        return dto;
    }

    private Venue currentVenue() {
        Venue venue = new Venue();
        venue.setCoverObjectKey("venue/old.jpg");
        return venue;
    }
}
