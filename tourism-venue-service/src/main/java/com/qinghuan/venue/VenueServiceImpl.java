package com.qinghuan.venue;

import com.github.benmanes.caffeine.cache.Cache;
import com.qinghuan.auth.context.UserContext;
import com.qinghuan.common.constant.cacheKeys.VenueConstant;
import com.qinghuan.common.exception.BusinessException;
import com.qinghuan.common.exception.ErrorCode;
import com.qinghuan.config.oss.OssUtils;
import com.qinghuan.pojo.dto.VenueUpdateDTO;
import com.qinghuan.pojo.entity.Venue;
import com.qinghuan.pojo.vo.CatalogVenueVO;
import com.qinghuan.pojo.vo.VenueVO;
import com.qinghuan.pojo.remote.venue.VenueSummaryDTO;
import com.qinghuan.pojo.enums.VenueStatus;
import com.qinghuan.redis.CacheClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class VenueServiceImpl implements VenueService {
    private final VenueMapper venueMapper;
    private final OssUtils ossUtils;
    private final CacheClient cacheClient;
    private final Cache<Long, CatalogVenueVO> venueDetailLocalCache;

    public VenueServiceImpl(
            VenueMapper venueMapper,
            OssUtils ossUtils,
            CacheClient cacheClient,
            @Qualifier("venueDetailLocalCache")
            Cache<Long, CatalogVenueVO> venueDetailLocalCache) {
        this.venueMapper = venueMapper;
        this.ossUtils = ossUtils;
        this.cacheClient = cacheClient;
        this.venueDetailLocalCache = venueDetailLocalCache;
    }

    public VenueVO getCurrentVenue() {
        Venue venue = getCurrentVenueEntity();
        VenueVO result = new VenueVO();
        result.setId(venue.getId());
        result.setName(venue.getName());
        result.setAddress(venue.getAddress());
        result.setDescription(venue.getDescription());
        result.setCoverUrl(ossUtils.getPublicUrl(venue.getCoverObjectKey()));
        result.setStatus(venue.getStatus());
        result.setLongitude(venue.getLongitude());
        result.setLatitude(venue.getLatitude());
        return result;
    }

    @Override
    public VenueSummaryDTO getVenueSummary(Long venueId) {
        Venue venue = venueMapper.getVenueById(venueId);
        if (venue == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "景点不存在");
        }
        return new VenueSummaryDTO(
                venue.getId(), venue.getName(), venue.getAddress(), venue.getStatus() == VenueStatus.ENABLED);
    }

    private Venue getCurrentVenueEntity() {
        Venue venue = venueMapper.getVenueById(UserContext.getRequired().venueId());
        if (venue == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "景点不存在");
        }
        return venue;
    }

    @Override
    public void updateCurrentVenue(VenueUpdateDTO updateDTO, MultipartFile newCover) {
        Long venueId = UserContext.getRequired().venueId();
        Venue currentVenue = getCurrentVenueEntity();
        if ((updateDTO.getLongitude() == null) != (updateDTO.getLatitude() == null)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "经纬度必须同时填写");
        }

        Venue newVenue = new Venue();
        newVenue.setName(updateDTO.getName().trim());
        newVenue.setAddress(updateDTO.getAddress().trim());
        newVenue.setDescription(updateDTO.getDescription());
        newVenue.setStatus(updateDTO.getStatus());
        newVenue.setLongitude(updateDTO.getLongitude());
        newVenue.setLatitude(updateDTO.getLatitude());
        String uploadedObjectKey = null;

        if (newCover != null && !newCover.isEmpty()) {
            if (!StringUtils.hasText(newCover.getContentType())
                    || !newCover.getContentType().startsWith("image/")) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "封面文件必须是图片");
            }
            uploadedObjectKey = ossUtils.upload("venue", newCover);
            newVenue.setCoverObjectKey(uploadedObjectKey);
        }

        try {
            int updatedRows = venueMapper.updateVenue(newVenue, venueId);
            if (updatedRows == 0) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "景点不存在");
            }
            /*
             * 数据库修改成功后删除两级缓存。
             *
             * 先删除 Redis，再删除 Caffeine，可以避免本地缓存刚删除，
             * 并发请求却从旧 Redis 数据重新填充 Caffeine。
             */
            cacheClient.delete(VenueConstant.VENUE_DETAIL_PREFIX + venueId);
            venueDetailLocalCache.invalidate(venueId);
        } catch (DuplicateKeyException exception) {
            ossUtils.delete(uploadedObjectKey);
            throw new BusinessException(ErrorCode.CONFLICT, "相同名称和地址的景点已存在");
        }

        if (uploadedObjectKey != null) {
            ossUtils.delete(currentVenue.getCoverObjectKey());
        }
    }
}
