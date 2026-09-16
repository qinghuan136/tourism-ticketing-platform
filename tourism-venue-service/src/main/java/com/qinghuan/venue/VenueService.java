package com.qinghuan.venue;

import com.qinghuan.pojo.dto.VenueUpdateDTO;
import com.qinghuan.pojo.vo.VenueVO;
import com.qinghuan.pojo.remote.venue.VenueSummaryDTO;
import org.springframework.web.multipart.MultipartFile;

public interface VenueService {
    // 获取当前景点信息
    VenueVO getCurrentVenue();

    void updateCurrentVenue(VenueUpdateDTO newVenue, MultipartFile newCover);

    /** 服务间创建业务快照时读取景点名称和启用状态。 */
    VenueSummaryDTO getVenueSummary(Long venueId);
}
