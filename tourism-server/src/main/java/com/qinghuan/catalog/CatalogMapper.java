package com.qinghuan.catalog;

import com.qinghuan.pojo.dto.CatalogVenuePageQueryDTO;
import com.qinghuan.pojo.vo.CatalogVenueVO;
import com.qinghuan.pojo.vo.SellableSessionVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface CatalogMapper {

    /** 查询至少存在一个当前可售场次票种的景点。 */
    List<CatalogVenueVO> listSellableVenues(CatalogVenuePageQueryDTO queryDTO);

    /** 只按启用状态查询景点，停用景点对游客视为不存在。 */
    CatalogVenueVO findEnabledVenue(Long venueId);

    /** 一次联表查询可售场次及其可售票种。 */
    List<SellableSessionVO> listSellableSessions(@Param("venueId") Long venueId,
                                                 @Param("visitDate") LocalDate visitDate);
    /**
     * 查询指定日期已经发布且预约尚未结束的场次ID。
     *
     * 包含未开售、正在销售和已经售罄的场次。
     */
    List<Long> listVisibleSessionIds(
            @Param("venueId") Long venueId,
            @Param("visitDate") LocalDate visitDate,
            @Param("now") LocalDateTime now
    );
}
