package com.qinghuan.catalog;

import com.qinghuan.pojo.dto.CatalogVenuePageQueryDTO;
import com.qinghuan.pojo.dto.NearbyVenueByNameQueryDTO;
import com.qinghuan.pojo.dto.NearbyVenueQueryDTO;
import com.qinghuan.pojo.vo.CatalogVenueVO;
import com.qinghuan.pojo.vo.NearbyVenueVO;
import com.qinghuan.pojo.vo.PageResult;
import com.qinghuan.pojo.vo.SellableSessionVO;

import java.time.LocalDate;
import java.util.List;

public interface CatalogService {

    /** 分页查询当前存在可售内容的启用景点。 */
    PageResult<CatalogVenueVO> pageSellableVenues(CatalogVenuePageQueryDTO queryDTO);

    /** 获取启用景点的游客端展示资料。 */
    CatalogVenueVO getVenue(Long venueId);

    /** 以游客当前坐标为中心，按距离由近到远查询景点。 */
    List<NearbyVenueVO> listNearbyVenues(NearbyVenueQueryDTO queryDTO);

    /** 先把地点名称解析为坐标，再复用坐标查询。 */
    List<NearbyVenueVO> listNearbyVenuesByName(
            NearbyVenueByNameQueryDTO queryDTO);

    /**
     * 查询指定景点、指定日期可展示的场次。
     *
     * 未开售场次只返回静态资料；
     * 已开售场次额外返回当前库存。
     */
    List<SellableSessionVO> listSellableSessions(Long venueId, LocalDate visitDate);
}
